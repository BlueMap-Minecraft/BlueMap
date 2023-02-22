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
