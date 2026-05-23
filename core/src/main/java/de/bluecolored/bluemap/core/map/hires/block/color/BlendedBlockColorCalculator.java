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
package de.bluecolored.bluemap.core.map.hires.block.color;

import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.block.BlockAccess;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

public class BlendedBlockColorCalculator implements BlockColorCalculator {

    private final BlockColorCalculator delegate;
    private final int
            blendMinX,
            blendMaxX,
            blendMinY,
            blendMaxY,
            blendMinZ,
            blendMaxZ;


    private final Color delegateColor = new Color();

    public BlendedBlockColorCalculator(BlockColorCalculator delegate) {
        this(delegate, 2, 1);
    }

    public BlendedBlockColorCalculator(BlockColorCalculator delegate, int horizontalBlend, int verticalBlend) {
        this.delegate = delegate;

        this.blendMinX = -horizontalBlend;
        this.blendMaxX = horizontalBlend;
        this.blendMinY = -verticalBlend;
        this.blendMaxY = verticalBlend;
        this.blendMinZ = -horizontalBlend;
        this.blendMaxZ = horizontalBlend;
    }

    @Override
    public Color getBlockColor(BlockAccess block, BlockState blockState, Color target) {
        target.set(0, 0, 0, 0, true);

        int dx, dy, dz;

        if (block instanceof BlockNeighborhood neighborhood) {
            for (dy = blendMinY; dy <= blendMaxY; dy++) {
                for (dx = blendMinX; dx <= blendMaxX; dx++) {
                    for (dz = blendMinZ; dz <= blendMaxZ; dz++) {
                        delegate.getBlockColor(neighborhood.getNeighborBlock(dx, dy, dz), blockState, delegateColor);
                        target.add(delegateColor);
                    }
                }
            }
        } else {
            int x = block.getX(), y = block.getY(), z = block.getZ();
            for (dy = blendMinY; dy <= blendMaxY; dy++) {
                for (dx = blendMinX; dx <= blendMaxX; dx++) {
                    for (dz = blendMinZ; dz <= blendMaxZ; dz++) {
                        block.set(x + dx, y + dy, z + dz);
                        delegate.getBlockColor(block, blockState, delegateColor);
                        target.add(delegateColor);
                    }
                }
            }
            block.set(x, y, z);
        }

        return target.flatten();
    }

}
