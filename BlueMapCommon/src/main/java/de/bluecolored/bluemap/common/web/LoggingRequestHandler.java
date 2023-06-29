package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.common.web.http.HttpHeader;
import de.bluecolored.bluemap.common.web.http.HttpRequest;
import de.bluecolored.bluemap.common.web.http.HttpRequestHandler;
import de.bluecolored.bluemap.common.web.http.HttpResponse;
import de.bluecolored.bluemap.core.logger.Logger;

public class LoggingRequestHandler implements HttpRequestHandler {

    private final HttpRequestHandler delegate;
    private final Logger logger;

    public LoggingRequestHandler(HttpRequestHandler delegate) {
        this(delegate, Logger.global);
    }

    public LoggingRequestHandler(HttpRequestHandler delegate, Logger logger) {
        this.delegate = delegate;
        this.logger = logger;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        String source = request.getSource().toString();

        HttpHeader xffHeader = request.getHeader("x-forwarded-for");
        if (xffHeader != null && !xffHeader.getValues().isEmpty()) {
            source = xffHeader.getValues().get(0);
        }

        String log = source + " \""
                + request.getMethod()
                + " " + request.getAddress()
                + " " + request.getVersion()
                + "\" ";

        HttpResponse response = delegate.handle(request);

        log += response.getStatusCode().toString();
        if (response.getStatusCode().getCode() < 400) {
            logger.logInfo(log);
        } else {
            logger.logWarning(log);
        }

        return response;
    }

}
