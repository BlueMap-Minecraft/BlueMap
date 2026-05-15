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

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Tracks active {@link SseConnection}s and provides queued broadcast delivery.
 */
public class SseConnectionManager implements Closeable {

    private final Set<SseConnection> connections = ConcurrentHashMap.newKeySet();
    private final LinkedBlockingQueue<String[]> broadcastQueue = new LinkedBlockingQueue<>();
    private final Thread broadcastThread;
    private volatile boolean closed;

    // allow objects to listen for when the connection count transitions between 0 and non-0
    private final Set<Consumer<Boolean>> hasConnectionsListeners = ConcurrentHashMap.newKeySet();

    public void addHasConnectionsListener(Consumer<Boolean> listener) { hasConnectionsListeners.add(listener); }
    public void removeHasConnectionsListener(Consumer<Boolean> listener) { hasConnectionsListeners.remove(listener); }

    public SseConnectionManager() {
        this.broadcastThread = new Thread(this::broadcastLoop, "bluemap-sse-broadcast");
        this.broadcastThread.setDaemon(true);
        this.broadcastThread.start();
    }

    /**
     * Creates a new {@link SseConnection}, registers it, and returns an {@link InputStream} suitable
     * for use as an HTTP response body. When the stream is closed (either because the client
     * disconnected or the server closed the connection), the connection is automatically removed
     * from this manager.
     */
    public InputStream openConnection() throws IOException {
        SseConnection connection = new SseConnection();
        add(connection);
        return new FilterInputStream(connection.getInputStream()) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    connection.close();
                    remove(connection);
                }
            }
        };
    }

    public void add(SseConnection connection) {
        boolean fire;
        synchronized (this) {
            fire = connections.isEmpty();
            fire = connections.add(connection) && fire;
        }
        if (fire) notifyHasConnections(true);
    }

    public void remove(SseConnection connection) {
        boolean fire;
        synchronized (this) {
            fire = connections.remove(connection) && connections.isEmpty();
        }
        if (fire) notifyHasConnections(false);
    }

    /**
     * Queues an SSE event to be sent to all live connections on the background broadcast thread.
     * Returns immediately without blocking.
     *
     * @param eventType the SSE event type
     * @param data      the event data payload
     */
    public void broadcast(String eventType, String data) {
        if (closed) return;
        broadcastQueue.offer(new String[]{eventType, data});
    }

    private void broadcastLoop() {
        try {
            while (!closed) {
                String[] event = broadcastQueue.take();
                broadcastSync(event[0], event[1]);
            }
        } catch (InterruptedException ignored) {}
    }

    /**
     * Sends an SSE event to all live connections synchronously.
     * Dead or broken connections are removed automatically.
     */
    private void broadcastSync(String eventType, String data) {
        if (connections.isEmpty()) return;
        List<SseConnection> toRemove = new ArrayList<>();
        for (SseConnection conn : connections) {
            try {
                conn.send(eventType, data);
            } catch (IOException ignored) {}
            if (conn.isClosed()) {
                toRemove.add(conn);
            }
        }
        if (!toRemove.isEmpty()) {
            boolean fire;
            synchronized (this) {
                fire = connections.removeAll(toRemove) && connections.isEmpty();
            }
            if (fire) notifyHasConnections(false);
        }
    }

    /**
     * Closes all registered connections, clears the registry, and stops the broadcast thread.
     */
    @Override
    public void close() {
        closed = true;
        broadcastThread.interrupt();
        boolean fire;
        synchronized (this) {
            fire = !connections.isEmpty();
            for (SseConnection conn : connections) {
                conn.close();
            }
            connections.clear();
        }
        if (fire) notifyHasConnections(false);
    }

    private void notifyHasConnections(boolean hasConnections) {
        hasConnectionsListeners.forEach(listener -> listener.accept(hasConnections));
    }

}
