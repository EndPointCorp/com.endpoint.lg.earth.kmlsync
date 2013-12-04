package com.endpoint.lg.earth.kmlsync;

import interactivespaces.activity.impl.web.BaseRoutableRosWebServerActivity;
import interactivespaces.service.web.server.HttpDynamicRequestHandler;
import interactivespaces.service.web.server.HttpRequest;
import interactivespaces.service.web.server.HttpResponse;
import interactivespaces.service.web.server.WebServer;

import java.net.URI;
import java.util.Map;

// http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/collect/Maps.html
import com.google.common.collect.Maps;

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
            /* http://docs.oracle.com/javase/6/docs/api/index.html?java/net/URI.html */
            URI uri = request.getUri();
            Map<String, String> parameters = request.getUriQueryParameters();

            // Which Earth Window is this HTTP request coming from?
            Object clientWindowSlug = parameters.get("window_slug");//try/catch

            // What Assets does this Earth Window already have loaded?
            String clientAssetSlugList = parameters.get("asset_slug");

            getLog().info("Activity com.endpoint.lg.earth.kmlsync" +
                " handle URI: " + uri.toString() +
                " parameters: " + parameters.toString());
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
