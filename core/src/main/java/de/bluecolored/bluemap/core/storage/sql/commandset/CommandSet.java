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
package de.bluecolored.bluemap.core.storage.sql.commandset;

import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;

public interface CommandSet extends Closeable {

    void initializeTables() throws IOException;

    void writeItem(String mapId, Key key, Compression compression, byte[] bytes) throws IOException;

    byte @Nullable [] readItem(String mapId, Key key, Compression compression) throws IOException;

    void deleteItem(String mapId, Key key) throws IOException;

    boolean hasItem(String mapId, Key key, Compression compression) throws IOException;

    void writeGridItem(
            String mapId, Key key, int x, int z, Compression compression,
            byte[] bytes
    ) throws IOException;

    byte @Nullable [] readGridItem(
            String mapId, Key key, int x, int z, Compression compression
    ) throws IOException;

    void deleteGridItem(
            String mapId, Key key, int x, int z
    ) throws IOException;

    boolean hasGridItem(
            String mapId, Key key, int x, int z, Compression compression
    ) throws IOException;

    TilePosition[] listGridItems(
            String mapId, Key key, Compression compression,
            int start, int count
    ) throws IOException;

    int countMapGridsItems(String mapId) throws IOException;

    int purgeMapGrids(String mapId, int limit) throws IOException;

    void purgeMap(String mapId) throws IOException;

    boolean hasMap(String mapId) throws IOException;

    String[] listMapIds(int start, int count) throws IOException;

    boolean isClosed();

    record TilePosition (int x, int z) {}

}
