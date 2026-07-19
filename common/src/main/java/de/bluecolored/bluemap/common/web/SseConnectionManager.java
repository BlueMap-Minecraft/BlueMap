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
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Tracks active {@link SseConnection}s and provides broadcast delivery.
 *
 * Broadcasting just queues events into each managed {@link SseConnection} without
 * blocking so a slow or stuck client only affects itself.
 */
public class SseConnectionManager implements Closeable {

    private final Set<SseConnection> connections = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    // allow objects to listen for when the connection count transitions between 0 and non-0
    private final Set<Consumer<Boolean>> hasConnectionsListeners = ConcurrentHashMap.newKeySet();

    public void addHasConnectionsListener(Consumer<Boolean> listener) { hasConnectionsListeners.add(listener); }
    public void removeHasConnectionsListener(Consumer<Boolean> listener) { hasConnectionsListeners.remove(listener); }

    /**
     * Creates a new {@link SseConnection}, registers it, and returns an {@link InputStream} suitable
     * for use as an HTTP response body. When the stream is closed (either because the client
     * disconnected or the server closed the connection), the connection is automatically removed
     * from this manager.
     */
    public InputStream openConnection() throws IOException {
        SseConnection connection = new SseConnection();
        add(connection);
        return connection.getInputStream();
    }

    public void add(SseConnection connection) {
        boolean fire;
        synchronized (this) {
            fire = connections.isEmpty();
            if (connections.add(connection)){
                if (!connection.setOnClose(() -> remove(connection))){
                    // failed to register an onClose callback (connection is already closed?)
                    connections.remove(connection);
                    fire = false;
                }
            }
            else {
                fire = false;
            }
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
     * Broadcast an SSE event to all live connections.
     * Returns immediately without blocking.
     *
     * @param eventType the SSE event type
     * @param data      the event data payload
     */
    public void broadcast(String eventType, String data) {
        if (closed) return;
        for (SseConnection conn : connections) {
            conn.enqueue(eventType, data);
        }
    }

    /**
     * Closes all registered connections. Each connection removes itself from the registry
     * (via {@link #remove(SseConnection)}) as it closes.
     */
    @Override
    public void close() {
        closed = true;
        for (SseConnection conn : connections) {
            conn.close();
        }
    }

    private void notifyHasConnections(boolean hasConnections) {
        hasConnectionsListeners.forEach(listener -> listener.accept(hasConnections));
    }

}
