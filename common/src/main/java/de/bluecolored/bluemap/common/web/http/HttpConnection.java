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
package de.bluecolored.bluemap.common.web.http;

import de.bluecolored.bluemap.core.logger.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class HttpConnection implements SelectionConsumer {

    private final HttpRequestHandler requestHandler;
    private final Executor responseHandlerExecutor;
    private HttpRequest request;
    private CompletableFuture<HttpResponse> futureResponse;
    private HttpResponse response;

    public HttpConnection(HttpRequestHandler requestHandler) {
        this(requestHandler, Runnable::run); //run synchronously
    }

    public HttpConnection(HttpRequestHandler requestHandler, Executor responseHandlerExecutor) {
        this.requestHandler = requestHandler;
        this.responseHandlerExecutor = responseHandlerExecutor;
    }

    @Override
    public void accept(SelectionKey selectionKey) {
        if (!selectionKey.isValid()) return;

        SelectableChannel selChannel = selectionKey.channel();

        if (!(selChannel instanceof SocketChannel)) return;
        SocketChannel channel = (SocketChannel) selChannel;

        try {

            if (request == null) {
                SocketAddress remote = channel.getRemoteAddress();
                InetAddress remoteInet = null;
                if (remote instanceof InetSocketAddress)
                    remoteInet = ((InetSocketAddress) remote).getAddress();

                request = new HttpRequest(remoteInet);
            }

            // receive request
            if (!request.write(channel)) {
                if (!selectionKey.isValid()) return;
                selectionKey.interestOps(SelectionKey.OP_READ);
                return;
            }

            // process request
            if (futureResponse == null) {
                futureResponse = CompletableFuture.supplyAsync(
                        () -> requestHandler.handle(request),
                        responseHandlerExecutor
                );
                futureResponse.handle((response, error) -> {
                    if (error != null) {
                        Logger.global.logError("Unexpected error handling request", error);
                        response = new HttpResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
                    }

                    try {
                        response.read(channel); // do an initial read to trigger response sending intent
                        this.response = response;
                    } catch (IOException e) {
                        handleIOException(channel, e);
                    }

                    return null;
                });
            }

            if (response == null) return;
            if (!selectionKey.isValid()) return;

            // send response
            if (!response.read(channel)){
                selectionKey.interestOps(SelectionKey.OP_WRITE);
                return;
            }

            // reset to accept new request
            request.clear();
            response.close();
            futureResponse = null;
            response = null;
            selectionKey.interestOps(SelectionKey.OP_READ);

        } catch (IOException e) {
            handleIOException(channel, e);
        }
    }

    private void handleIOException(Channel channel, IOException e) {
        request.clear();

        if (response != null) {
            try {
                response.close();
            } catch (IOException e2) {
                Logger.global.logWarning("Failed to close response: " + e2);
            }
            response = null;
        }

        if (futureResponse != null) {
            futureResponse.thenAccept(response -> {
                try {
                    response.close();
                } catch (IOException e2) {
                    Logger.global.logWarning("Failed to close response: " + e2);
                }
            });
            futureResponse = null;
        }

        Logger.global.logDebug("Failed to process selection: " + e);
        try {
            channel.close();
        } catch (IOException e2) {
            Logger.global.logWarning("Failed to close channel" + e2);
        }
    }

}
