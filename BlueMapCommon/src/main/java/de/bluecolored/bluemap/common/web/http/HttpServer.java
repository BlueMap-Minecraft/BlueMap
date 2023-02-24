package de.bluecolored.bluemap.common.web.http;

import java.io.IOException;

public class HttpServer extends Server {

    private final HttpRequestHandler requestHandler;

    public HttpServer(HttpRequestHandler requestHandler) throws IOException {
        this.requestHandler = requestHandler;
    }

    @Override
    public SelectionConsumer createConnectionHandler() {
        return new HttpConnection(requestHandler);
    }

}
