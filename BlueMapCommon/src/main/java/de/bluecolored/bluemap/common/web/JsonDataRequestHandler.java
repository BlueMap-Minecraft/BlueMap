package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.common.webserver.HttpRequest;
import de.bluecolored.bluemap.common.webserver.HttpRequestHandler;
import de.bluecolored.bluemap.common.webserver.HttpResponse;
import de.bluecolored.bluemap.common.webserver.HttpStatusCode;
import de.bluecolored.bluemap.core.BlueMap;

import java.util.function.Supplier;

public class JsonDataRequestHandler implements HttpRequestHandler {

    private final Supplier<String> dataSupplier;

    public JsonDataRequestHandler(Supplier<String> dataSupplier) {
        this.dataSupplier = dataSupplier;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        HttpResponse response = new HttpResponse(HttpStatusCode.OK);
        response.addHeader("Server", "BlueMap v" + BlueMap.VERSION);
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Content-Type", "application/json");
        response.setData(dataSupplier.get());
        return response;
    }

}
