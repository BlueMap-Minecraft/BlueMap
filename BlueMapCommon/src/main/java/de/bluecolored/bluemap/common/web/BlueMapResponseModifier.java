package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.common.webserver.HttpRequest;
import de.bluecolored.bluemap.common.webserver.HttpRequestHandler;
import de.bluecolored.bluemap.common.webserver.HttpResponse;
import de.bluecolored.bluemap.common.webserver.HttpStatusCode;
import de.bluecolored.bluemap.core.BlueMap;

public class BlueMapResponseModifier implements HttpRequestHandler {

    private final HttpRequestHandler delegate;
    private final String serverName;

    public BlueMapResponseModifier(HttpRequestHandler delegate) {
        this.delegate = delegate;
        this.serverName = "BlueMap " + BlueMap.VERSION + " " + BlueMap.GIT_HASH + " " + BlueMap.GIT_CLEAN;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        HttpResponse response = delegate.handle(request);

        HttpStatusCode status = response.getStatusCode();
        if (status.getCode() >= 400 && !response.hasData()){
            response.setData(status.getCode() + " - " + status.getMessage() + "\n" + this.serverName);
        }

        response.addHeader("Server", this.serverName);

        return response;
    }

}
