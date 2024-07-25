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
package de.bluecolored.bluemap.core.storage;

import java.io.Closeable;
import java.io.IOException;
import java.util.stream.Stream;

public interface Storage extends Closeable {

    /**
     * Does everything necessary to initialize this storage.
     * (E.g. create tables on a database if they don't exist or upgrade older data).
     */
    void initialize() throws IOException;

    /**
     * Returns the {@link MapStorage} for the given mapId.<br>
     * <br>
     * If this method is invoked multiple times with the same <code>mapId</code>, it is important that the returned MapStorage should at least
     * be equal (<code>equals() == true</code>) to the previously returned storages!
     */
    MapStorage map(String mapId);

    /**
     * Fetches and returns a stream of all map-id's in this storage
     */
    Stream<String> mapIds() throws IOException;

    /**
     * Checks if this storage is closed
     */
    boolean isClosed();

}
