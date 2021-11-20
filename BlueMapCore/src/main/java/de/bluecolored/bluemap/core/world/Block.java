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

public class Block<T extends Block<T>> {

    private World world;
    private int x, y, z;

    private Chunk chunk;

    private BlockState blockState;
    private final LightData lightData = new LightData(-1, -1);
    private String biomeId;

    public Block(World world, int x, int y, int z) {
        set(world, x, y, z);
    }

    public T set(World world, int x, int y, int z) {
        if (this.x == x && this.z == z && this.world == world){
            if (this.y == y) return self();
        } else {
            this.chunk = null; //only reset the chunk if x or z have changed
        }

        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;

        reset();

        return self();
    }

    public T set(int x, int y, int z) {
        if (this.x == x && this.z == z){
            if (this.y == y) return self();
        } else {
            this.chunk = null; //only reset the chunk if x or z have changed
        }

        this.x = x;
        this.y = y;
        this.z = z;

        reset();

        return self();
    }

    protected void reset() {
        this.blockState = null;
        this.lightData.set(-1, -1);
        this.biomeId = null;
    }

    public T add(int dx, int dy, int dz) {
        return set(x + dx, y + dy, z + dz);
    }

    public T copy(Block<?> source) {
        this.world = source.world;
        this.chunk = source.chunk;
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;

        reset();

        this.blockState = source.blockState;
        this.lightData.set(source.lightData.getSkyLight(), source.lightData.getBlockLight());
        this.biomeId = source.biomeId;

        return self();
    }

    /**
     * copy with offset
     */
    public T copy(Block<?> source, int dx, int dy, int dz) {
        this.world = source.world;
        this.x = source.x + dx;
        this.y = source.y + dy;
        this.z = source.z + dz;

        this.chunk = null;

        reset();

        return self();
    }

    public World getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public Chunk getChunk() {
        if (chunk == null) chunk = world.getChunkAtBlock(x, y, z);
        return chunk;
    }

    public BlockState getBlockState() {
        if (blockState == null) blockState = getChunk().getBlockState(x, y, z);
        return blockState;
    }

    public LightData getLightData() {
        if (lightData.getSkyLight() < 0) getChunk().getLightData(x, y, z, lightData);
        return lightData;
    }

    public String getBiomeId() {
        if (biomeId == null) biomeId = getChunk().getBiome(x, y, z);
        return biomeId;
    }

    public int getSunLightLevel() {
        return getLightData().getSkyLight();
    }

    public int getBlockLightLevel() {
        return getLightData().getBlockLight();
    }

    @Override
    public String toString() {
        if (world != null) {
            return "Block{" +
                   "world=" + world +
                   ", x=" + x +
                   ", y=" + y +
                   ", z=" + z +
                   ", chunk=" + getChunk() +
                   ", blockState=" + getBlockState() +
                   ", lightData=" + getLightData() +
                   ", biomeId=" + getBiomeId() +
                   '}';
        } else {
            return "Block{" +
                   "world=" + world +
                   ", x=" + x +
                   ", y=" + y +
                   ", z=" + z +
                   '}';
        }
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

}
