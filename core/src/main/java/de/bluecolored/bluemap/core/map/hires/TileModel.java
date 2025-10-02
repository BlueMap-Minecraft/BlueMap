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
package de.bluecolored.bluemap.core.map.hires;

import de.bluecolored.bluemap.core.util.math.MatrixM3f;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;

public interface TileModel {

    int size();

    int add(int count);

    TileModel setPositions(
            int face,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3
    );

    TileModel setUvs(
            int face,
            float u1, float v1,
            float u2, float v2,
            float u3, float v3
    );

    TileModel setAOs(
            int face,
            float ao1, float ao2, float ao3
    );

    TileModel setColor(
            int face,
            float r, float g, float b
    );

    TileModel setSunlight(int face, int sl);

    TileModel setBlocklight(int face, int bl);

    TileModel setMaterialIndex(int face, int m);

    TileModel invertOrientation(int face);

    TileModel rotate(
            int start, int count,
            float angle, float axisX, float axisY, float axisZ
    );

    TileModel rotate(
            int start, int count,
            float pitch, float yaw, float roll
    );

    TileModel rotateByQuaternion(
            int start, int count,
            double qx, double qy, double qz, double qw
    );

    TileModel scale(
            int start, int count,
            float sx, float sy, float sz
    );

    TileModel translate(
            int start, int count,
            float dx, float dy, float dz
    );

    TileModel transform(int start, int count, MatrixM3f t);

    TileModel transform(
            int start, int count,
            float m00, float m01, float m02,
            float m10, float m11, float m12,
            float m20, float m21, float m22
    );

    TileModel transform(int start, int count, MatrixM4f t);

    TileModel transform(
            int start, int count,
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33
    );

    default TileModel invertOrientation(int start, int count) {
        int end = start + count;
        for (int face = start; face < end; face++) {
            invertOrientation(face);
        }
        return this;
    }

    TileModel reset(int size);

    TileModel clear();

    void sort();

}
