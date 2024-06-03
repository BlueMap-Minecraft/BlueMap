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
package de.bluecolored.bluemap.core.util;

import lombok.experimental.StandardException;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A watch service that watches for changes and events.
 * @param <T> The type of the events or changes this WatchService provides
 */
public interface WatchService<T> extends AutoCloseable {

    /**
     * Retrieves and consumes the next batch of events.
     * @throws ClosedException If the watch-service is closed
     */
    @Nullable
    List<T> poll();

    /**
     * Retrieves and consumes the next batch of events,
     * waiting if necessary up to the specified wait time if none are yet present.
     * @throws ClosedException If the watch-service is closed, or it is closed while waiting for the next event
     * @throws InterruptedException If interrupted while waiting
     */
    @Nullable List<T> poll(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Retrieves and consumes the next batch of events,
     * waiting if necessary until an event becomes available.
     * @throws ClosedException If the watch-service is closed, or it is closed while waiting for the next event
     * @throws InterruptedException If interrupted while waiting
     */
    List<T> take() throws InterruptedException;

    /**
     * Thrown when the WatchService is closed or gets closed when polling or while waiting for events
     */
    @StandardException
    class ClosedException extends RuntimeException {}

}
