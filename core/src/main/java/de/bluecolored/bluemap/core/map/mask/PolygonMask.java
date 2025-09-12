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
import de.bluecolored.bluemap.api.math.Shape;
import de.bluecolored.bluemap.core.util.Tristate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PolygonMask implements Mask {

    private final Shape shape;
    private final int minY, maxY;

    @Override
    public boolean test(int x, int y, int z) {
        return
                minY <= y &&
                maxY >= y &&
                testXZ(x, z);
    }

    public boolean testXZ(int x, int z) {
        boolean contains = false;
        Vector2d[] points = shape.getPoints();
        for (int i = 0, j = points.length - 1; i < points.length; i++) {
            double x1 = points[i].getX(), x2 = points[j].getX();
            double z1 = points[i].getY(), z2 = points[j].getY();

            if (
                    ((z1 > z) != (z2 > z)) &&
                    (x < (x2 - x1) * (z - z1) / (z2 - z1) + x1)
            ) contains = !contains;

            j = i;
        }
        return contains;
    }

    @Override
    public Tristate test(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return testY(minY, maxY).and(() -> testXZ(minX, minZ, maxX, maxZ));
    }

    public Tristate testXZ(int minX, int minZ, int maxX, int maxZ) {
        Vector2d[] points = shape.getPoints();
        for (int i = 0, j = points.length - 1; i < points.length; i++) {
            double x1 = points[i].getX(), x2 = points[j].getX();
            double z1 = points[i].getY(), z2 = points[j].getY();

            // check polygon-line collision with all 4 sides of the rectangle
            if (linesCollide(minX, minZ, minX, maxZ, x1, z1, x2, z2)) return Tristate.UNDEFINED;
            if (linesCollide(minX, maxZ, maxX, maxZ, x1, z1, x2, z2)) return Tristate.UNDEFINED;
            if (linesCollide(maxX, maxZ, maxX, minZ, x1, z1, x2, z2)) return Tristate.UNDEFINED;
            if (linesCollide(maxX, minZ, minX, minZ, x1, z1, x2, z2)) return Tristate.UNDEFINED;

            j = i;
        }

        // no collision: check if any point of the rectangle is inside or outside
        return Tristate.valueOf(testXZ(minX, minZ));
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

    private static boolean linesCollide(
            double xA1, double yA1, double xA2, double yA2,
            double xB1, double yB1, double xB2, double yB2
    ) {
        double v = (yB2 - yB1) * (xA2 - xA1) - (xB2 - xB1) * (yA2 - yA1);
        double uA = ((xB2 - xB1) * (yA1 - yB1) - (yB2 - yB1) * (xA1 - xB1)) / v;
        double uB = ((xA2 - xA1) * (yA1 - yB1) - (yA2 - yA1) * (xA1 - xB1)) / v;
        return uA >= 0 && uA <= 1 && uB >= 0 && uB <= 1;
    }

}
