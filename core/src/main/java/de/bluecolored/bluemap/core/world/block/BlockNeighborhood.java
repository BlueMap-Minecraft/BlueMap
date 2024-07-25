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
import de.bluecolored.bluemap.core.world.World;

public class BlockNeighborhood<T extends BlockNeighborhood<T>> extends ExtendedBlock<T> {

    private static final int DIAMETER = 8;
    private static final int DIAMETER_MASK = DIAMETER - 1;
    private static final int DIAMETER_SQUARED = DIAMETER * DIAMETER;

    private final ExtendedBlock<?>[] neighborhood;

    private int thisIndex;

    public BlockNeighborhood(ExtendedBlock<?> center) {
        super(center.getResourcePack(), center.getRenderSettings(), null, 0, 0, 0);
        copy(center);

        neighborhood = new ExtendedBlock[DIAMETER * DIAMETER * DIAMETER];
        init();
    }

    public BlockNeighborhood(ResourcePack resourcePack, RenderSettings renderSettings, World world, int x, int y, int z) {
        super(resourcePack, renderSettings, world, x, y, z);

        neighborhood = new ExtendedBlock[DIAMETER * DIAMETER * DIAMETER];
        init();
    }

    @Override
    public T set(int x, int y, int z) {
        return copy(getBlock(x, y, z));
    }

    @Override
    public T set(World world, int x, int y, int z) {
        if (getWorld() == world)
            return copy(getBlock(x, y, z));
        else
            return super.set(world, x, y, z);
    }

    @Override
    protected void reset() {
        super.reset();

        this.thisIndex = -1;
    }

    private void init() {
        this.thisIndex = -1;
        for (int i = 0; i < neighborhood.length; i++) {
            neighborhood[i] = new ExtendedBlock<>(this.getResourcePack(), this.getRenderSettings(), null, 0, 0, 0);
        }
    }

    public ExtendedBlock<?> getNeighborBlock(int dx, int dy, int dz) {
        return getBlock(
                getX() + dx,
                getY() + dy,
                getZ() + dz
        );
    }

    private ExtendedBlock<?> getBlock(int x, int y, int z) {
        int i = index(x, y, z);
        if (i == thisIndex()) return this;
        return neighborhood[i].set(getWorld(), x, y, z);
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
