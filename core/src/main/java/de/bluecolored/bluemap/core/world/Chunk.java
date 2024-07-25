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

import de.bluecolored.bluemap.core.world.biome.Biome;
import de.bluecolored.bluemap.core.world.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface Chunk {

    Chunk EMPTY_CHUNK = new Chunk() {};
    Chunk ERRORED_CHUNK = new Chunk() {};

    default boolean isGenerated() {
        return false;
    }

    default boolean hasLightData() {
        return false;
    }

    default long getInhabitedTime() {
        return 0;
    }

    default BlockState getBlockState(int x, int y, int z) {
        return BlockState.AIR;
    }

    default LightData getLightData(int x, int y, int z, LightData target) {
        return target.set(0, 0);
    }

    default Biome getBiome(int x, int y, int z) {
        return Biome.DEFAULT;
    }

    default int getMaxY(int x, int z) {
        return 255;
    }

    default int getMinY(int x, int z) {
        return 0;
    }

    default boolean hasWorldSurfaceHeights() {
        return false;
    }

    default int getWorldSurfaceY(int x, int z) { return 0; }

    default boolean hasOceanFloorHeights() {
        return false;
    }

    default int getOceanFloorY(int x, int z) { return 0; }

    default @Nullable BlockEntity getBlockEntity(int x, int y, int z) { return null; }

    default void iterateBlockEntities(Consumer<BlockEntity> consumer) { }

}
