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
import lombok.RequiredArgsConstructor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class HttpConnection implements Runnable {

    private final Socket socket;
    private final HttpRequestInputStream requestIn;
    private final HttpResponseOutputStream responseOut;
    private final HttpRequestHandler requestHandler;

    public HttpConnection(Socket socket, HttpRequestHandler requestHandler) throws IOException {
        this.socket = socket;
        this.requestHandler = requestHandler;

        this.requestIn = new HttpRequestInputStream(new BufferedInputStream(socket.getInputStream()), socket.getInetAddress());
        this.responseOut = new HttpResponseOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    public void run() {
        try {
            while (socket.isConnected() && !socket.isClosed() && !socket.isInputShutdown() && !socket.isOutputShutdown()) {
                HttpRequest request = requestIn.read();
                if (request == null) continue;

                try (HttpResponse response = requestHandler.handle(request)) {
                    responseOut.write(response);
                }
            }
        } catch (EOFException | SocketTimeoutException ignore) {
            // ignore known exceptions that happen when browsers or us close the connection
        } catch (IOException e) {
            if ( // ignore known exceptions that happen when browsers close the connection
                    e.getMessage() == null ||
                    !e.getMessage().equals("Broken pipe")
            ) {
                Logger.global.logDebug("Exception in HttpConnection: " + e);
            }
        } catch (Exception e) {
            Logger.global.logDebug("Exception in HttpConnection: " + e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                Logger.global.logDebug("Exception closing HttpConnection: " + e);
            }
        }
    }

}
