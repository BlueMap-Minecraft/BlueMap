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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Polls a {@link Supplier} on a background thread and notifies registered listeners
 * whenever the returned value changes.
 *
 * Call {@link #update()} to get the current value (rate-limited by the polling rate)
 * Call {@link #close()} to stop the background thread.
 */
public class LiveDataSupplierBroadcaster<T> implements Supplier<T>, Closeable {

    private final Supplier<T> dataSupplier;
    private final long pollIntervalMillis;
    private final Set<Consumer<T>> listeners = ConcurrentHashMap.newKeySet();
    private final Thread pollThread;
    private volatile boolean closed = false;
    private long lastUpdate = -1;
    private T data = null;

    public LiveDataSupplierBroadcaster(Supplier<T> dataSupplier, long pollIntervalMillis) {
        this.dataSupplier = dataSupplier;
        this.pollIntervalMillis = pollIntervalMillis;
        this.pollThread = new Thread(this::pollLoop, String.format("%sPoller", dataSupplier.getClass().getName()));
        this.pollThread.setDaemon(true);
        this.pollThread.start();
    }

    public synchronized void addUpdateListener(Consumer<T> listener) {
        listeners.add(listener);
        notifyAll();
    }

    public void removeUpdateListener(Consumer<T> listener) {
        listeners.remove(listener);
    }

    private void pollLoop() {
        while (!closed) {
            // suspend polling until at least one listener is registered
            synchronized (this) {
                try {
                    while (listeners.isEmpty() && !closed){
                        wait();
                    }
                } catch (InterruptedException ignored){
                    break;
                }
            }
            if (closed) break;
            update();

            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException ignored) {
                break;
            }
        }
    }

    /**
     * Ensure the data is up to date and return it.
     *
     * Note that this will only get new data from the supplier if the current
     * cached data is stale (according to {@code pollIntervalMillis}).
     */
    @Override
    public T get() {
        update();
        return this.data;
    }

    private void update() {
        synchronized (this) {
            long now = System.currentTimeMillis();
            if (this.data != null && now < this.lastUpdate + this.pollIntervalMillis) return;
            this.lastUpdate = now;

            T newdata = dataSupplier.get();
            if (newdata == null || newdata.equals(this.data)) return;

            // new data, update listeners
            this.data = newdata;
        }
        for (Consumer<T> listener : listeners) {
            listener.accept(this.data);
        }
    }

    /**
     * Stops the background polling thread.
     */
    @Override
    public synchronized void close() {
        closed = true;
        notifyAll();
        pollThread.interrupt();
    }

}
