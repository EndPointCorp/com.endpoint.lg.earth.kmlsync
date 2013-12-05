package com.endpoint.lg.earth.kmlsync;

import interactivespaces.activity.impl.web.BaseRoutableRosWebServerActivity;
import interactivespaces.service.web.HttpResponseCode;
import interactivespaces.service.web.server.HttpDynamicRequestHandler;
import interactivespaces.service.web.server.HttpRequest;
import interactivespaces.service.web.server.HttpResponse;
import interactivespaces.service.web.server.WebServer;
import interactivespaces.util.data.json.JsonNavigator;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap; // for GET query parsing
import com.google.common.collect.Lists;
// http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/collect/Maps.html
import com.google.common.collect.Maps;

import com.endpoint.lg.support.message.MessageTypes;
import com.endpoint.lg.support.message.MessageWrapper;

import java.io.OutputStream;
// http://docs.oracle.com/javase/6/docs/api/index.html?java/net/URI.html
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An Activity to serve KML to Google Earth, and updates from routes.
 */
public class KMLSyncServer extends BaseRoutableRosWebServerActivity {

  /**
   * A Map whose keys are Window slugs, and whose values are Asset Maps. This
   * contains the state of which URL's should display on Windows.
   */
  Map<String, List<Map<String, Object>>> windowAssetMap = Maps.newHashMap();

  /**
   * Handler for HTTP GET Requests from Google Earth.
   *
   * KML Resources:
   * https://developers.google.com/kml/documentation/kml_tut#network_links
   * https://developers.google.com/kml/documentation/updates
   */
  private class KMLSyncWebHandler implements HttpDynamicRequestHandler {
    @Override
    public void handle(HttpRequest request, HttpResponse response) {
      handleKmlSyncRequest(request, response);
    }
  }

  @Override
  public void onActivityStartup() {
    getLog().info("Activity com.endpoint.lg.earth.kmlsync startup");

    WebServer webserver = getWebServer();
    webserver.addDynamicContentHandler("/", false, new KMLSyncWebHandler());
  }

  @Override
  public void onNewInputJson(String channelName, Map<String, Object> m) {
    getLog().info("Got message on input channel " + channelName);
    getLog().debug(m);

    JsonNavigator message = new JsonNavigator(m);
    String type = message.getString(MessageWrapper.MESSAGE_FIELD_TYPE);
    if (MessageTypes.MESSAGE_TYPE_WINDOW_ASSETS.equals(type)) {
      message.down(MessageWrapper.MESSAGE_FIELD_DATA);

      Set<String> windowSlugs = message.getProperties();
      for (String windowSlug : windowSlugs) {
        // For asset messages the next level is a list.
        List<Map<String, Object>> assets = message.getItem(windowSlug);

        synchronized (windowAssetMap) {
          // TODO(keith): Potentially turn the list entries into JsonNavigators
          // to simplify code later on.
          windowAssetMap.put(windowSlug, assets);
        }
      }
      getLog().debug("windowAssetMap is now " + windowAssetMap);
    }
  }

  /**
   * A dynamic HTTP request has come in for a KML Sync.
   *
   * @param request
   *          the HTTP request
   * @param response
   *          the HTTP response
   */
  private void handleKmlSyncRequest(HttpRequest request, HttpResponse response) {
    URI uri = request.getUri();

    getLog().debug(
        String.format("Activity com.endpoint.lg.earth.kmlsync handle URI: %s parameters: %s", uri, uri.getQuery()));

    // GET Parameter parsing courtesy of Keith Hughes.
    ListMultimap<String, String> params = ArrayListMultimap.create();

    String rawQuery = uri.getQuery();
    if (rawQuery != null && !rawQuery.isEmpty()) {
      String[] components = rawQuery.split("\\&");
      for (String component : components) {
        int pos = component.indexOf('=');
        if (pos != -1) {
          String decode = component.substring(pos + 1);
          try {
            decode = URLDecoder.decode(decode, "UTF-8");
          } catch (Exception e) {
            // Don't care
          }
          params.put(component.substring(0, pos).trim(), decode);
        } else {
          params.put(component.trim(), "");
        }
      }
    }

    // Which Earth Window is this HTTP request coming from?
    String clientWindowSlug = params.get("window_slug").get(0);

    // What Assets does this Earth Window already have loaded?
    List<String> clientAssetSlugList = params.get("asset_slug");

    // What Assets _should_ the client have loaded?
    List<Map<String, Object>> serverAssetList;
    synchronized (windowAssetMap) {
      serverAssetList = windowAssetMap.get(clientWindowSlug);
    }

    getLog().debug("Window " + clientWindowSlug + " has " + clientAssetSlugList + " should have " + serverAssetList);

    // What Assets need <Create> KML entries?
    List<Map<String, Object>> createAssetList = Lists.newArrayList();
    // For each Asset the server wants the client to load,
    for (Map<String, Object> serverAsset : serverAssetList) {
      JsonNavigator nav = new JsonNavigator(serverAsset);
      String serverAssetSlug = nav.down("fields").getString("slug");

      // If the client has not loaded this asset ...
      if (!clientAssetSlugList.contains(serverAssetSlug)) {
        // ... add it to the list needing <Create> elements.
        createAssetList.add(serverAsset);
      }
    }

    // Which Assets need <Delete> KML entries?
    List<String> deleteAssetSlugList = Lists.newArrayList(clientAssetSlugList);

    // This will see all the ones the server requires and removes them from the
    // delete list. All that will be left is the ones to delete.
    for (Map<String, Object> serverAsset : serverAssetList) {
      deleteAssetSlugList.remove(serverAsset.get("slug"));
    }

    // Begin rendering the KML for the HTTP Response.
    getLog().debug("Create list: " + createAssetList);
    getLog().debug("Delete list: " + deleteAssetSlugList);

    OutputStream outputStream = response.getOutputStream();

    StringBuilder output = new StringBuilder();

    output.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    output
        .append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\" xmlns:kml=\"http://www.opengis.net/kml/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
    output.append("<NetworkLinkControl>\n");
    output.append("  <minRefreshPeriod>1</minRefreshPeriod>\n");
    output.append("  <maxSessionLength>-1</maxSessionLength>\n");

    output.append("  <cookie><![CDATA[");
    // slugs of serverAssetList go here
    Joiner joiner = Joiner.on("&").skipNulls();
    List<String> cookies = Lists.newArrayList();
    for (Map<String, Object> serverAsset : serverAssetList) {
      JsonNavigator nav = new JsonNavigator(serverAsset);
      String serverAssetSlug = nav.down("fields").getString("slug");
      cookies.add("asset_slug=" + serverAssetSlug);
    }
    output.append(joiner.join(cookies));
    output.append("]]></cookie>\n");

    output.append("  <Update>\n");
    output.append("    <targetHref>");
    // URL to master.kml goes here.
    output.append("</targetHref>\n");

    // If there are any assets the client should load but hasn't yet,
    if (createAssetList.size() > 0) {
      output.append("      <Create><Document targetId=\"master\">\n");
      // For each asset the client should load but hasn't yet,
      for (Map<String, Object> asset : createAssetList) {
        JsonNavigator nav = new JsonNavigator(asset);
        nav.down("fields");

        output.append("        <NetworkLink id=\"");
        // asset.slug goes here
        output.append(nav.getString("slug"));
        output.append("\">\n");

        output.append("          <name>");
        // asset.title goes here
        output.append(nav.getString("title"));
        output.append("</name>\n");

        output.append("          <Link><href>");
        // asset.storage goes here
        output.append(nav.getString("storage"));
        output.append("</href></Link>\n");

        output.append("        </NetworkLink>\n");
      }
      output.append("      </Document></Create>\n");
    }

    // If there are any assets the client has loaded but should unload,
    if (deleteAssetSlugList.size() > 0) {
      output.append("      <Delete>\n");
      for (String assetSlug : deleteAssetSlugList) {
        output.append("        <NetworkLink targetId=\"");
        output.append(assetSlug);
        output.append("\" />\n");
      }
      output.append("      </Delete>\n");
    }

    output.append("  </Update>\n");
    output.append("</NetworkLinkControl>\n");
    output.append("</kml>\n");

    // Write the HTTP Response to the client.
    try {
      outputStream.write(output.toString().getBytes());
    } catch (Exception e) {
      getLog().error("Error writing HTTP Response", e);
      response.setResponseCode(HttpResponseCode.BAD_REQUEST);
    }
  }
}
