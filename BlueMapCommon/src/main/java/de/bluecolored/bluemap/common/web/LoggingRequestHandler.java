package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.common.web.http.*;
import de.bluecolored.bluemap.core.logger.Logger;

public class LoggingRequestHandler implements HttpRequestHandler {

    private final HttpRequestHandler delegate;
    private final Logger logger;
    private final String format;

    public LoggingRequestHandler(HttpRequestHandler delegate) {
        this(delegate, Logger.global);
    }

    public LoggingRequestHandler(HttpRequestHandler delegate, Logger logger) {
        this(delegate, "", logger);
    }

    public LoggingRequestHandler(HttpRequestHandler delegate, String format) {
        this(delegate, format, Logger.global);
    }

    public LoggingRequestHandler(HttpRequestHandler delegate, String format, Logger logger) {
        this.delegate = delegate;
        this.format = format;
        this.logger = logger;
    }

    @Override
    public HttpResponse handle(HttpRequest request) {

        // gather format parameters from request
        String source = request.getSource().toString();
        String xffSource = source;
        HttpHeader xffHeader = request.getHeader("X-Forwarded-For");
        if (xffHeader != null && !xffHeader.getValues().isEmpty()) {
            xffSource = xffHeader.getValues().get(0);
        }

        String method = request.getMethod();
        String address = request.getAddress();
        String version = request.getVersion();

        // run request
        HttpResponse response = delegate.handle(request);

        // gather format parameters from response
        HttpStatusCode status = response.getStatusCode();
        int statusCode = status.getCode();
        String statusMessage = status.getMessage();

        // format log message
        String log = String.format(this.format,
                source,
                xffSource,
                method,
                address,
                version,
                statusCode,
                statusMessage
        );

        // do the logging
        if (statusCode < 500) {
            logger.logInfo(log);
        } else {
            logger.logWarning(log);
        }

        // return the response
        return response;
    }

}
