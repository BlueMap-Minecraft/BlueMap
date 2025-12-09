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

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.core.util.Tristate;

public class EllipseMask implements Mask {

    private final Vector2d center;
    private final double radiusSquaredX, radiusSquaredZ;
    private final int minY, maxY;

    public EllipseMask(Vector2d center, double radius, int minY, int maxY) {
        this.center = center;
        this.radiusSquaredX = radius * radius;
        this.radiusSquaredZ = radiusSquaredX;
        this.minY = minY;
        this.maxY = maxY;
    }

    public EllipseMask(Vector2d center, double radiusX, double radiusZ, int minY, int maxY) {
        this.center = center;
        this.radiusSquaredX = radiusX * radiusX;
        this.radiusSquaredZ = radiusZ * radiusZ;
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    public boolean test(int x, int y, int z) {
        return
                minY <= y &&
                maxY >= y &&
                testXZ(x, z);
    }

    public boolean testXZ(double x, double z) {
        x = x - center.getX();
        z = z - center.getY();
        return (x * x) / radiusSquaredX + (z * z) / radiusSquaredZ <= 1.0;
    }

    @Override
    public Tristate test(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return testY(minY, maxY).and(() -> testXZ(minX, minZ, maxX, maxZ));
    }

    public Tristate testXZ(int minX, int minZ, int maxX, int maxZ) {
        // if all corners are inside the circle then it's fully in
        if (
                testXZ(minX, minZ) &&
                testXZ(maxX, minZ) &&
                testXZ(minX, maxZ) &&
                testXZ(maxX, maxZ)
        ) return Tristate.TRUE;

        // if the closest point of the rectangle is outside then it's fully out
        double closestX = Math.clamp(center.getX(), minX, maxX);
        double closestZ = Math.clamp(center.getY(), minZ, maxZ);
        if (!testXZ(closestX, closestZ)) return Tristate.FALSE;

        // else its on the circles border
        return Tristate.UNDEFINED;
    }

    public Tristate testY(int minY, int maxY) {
        if (maxY < this.minY || minY > this.maxY) return Tristate.FALSE;
        if (minY >= this.minY && maxY <= this.maxY) return Tristate.TRUE;
        return Tristate.UNDEFINED;
    }

    @Override
    public boolean isEdge(int minX, int minZ, int maxX, int maxZ) {
        return testXZ(minX, minZ, maxX, maxZ) == Tristate.UNDEFINED;
    }

}
