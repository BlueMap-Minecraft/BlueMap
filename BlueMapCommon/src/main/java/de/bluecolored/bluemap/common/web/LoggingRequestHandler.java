/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.common.web.http.*;
import de.bluecolored.bluemap.core.logger.Logger;
import lombok.Getter;

@Getter
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
