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
package de.bluecolored.bluemap.core.world.mca.chunk;

import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.mca.ChunkLoader;
import de.bluecolored.bluemap.core.world.mca.MCAUtil;
import de.bluecolored.bluemap.core.world.mca.MCAWorld;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.BiFunction;

public class MCAChunkLoader implements ChunkLoader<Chunk> {

    private final MCAWorld world;

    public MCAChunkLoader(MCAWorld world) {
        this.world = world;
    }

    // sorted list of chunk-versions, loaders at the start of the list are preferred over loaders at the end
    private static final List<ChunkVersionLoader<?>> CHUNK_VERSION_LOADERS = List.of(
            new ChunkVersionLoader<>(Chunk_1_18.Data.class, Chunk_1_18::new, 2844),
            new ChunkVersionLoader<>(Chunk_1_16.Data.class, Chunk_1_16::new, 2500),
            new ChunkVersionLoader<>(Chunk_1_15.Data.class, Chunk_1_15::new, 2200),
            new ChunkVersionLoader<>(Chunk_1_13.Data.class, Chunk_1_13::new, 0)
    );

    private ChunkVersionLoader<?> lastUsedLoader = CHUNK_VERSION_LOADERS.get(0);

    public MCAChunk load(byte[] data, int offset, int length, Compression compression) throws IOException {
        InputStream in = new ByteArrayInputStream(data, offset, length);
        in.mark(-1);

        // try last used version
        ChunkVersionLoader<?> usedLoader = lastUsedLoader;
        MCAChunk chunk;
        try (InputStream decompressedIn = compression.decompress(in)) {
            chunk = usedLoader.load(world, decompressedIn);
        }

        // check version and reload chunk if the wrong loader has been used and a better one has been found
        ChunkVersionLoader<?> actualLoader = findBestLoaderForVersion(chunk.getDataVersion());
        if (actualLoader != null && usedLoader != actualLoader) {
            in.reset(); // reset read position
            try (InputStream decompressedIn = compression.decompress(in)) {
                chunk = actualLoader.load(world, decompressedIn);
            }
            lastUsedLoader = actualLoader;
        }

        return chunk;
    }

    @Override
    public Chunk emptyChunk() {
        return Chunk.EMPTY_CHUNK;
    }

    @Override
    public Chunk erroredChunk() {
        return Chunk.ERRORED_CHUNK;
    }

    private @Nullable ChunkVersionLoader<?> findBestLoaderForVersion(int version) {
        for (ChunkVersionLoader<?> loader : CHUNK_VERSION_LOADERS) {
            if (loader.mightSupport(version)) return loader;
        }
        return null;
    }

    @RequiredArgsConstructor
    @Getter
    private static class ChunkVersionLoader<D extends MCAChunk.Data> {

        private final Class<D> dataType;
        private final BiFunction<MCAWorld, D, MCAChunk> constructor;
        private final int dataVersion;

        public MCAChunk load(MCAWorld world, InputStream in) throws IOException {
            try {
                D data = MCAUtil.BLUENBT.read(in, dataType);
                return mightSupport(data.getDataVersion()) ? constructor.apply(world, data) : new MCAChunk(world, data) {};
            } catch (Exception e) {
                throw new IOException("Failed to parse chunk-data: " + e, e);
            }
        }

        public boolean mightSupport(int dataVersion) {
            return dataVersion >= this.dataVersion;
        }

    }

}
