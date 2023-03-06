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

        // Enable async request handling ...
        // TODO: maybe find a better/separate executor than using bluemap's common thread-pool
        //return new HttpConnection(requestHandler, BlueMap.THREAD_POOL);
    }

}
