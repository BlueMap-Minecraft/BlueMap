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
package de.bluecolored.bluemap.core.world;

import java.io.IOException;

public interface Region<T> {

    /**
     * Directly loads and returns the specified chunk.<br>
     * (implementations should consider overriding this method for a faster implementation)
     */
    default T loadChunk(int chunkX, int chunkZ) throws IOException {
        class SingleChunkConsumer implements ChunkConsumer<T> {
            private T foundChunk = emptyChunk();

            @Override
            public boolean filter(int x, int z, int lastModified) {
                return x == chunkX && z == chunkZ;
            }

            @Override
            public void accept(int chunkX, int chunkZ, T chunk) {
                this.foundChunk = chunk;
            }

        }

        SingleChunkConsumer singleChunkConsumer = new SingleChunkConsumer();
        iterateAllChunks(singleChunkConsumer);
        return singleChunkConsumer.foundChunk;
    }

    /**
     * Iterates over all chunks in this region and first calls {@link ChunkConsumer#filter(int, int, int)}.<br>
     * And if (any only if) that method returned <code>true</code>, the chunk will be loaded and {@link ChunkConsumer#accept(int, int, T)}
     * will be called with the loaded chunk.
     * @param consumer the consumer choosing which chunks to load and accepting them
     * @throws IOException if an IOException occurred trying to read the region
     */
    void iterateAllChunks(ChunkConsumer<T> consumer) throws IOException;

    T emptyChunk();

}
