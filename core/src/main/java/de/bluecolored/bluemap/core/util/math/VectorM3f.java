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

import com.flowpowered.math.vector.Vector3i;

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

    public VectorM3f set(Vector3i v) {
        this.x = v.getX();
        this.y = v.getY();
        this.z = v.getZ();
        return this;
    }

    public VectorM3f set(VectorM3f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }

    public VectorM3f mul(float a) {
        this.x *= a;
        this.y *= a;
        this.z *= a;
        return this;
    }

    public VectorM3f cross(VectorM3f v) {
        return set(
                y * v.z - z * v.y,
                z * v.x - x * v.z,
                x * v.y - y * v.x
        );
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

    public VectorM3f absolute() {
        x = Math.abs(x);
        y = Math.abs(y);
        z = Math.abs(z);
        return this;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public float dot(VectorM3f v) {
        return x * v.x + y * v.y + z * v.z;
    }

    public float max() {
        return Math.max(x, Math.max(y, z));
    }

    public float min() {
        return Math.min(x, Math.min(y, z));
    }

}
