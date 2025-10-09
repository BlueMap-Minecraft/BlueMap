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
package de.bluecolored.bluemap.core.world.block;

import de.bluecolored.bluemap.core.world.*;
import de.bluecolored.bluemap.core.world.biome.Biome;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class Block implements BlockAccess {

    @Getter private final World world;
    @Getter private int x, y, z;

    private @Nullable Chunk chunk;

    private @Nullable BlockState blockState;
    private final LightData lightData = new LightData(-1, -1);
    private @Nullable Biome biome;

    private boolean isBlockEntitySet;
    private @Nullable BlockEntity blockEntity;

    public Block(World world, int x, int y, int z) {
        this.world = world;
        set(x, y, z);
    }

    @Override
    public void set(int x, int y, int z) {
        if (this.x == x && this.z == z){
            if (this.y == y) return;
        } else {
            this.chunk = null; //only reset the chunk if x or z have changed
        }

        this.x = x;
        this.y = y;
        this.z = z;

        this.blockState = null;
        this.lightData.set(-1, -1);
        this.biome = null;
        this.isBlockEntitySet = false;
        this.blockEntity = null;
    }

    @Override
    public BlockAccess copy() {
        return new Block(world, x, y, z);
    }

    public Chunk getChunk() {
        if (chunk == null) chunk = world.getChunkAtBlock(x, z);
        return chunk;
    }

    @Override
    public BlockState getBlockState() {
        if (blockState == null) blockState = getChunk().getBlockState(x, y, z);
        return blockState;
    }

    @Override
    public LightData getLightData() {
        if (lightData.getSkyLight() < 0) getChunk().getLightData(x, y, z, lightData);
        return lightData;
    }

    @Override
    public Biome getBiome() {
        if (biome == null) biome = getChunk().getBiome(x, y, z);
        return biome;
    }

    @Override
    public @Nullable BlockEntity getBlockEntity() {
        if (!isBlockEntitySet) {
            blockEntity = getChunk().getBlockEntity(x, y, z);
            isBlockEntitySet = true;
        }
        return blockEntity;
    }

    @Override
    public boolean hasOceanFloorY() {
        return getChunk().hasOceanFloorHeights();
    }

    @Override
    public int getOceanFloorY() {
        return getChunk().getOceanFloorY(x, z);
    }

}
