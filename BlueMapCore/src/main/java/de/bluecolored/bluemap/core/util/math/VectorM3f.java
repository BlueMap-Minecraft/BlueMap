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

public class VectorM3f {

    public float x, y, z;

    public VectorM3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public VectorM3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public VectorM3f transform(MatrixM3f t) {
        return set(
                t.m00 * x + t.m01 * y + t.m02 * z,
                t.m10 * x + t.m11 * y + t.m12 * z,
                t.m20 * x + t.m21 * y + t.m22 * z
        );
    }

    public VectorM3f transform(MatrixM4f t) {
        return set(
                t.m00 * x + t.m01 * y + t.m02 * z + t.m03,
                t.m10 * x + t.m11 * y + t.m12 * z + t.m13,
                t.m20 * x + t.m21 * y + t.m22 * z + t.m23
        );
    }

    public VectorM3f rotateAndScale(MatrixM4f t) {
        return set(
                t.m00 * x + t.m01 * y + t.m02 * z,
                t.m10 * x + t.m11 * y + t.m12 * z,
                t.m20 * x + t.m21 * y + t.m22 * z
        );
    }

    public VectorM3f normalize() {
        final float length = length();
        x /= length;
        y /= length;
        z /= length;
        return this;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

}
