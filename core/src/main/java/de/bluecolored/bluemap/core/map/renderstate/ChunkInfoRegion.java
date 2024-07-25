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
package de.bluecolored.bluemap.core.map.renderstate;

import de.bluecolored.bluenbt.NBTName;
import de.bluecolored.bluenbt.NBTPostDeserialize;
import lombok.Getter;

import static de.bluecolored.bluemap.core.map.renderstate.MapChunkState.SHIFT;

public class ChunkInfoRegion implements CellStorage.Cell {

    static final int REGION_LENGTH = 1 << SHIFT;
    static final int REGION_MASK = REGION_LENGTH - 1;
    static final int CHUNKS_PER_REGION = REGION_LENGTH * REGION_LENGTH;

    @NBTName("chunk-hashes")
    private int[] chunkHashes;

    @Getter
    private transient boolean modified;

    private ChunkInfoRegion() {}

    @NBTPostDeserialize
    public void init() {
        if (chunkHashes == null || chunkHashes.length != CHUNKS_PER_REGION)
            chunkHashes = new int[CHUNKS_PER_REGION];
    }

    public int get(int x, int z) {
        return chunkHashes[index(x, z)];
    }

    public int set(int x, int z, int hash) {
        int index = index(x, z);
        int previous = chunkHashes[index];

        chunkHashes[index] = hash;

        if (previous != hash)
            modified = true;

        return previous;
    }

    private static int index(int x, int z) {
        return (z & REGION_MASK) << SHIFT | (x & REGION_MASK);
    }

    public static ChunkInfoRegion create() {
        ChunkInfoRegion region = new ChunkInfoRegion();
        region.init();
        return region;
    }

}
