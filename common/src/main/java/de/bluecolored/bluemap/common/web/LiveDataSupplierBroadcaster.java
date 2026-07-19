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

import de.bluecolored.bluemap.core.BlueMap;

import java.io.Closeable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Polls a {@link Supplier} and notifies registered listeners whenever the
 * returned value changes.
 *
 * Polling and updating the data is done by the {@link BlueMap#SCHEDULER} and
 * {@link BlueMap#THREAD_POOL}.
 *
 * Call {@link #update()} to get the current value (rate-limited by the polling rate)
 * Call {@link #close()} to stop the background polling.
 */
public class LiveDataSupplierBroadcaster<T> implements Supplier<T>, Closeable {

    private final Supplier<T> dataSupplier;
    private final long pollIntervalMillis;
    private final Set<Consumer<T>> listeners = ConcurrentHashMap.newKeySet();
    private ScheduledFuture<?> pollTask = null;
    private boolean closed = false;
    private long lastUpdate = -1;
    private T data = null;

    public LiveDataSupplierBroadcaster(Supplier<T> dataSupplier, long pollIntervalMillis) {
        this.dataSupplier = dataSupplier;
        this.pollIntervalMillis = pollIntervalMillis;
    }

    public synchronized void addUpdateListener(Consumer<T> listener) {
        listeners.add(listener);

        // have a listener - ensure scheduled poll task is running
        if (!closed && pollTask == null) {
            pollTask = BlueMap.SCHEDULER.scheduleWithFixedDelay(
                () -> BlueMap.THREAD_POOL.execute(this::update),
                0,
                pollIntervalMillis,
                TimeUnit.MILLISECONDS
            );
        }
    }

    public synchronized void removeUpdateListener(Consumer<T> listener) {
        listeners.remove(listener);

        // stop automatically updating if nothing is listening anymore
        if (listeners.isEmpty() && pollTask != null) {
            pollTask.cancel(false);
            pollTask = null;
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
     * Stops the background polling.
     */
    @Override
    public synchronized void close() {
        closed = true;
        if (pollTask != null) {
            pollTask.cancel(false);
            pollTask = null;
        }
    }

}
