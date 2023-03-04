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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public abstract class Storage implements Closeable {

    public abstract void initialize() throws IOException;

    public abstract OutputStream writeMapTile(String mapId, int lod, Vector2i tile) throws IOException;

    public abstract Optional<CompressedInputStream> readMapTile(String mapId, int lod, Vector2i tile) throws IOException;

    public abstract Optional<TileInfo> readMapTileInfo(String mapId, int lod, Vector2i tile) throws IOException;

    public abstract void deleteMapTile(String mapId, int lod, Vector2i tile) throws IOException;

    public abstract OutputStream writeMeta(String mapId, String name) throws IOException;

    public abstract Optional<InputStream> readMeta(String mapId, String name) throws IOException;

    public abstract Optional<MetaInfo> readMetaInfo(String mapId, String name) throws IOException;

    public abstract void deleteMeta(String mapId, String name) throws IOException;

    public abstract void purgeMap(String mapId, Function<ProgressInfo, Boolean> onProgress) throws IOException;

    public abstract Collection<String> collectMapIds() throws IOException;

    public abstract long estimateMapSize(String mapId) throws IOException;

    public MapStorage mapStorage(final String mapId) {
        return new MapStorage(mapId);
    }

    public TileStorage tileStorage(final String mapId, final int lod) {
        return new TileStorage(mapId, lod);
    }

    public abstract boolean isClosed();

    public class MapStorage {

        private final String mapId;

        private MapStorage(String mapId) {
            this.mapId = mapId;
        }

        public OutputStream write(int lod, Vector2i tile) throws IOException {
            return writeMapTile(mapId, lod, tile);
        }

        public Optional<CompressedInputStream> read(int lod, Vector2i tile) throws IOException {
            return readMapTile(mapId, lod, tile);
        }

        public void delete(int lod, Vector2i tile) throws IOException {
            deleteMapTile(mapId, lod, tile);
        }

        public Storage getStorage() {
            return Storage.this;
        }

    }

    public class TileStorage {

        private final String mapId;
        private final int lod;

        private TileStorage(String mapId, int lod) {
            this.mapId = mapId;
            this.lod = lod;
        }

        public OutputStream write(Vector2i tile) throws IOException {
            return writeMapTile(mapId, lod, tile);
        }

        public Optional<CompressedInputStream> read(Vector2i tile) throws IOException {
            return readMapTile(mapId, lod, tile);
        }

        public void delete(Vector2i tile) throws IOException {
            deleteMapTile(mapId, lod, tile);
        }

        public Storage getStorage() {
            return Storage.this;
        }

    }

    public static class ProgressInfo {

        private final double progress;

        public ProgressInfo(double progress) {
            this.progress = progress;
        }

        public double getProgress() {
            return progress;
        }

    }

    public static String escapeMetaName(String name) {
        return name.replaceAll("[^\\w\\d.\\-_/]", "_").replace("..", "_.");
    }

}
