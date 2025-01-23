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

import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.world.DimensionType;

public class BlockNeighborhood extends ExtendedBlock {

    private static final int DIAMETER = 8; // must be a power of 2
    private static final int DIAMETER_MASK = DIAMETER - 1;
    private static final int DIAMETER_SQUARED = DIAMETER * DIAMETER;

    private final ExtendedBlock[] neighborhood;

    private int thisIndex = -1;

    public BlockNeighborhood(BlockAccess blockAccess, ResourcePack resourcePack, RenderSettings renderSettings, DimensionType dimensionType) {
        super(blockAccess, resourcePack, renderSettings, dimensionType);

        this.neighborhood = new ExtendedBlock[DIAMETER * DIAMETER * DIAMETER];
    }

    @Override
    public void set(int x, int y, int z) {
        int i = index(x, y, z);
        if (i == thisIndex()) return;

        if (neighborhood[i] == null) neighborhood[i] = copy();
        neighborhood[i].set(x, y, z);
        copyFrom(neighborhood[i]);
        this.thisIndex = i;
    }

    public ExtendedBlock getNeighborBlock(int dx, int dy, int dz) {
        return getBlock(
                getX() + dx,
                getY() + dy,
                getZ() + dz
        );
    }

    private ExtendedBlock getBlock(int x, int y, int z) {
        int i = index(x, y, z);
        if (i == thisIndex()) return this;

        if (neighborhood[i] == null) neighborhood[i] = copy();
        neighborhood[i].set(x, y, z);
        return neighborhood[i];
    }

    private int thisIndex() {
        if (thisIndex == -1) thisIndex = index(getX(), getY(), getZ());
        return thisIndex;
    }

    private int index(int x, int y, int z) {
        return (x & DIAMETER_MASK) * DIAMETER_SQUARED +
               (y & DIAMETER_MASK) * DIAMETER +
               (z & DIAMETER_MASK);
    }

}
