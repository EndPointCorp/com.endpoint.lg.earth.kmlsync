package com.endpoint.lg.earth.kmlsync;

import interactivespaces.activity.impl.web.BaseRoutableRosWebServerActivity;
import interactivespaces.service.web.server.HttpDynamicRequestHandler;
import interactivespaces.service.web.server.HttpRequest;
import interactivespaces.service.web.server.HttpResponse;
import interactivespaces.service.web.server.WebServer;

// http://docs.oracle.com/javase/6/docs/api/index.html?java/net/URI.html
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

// http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/collect/Maps.html
import com.google.common.collect.Maps;
import com.google.common.collect.ListMultimap; // for GET query parsing
import com.google.common.collect.ArrayListMultimap;

/**
 * An Activity to serve KML to Google Earth, and updates from routes.
 */
public class KMLSyncServer extends BaseRoutableRosWebServerActivity {

    /* A Map whose keys are Window slugs, and whose values are Asset Maps. */
    /* This contains the state of which URL's should display on Windows. */
    Map<String, Object> windowMap = Maps.newHashMap();

    /**
     * Handler for HTTP GET Requests from Google Earth.
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
            ArrayList serverAssetList =
                (ArrayList) windowMap.get(clientWindowSlug);

            getLog().debug("Window " + clientWindowSlug + " should have " +
                serverAssetList.toString());
            //getLog().debug(serverAssetSlugList.getClass().getName());
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

        windowMap.putAll(message);

        getLog().debug("windowMap is now " + windowMap.toString());
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
