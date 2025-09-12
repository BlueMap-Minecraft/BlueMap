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
package de.bluecolored.bluemap.core.map.mask;

import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.util.Tristate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BoxMask implements Mask {

    private final Vector3i min, max;

    @Override
    public boolean test(int x, int y, int z) {
        return
                testXZ(x, z) &&
                y >= min.getY() &&
                y <= max.getY();
    }

    public boolean testXZ(int x, int z) {
        return
                x >= min.getX() &&
                x <= max.getX() &&
                z >= min.getZ() &&
                z <= max.getZ();
    }

    @Override
    public Tristate test(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        if (
                minX >= min.getX() && maxX <= max.getX() &&
                minZ >= min.getZ() && maxZ <= max.getZ() &&
                minY >= min.getY() && maxY <= max.getY()
        ) return Tristate.TRUE;

        if (
                maxX < min.getX() || minX > max.getX() ||
                maxZ < min.getZ() || minZ > max.getZ() ||
                maxY < min.getY() || minY > max.getY()
        ) return Tristate.FALSE;

        return Tristate.UNDEFINED;
    }

    @Override
    public boolean isEdge(int minX, int minZ, int maxX, int maxZ) {
        return test(minX, min.getY(), minZ, maxX, max.getY(), maxZ) == Tristate.UNDEFINED;
    }

}
