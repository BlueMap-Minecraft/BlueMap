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
package de.bluecolored.bluemap.core.util.math;

import java.util.Random;

public class SimplexNoise {

    private static final double SQRT_3 = Math.sqrt(3.0);
    private static final double F2 = 0.5 * (SQRT_3 - 1.0);
    private static final double G2 = (3.0 - SQRT_3) / 6.0;

    private static final int[][] GRADIENT = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}
    };

    private final int[] p = new int[256];

    public SimplexNoise(Random random) {

        // vanilla uses 3 doubles here so we need to use them too first
        random.nextDouble();
        random.nextDouble();
        random.nextDouble();

        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }

        for (int i = 0; i < 256; i++) {
            int offset = random.nextInt(256 - i);
            int swap = p[i];
            p[i] = p[i + offset];
            p[i + offset] = swap;
        }
    }

    private int p(int index) {
        return p[index & 255];
    }

    private double cornerNoise(int index, double x, double y, double z, double base) {
        double t = base - x * x - y * y - z * z;
        if (t < 0) return 0;
        t *= t;
        return t * t * dot(GRADIENT[index], x, y, z);
    }

    public double getValue(double x, double y) {
        double skew = (x + y) * F2;
        int i = (int) Math.floor(x + skew);
        int j = (int) Math.floor(y + skew);

        double unskew = (i + j) * G2;
        double x0 = x - (i - unskew);
        double y0 = y - (j - unskew);

        int i1, j1;
        if (x0 > y0) {
            i1 = 1;
            j1 = 0;
        } else {
            i1 = 0;
            j1 = 1;
        }

        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1 + 2 * G2;
        double y2 = y0 - 1 + 2 * G2;

        int ii = i & 255;
        int jj = j & 255;

        int gi0 = p(ii + p(jj)) % 12;
        int gi1 = p(ii + i1 + p(jj + j1)) % 12;
        int gi2 = p(ii + 1 + p(jj + 1)) % 12;

        double n0 = cornerNoise(gi0, x0, y0, 0, 0.5);
        double n1 = cornerNoise(gi1, x1, y1, 0, 0.5);
        double n2 = cornerNoise(gi2, x2, y2, 0, 0.5);

        return 70.0 * (n0 + n1 + n2);
    }

    private static double dot(int[] gradient, double x, double y, double z) {
        return gradient[0] * x + gradient[1] * y + gradient[2] * z;
    }

}
