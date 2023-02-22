package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.common.web.http.HttpConnection;
import de.bluecolored.bluemap.common.web.http.HttpRequestHandler;
import de.bluecolored.bluemap.common.web.http.SelectionConsumer;
import de.bluecolored.bluemap.common.web.http.Server;

import java.io.IOException;

public class WebServer extends Server {

    private final HttpRequestHandler requestHandler;

    public WebServer(HttpRequestHandler requestHandler) throws IOException {
        this.requestHandler = requestHandler;
    }

    @Override
    public SelectionConsumer createConnectionHandler() {
        return new HttpConnection(requestHandler);
    }

}
