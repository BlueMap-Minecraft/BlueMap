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

@FunctionalInterface
public interface ChunkConsumer<T> {

    default boolean filter(int chunkX, int chunkZ, int lastModified) {
        return true;
    }

    void accept(int chunkX, int chunkZ, T chunk);

    default void fail(int chunkX, int chunkZ, IOException exception) throws IOException {
        throw exception;
    }

    @FunctionalInterface
    interface ListOnly<T> extends ChunkConsumer<T> {

        void accept(int chunkX, int chunkZ, int lastModified);

        @Override
        default boolean filter(int chunkX, int chunkZ, int lastModified) {
            accept(chunkX, chunkZ, lastModified);
            return false;
        }

        @Override
        default void accept(int chunkX, int chunkZ, T chunk) {
            throw new IllegalStateException("Should never be called.");
        }

    }

}
