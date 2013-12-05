package com.endpoint.lg.earth.kmlsync;

import interactivespaces.activity.impl.web.BaseRoutableRosWebServerActivity;
import interactivespaces.service.web.server.HttpDynamicRequestHandler;
import interactivespaces.service.web.server.HttpRequest;
import interactivespaces.service.web.server.HttpResponse;
import interactivespaces.service.web.server.WebServer;
import interactivespaces.service.web.HttpResponseCode;

// http://docs.oracle.com/javase/6/docs/api/index.html?java/net/URI.html
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.io.OutputStream;
import java.lang.StringBuilder;

// http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/collect/Maps.html
import com.google.common.collect.Maps;
import com.google.common.collect.Lists;
import com.google.common.collect.ListMultimap; // for GET query parsing
import com.google.common.collect.ArrayListMultimap;
import com.google.common.base.Joiner;

/**
 * An Activity to serve KML to Google Earth, and updates from routes.
 */
public class KMLSyncServer extends BaseRoutableRosWebServerActivity {

    /* A Map whose keys are Window slugs, and whose values are Asset Maps. */
    /* This contains the state of which URL's should display on Windows. */
    Map<String, Object> windowAssetMap = Maps.newHashMap();

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
            URI uri = request.getUri();

            getLog().debug("Activity com.endpoint.lg.earth.kmlsync" +
                " handle URI: " + uri.toString() +
                " parameters: " + uri.getQuery());

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
            ArrayList<Map> serverAssetList =
                (ArrayList) windowAssetMap.get(clientWindowSlug);

            getLog().debug("Window " + clientWindowSlug + 
                " has " + clientAssetSlugList.toString() +
                " should have " + serverAssetList.toString());

            // What Assets need <Create> KML entries?
            List<Map> createAssetList = Lists.newArrayList();
            // For each Asset the server wants the client to load,
            for (Map<String,Map> serverAsset : serverAssetList) {
                String serverAssetSlug = (String) serverAsset.get("fields").get("slug");
                // If the client has not loaded this asset ...
                if ( ! clientAssetSlugList.contains(serverAssetSlug) ) {
                    // ... add it to the list needing <Create> elements.
                    createAssetList.add(serverAsset);
                }
            }

            // Which Assets need <Delete> KML entries?
            List<String> deleteAssetSlugList = Lists.newArrayList();
            // For each Asset the client has loaded,
            for (String clientAssetSlug : clientAssetSlugList) {
                // Search for an Asset with this slug on the Server side.
                // (I'm sure there's a better method for this.)
                boolean haveFoundMatch = false;
                for (Map<String,Object> serverAsset : serverAssetList) {
                    if (clientAssetSlug.equals(serverAsset.get("slug"))) {
                        haveFoundMatch = true;
                    }
                }
                if ( ! haveFoundMatch ) {
                    deleteAssetSlugList.add(clientAssetSlug);
                }
            }

            // Begin rendering the KML for the HTTP Response.
            getLog().debug("Create list: " + createAssetList.toString());
            getLog().debug("Delete list: " + deleteAssetSlugList.toString());

            OutputStream outputStream = response.getOutputStream();
            StringBuilder output = new StringBuilder();

            output.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            output.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\" xmlns:kml=\"http://www.opengis.net/kml/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
            output.append("<NetworkLinkControl>\n");
            output.append("  <minRefreshPeriod>1</minRefreshPeriod>\n");
            output.append("  <maxSessionLength>-1</maxSessionLength>\n");

            output.append("  <cookie><![CDATA[");
            // slugs of serverAssetList go here
            Joiner joiner = Joiner.on("&").skipNulls();
            List<String> cookies = Lists.newArrayList();
            for (Map<String,Map> serverAsset : serverAssetList) {
                String serverAssetSlug =
                    (String) serverAsset.get("fields").get("slug");
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
                    Map<String, String> fields =
                        (Map<String, String>) asset.get("fields");
                    output.append("        <NetworkLink id=\"");
                    // asset.slug goes here
                    output.append(fields.get("slug"));
                    output.append("\">\n");

                    output.append("          <name>");
                    // asset.title goes here
                    output.append(fields.get("title"));
                    output.append("</name>\n");

                    output.append("          <Link><href>");
                    // asset.storage goes here
                    output.append(fields.get("storage"));
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

    @Override
    public void onActivitySetup() {
        getLog().info("Activity com.endpoint.lg.earth.kmlsync setup");
    }

    @Override
    public void onActivityStartup() {
        getLog().info("Activity com.endpoint.lg.earth.kmlsync startup");

        WebServer webserver = getWebServer();
        webserver.addDynamicContentHandler("/", false, new KMLSyncWebHandler());
    }

    @Override
    public void onActivityPostStartup() {
        getLog().info("Activity com.endpoint.lg.earth.kmlsync post startup");
    }

    @Override
    public void onActivityActivate() {
        getLog().info("Activity com.endpoint.lg.earth.kmlsync activate");
    }

    @Override
    public void onNewInputJson(String channelName,Map<String,Object> message) {
        getLog().info("Got message on input channel " + channelName);
        getLog().debug(message);

        windowAssetMap.putAll(message);

        getLog().debug("windowAssetMap is now " + windowAssetMap.toString());
    }

    @Override
    public void onActivityDeactivate() {
        getLog().info("Activity com.endpoint.lg.earth.kmlsync deactivate");
    }

    @Override
    public void onActivityPreShutdown() {
        getLog().info("Activity com.endpoint.lg.earth.kmlsync pre shutdown");
    }

    @Override
    public void onActivityShutdown() {
        getLog().info("Activity com.endpoint.lg.earth.kmlsync shutdown");
    }

    @Override
    public void onActivityCleanup() {
        getLog().info("Activity com.endpoint.lg.earth.kmlsync cleanup");
    }
}
