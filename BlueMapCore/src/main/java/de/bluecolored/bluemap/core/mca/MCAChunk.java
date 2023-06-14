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
package de.bluecolored.bluemap.core.mca;

import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.LightData;
import net.querz.nbt.CompoundTag;

import java.io.IOException;
import java.util.Arrays;

public abstract class MCAChunk implements Chunk {

    private final MCAWorld world;
    private final int dataVersion;

    private int[] netherCeilingHeights;

    protected MCAChunk() {
        this(null, -1);
    }

    protected MCAChunk(MCAWorld world) {
        this(world, -1);
    }

    protected MCAChunk(MCAWorld world, CompoundTag chunkTag) {
        this(world, chunkTag.getInt("DataVersion"));
    }

    private MCAChunk(MCAWorld world, int dataVersion) {
        this.world = world;
        this.dataVersion = dataVersion;

        this.netherCeilingHeights = new int[16 * 16];
        Arrays.fill(this.netherCeilingHeights, Integer.MIN_VALUE);
    }

    @Override
    public abstract boolean isGenerated();

    public int getDataVersion() {
        return dataVersion;
    }

    @Override
    public abstract long getInhabitedTime();

    @Override
    public abstract BlockState getBlockState(int x, int y, int z);

    @Override
    public abstract LightData getLightData(int x, int y, int z, LightData target);

    @Override
    public abstract String getBiome(int x, int y, int z);

    @Override
    public int getMaxY(int x, int z) {
        return 255;
    }

    @Override
    public int getMinY(int x, int z) {
        return 0;
    }

    @Override
    public int getWorldSurfaceY(int x, int z) { return 0; }

    @Override
    public int getOceanFloorY(int x, int z) { return 0; }

    @Override
    public int getNetherCeilingY(int x, int z) {
        int lx = x & 0xF, lz = z & 0xF;
        int i = lz * 16 + lx;
        int y = netherCeilingHeights[i];

        if (y == Integer.MIN_VALUE) {
            int maxY = getMaxY(x, z);
            int minY = getMinY(x, z);

            for (y = Math.min(maxY, 120); y >= minY; y--){
                if (!getBlockState(x, y, z).isNetherCeiling()) break;
            }

            netherCeilingHeights[i] = y;
        }

        return y;
    }

    protected MCAWorld getWorld() {
        return world;
    }

    public static MCAChunk create(MCAWorld world, CompoundTag chunkTag) throws IOException {
        int version = chunkTag.getInt("DataVersion");

        if (version < 2200) return new ChunkAnvil113(world, chunkTag);
        if (version < 2500) return new ChunkAnvil115(world, chunkTag);
        if (version < 2844) return new ChunkAnvil116(world, chunkTag);
        return new ChunkAnvil118(world, chunkTag);
    }

    @Override
    public String toString() {
        return "MCAChunk{" +
               "world=" + world +
               "dataVersion=" + dataVersion +
               "isGenerated()=" + isGenerated() +
               '}';
    }

}
