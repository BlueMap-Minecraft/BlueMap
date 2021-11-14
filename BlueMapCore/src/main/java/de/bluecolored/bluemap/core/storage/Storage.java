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

import com.flowpowered.math.vector.Vector2i;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

public abstract class Storage implements Closeable {

    public abstract void initialize() throws IOException;

    public abstract OutputStream writeMapTile(String mapId, TileType tileType, Vector2i tile) throws IOException;

    public abstract Optional<CompressedInputStream> readMapTile(String mapId, TileType tileType, Vector2i tile) throws IOException;

    public abstract Optional<TileData> readMapTileData(String mapId, TileType tileType, Vector2i tile) throws IOException;

    public abstract void deleteMapTile(String mapId, TileType tileType, Vector2i tile) throws IOException;

    public abstract OutputStream writeMeta(String mapId, MetaType metaType) throws IOException;

    public abstract Optional<CompressedInputStream> readMeta(String mapId, MetaType metaType) throws IOException;

    public abstract void deleteMeta(String mapId, MetaType metaType) throws IOException;

    public abstract void purgeMap(String mapId) throws IOException;

    public TileStorage tileStorage(final String mapId, final TileType tileType) {
        return new TileStorage(mapId, tileType);
    }

    public class TileStorage {

        private final String mapId;
        private final TileType tileType;

        private TileStorage(String mapId, TileType tileType) {
            this.mapId = mapId;
            this.tileType = tileType;
        }

        public OutputStream write(Vector2i tile) throws IOException {
            return writeMapTile(mapId, tileType, tile);
        }

        public Optional<CompressedInputStream> read(Vector2i tile) throws IOException {
            return readMapTile(mapId, tileType, tile);
        }

        public void delete(Vector2i tile) throws IOException {
            deleteMapTile(mapId, tileType, tile);
        }

    }

}
