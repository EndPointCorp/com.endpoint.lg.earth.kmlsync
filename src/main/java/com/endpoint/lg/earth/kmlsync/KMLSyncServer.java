package com.endpoint.lg.earth.kmlsync;

import interactivespaces.activity.impl.BaseActivity;
import interactivespaces.activity.impl.web.BaseRoutableRosWebServerActivity;
import interactivespaces.service.web.server.HttpDynamicRequestHandler;
import interactivespaces.service.web.server.HttpRequest;
import interactivespaces.service.web.server.HttpResponse;
import interactivespaces.service.web.server.WebServer;

import java.net.URI;
import java.util.Map;

/**
 * A simple Interactive Spaces Java-based activity.
 */
public class KMLSyncServer extends BaseRoutableRosWebServerActivity {

    private class KMLSyncWebHandler implements HttpDynamicRequestHandler {
        @Override
        public void handle(HttpRequest request, HttpResponse response) {
            /* http://docs.oracle.com/javase/6/docs/api/index.html?java/net/URI.html */
            URI uri = request.getUri();
            Map parameters = request.getUriQueryParameters();
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
