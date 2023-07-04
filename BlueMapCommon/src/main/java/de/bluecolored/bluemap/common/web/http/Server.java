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

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Collection;

public abstract class Server extends Thread implements Closeable, Runnable {

    private final Selector selector;
    private final Collection<ServerSocketChannel> server;

    public Server() throws IOException {
        this.selector = Selector.open();
        this.server = new ArrayList<>();
    }

    public abstract SelectionConsumer createConnectionHandler();

    public void bind(SocketAddress address) throws IOException {
        final ServerSocketChannel server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT, (SelectionConsumer) this::accept);
        server.bind(address);
        this.server.add(server);

        Logger.global.logInfo("WebServer bound to: " + server.getLocalAddress());
    }

    @Override
    public void run() {
        Logger.global.logInfo("WebServer started.");
        while (this.selector.isOpen()) {
            try {
                this.selector.select(this::selection);
            } catch (IOException e) {
                Logger.global.logDebug("Failed to select channel: " + e);
            } catch (ClosedSelectorException ignore) {}
        }
    }

    private void selection(SelectionKey selectionKey) {
        Object attachment = selectionKey.attachment();
        if (attachment instanceof SelectionConsumer) {
            ((SelectionConsumer) attachment).accept(selectionKey);
        }
    }

    private void accept(SelectionKey selectionKey) {
        try {
            //noinspection resource
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
            SocketChannel channel = serverSocketChannel.accept();
            if (channel == null) return;
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, createConnectionHandler());
        } catch (IOException e) {
            Logger.global.logDebug("Failed to accept connection: " + e);
        }
    }

    @Override
    public void close() throws IOException {
        IOException exception = null;

        try {
            this.selector.close();
            this.selector.wakeup();
        } catch (IOException ex) {
            exception = ex;
        }

        for (ServerSocketChannel server : this.server) {
            try {
                server.close();
            } catch (IOException ex) {
                if (exception == null) exception = ex;
                else exception.addSuppressed(ex);
            }
        }

        if (exception != null) throw exception;
    }

}
