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

        HttpHeader xffHeader = request.getHeader("X-Forwarded-For");
        if (xffHeader != null && !xffHeader.getValues().isEmpty()) {
            source = xffHeader.getValues().get(0);
        }

        StringBuilder log = new StringBuilder()
                .append(source)
                .append(" \"").append(request.getMethod())
                .append(" ").append(request.getAddress())
                .append(" ").append(request.getVersion())
                .append("\" ");

        HttpResponse response = delegate.handle(request);

        log.append(response.getStatusCode());
        if (response.getStatusCode().getCode() < 400) {
            logger.logInfo(log.toString());
        } else {
            logger.logWarning(log.toString());
        }

        return response;
    }

}
