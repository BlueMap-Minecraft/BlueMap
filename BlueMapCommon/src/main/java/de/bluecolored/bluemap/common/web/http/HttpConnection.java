package de.bluecolored.bluemap.common.web.http;

import de.bluecolored.bluemap.core.logger.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;

public class HttpConnection implements SelectionConsumer {

    private final ReentrantLock processingLock = new ReentrantLock();
    private final HttpRequestHandler requestHandler;
    private HttpRequest request;
    private HttpResponse response;

    public HttpConnection(HttpRequestHandler requestHandler) {
        this.requestHandler = requestHandler;
    }

    @Override
    public void accept(SelectionKey selectionKey) {
        if (!selectionKey.isValid()) return;
        if (!processingLock.tryLock()) return;

        try {
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
                if (response == null) {
                    this.response = requestHandler.handle(request);
                }

                if (!selectionKey.isValid()) return;

                // send response
                if (!response.read(channel)){
                    selectionKey.interestOps(SelectionKey.OP_WRITE);
                    return;
                }

                // reset to accept new request
                request.clear();
                response.close();
                response = null;
                selectionKey.interestOps(SelectionKey.OP_READ);

            } catch (IOException e) {
                Logger.global.logDebug("Failed to process selection: " + e);
                try {
                    channel.close();
                } catch (IOException e2) {
                    Logger.global.logWarning("Failed to close channel" + e2);
                }
            }

        } finally {
            processingLock.unlock();
        }
    }

}
