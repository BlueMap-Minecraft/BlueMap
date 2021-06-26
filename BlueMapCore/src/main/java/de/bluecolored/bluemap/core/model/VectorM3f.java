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
package de.bluecolored.bluemap.core.model;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.vector.Vector3f;

@Deprecated
public class VectorM3f {

    public float x, y, z;

    public VectorM3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public VectorM3f(VectorM3f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public VectorM3f(Vector3f v) {
        this.x = v.getX();
        this.y = v.getY();
        this.z = v.getZ();
    }

    public void set(Vector3f v) {
        this.x = v.getX();
        this.y = v.getY();
        this.z = v.getZ();
    }

    public void add(VectorM3f translation) {
        this.x += translation.x;
        this.y += translation.y;
        this.z += translation.z;
    }

    public void add(Vector3f translation) {
        this.x += translation.getX();
        this.y += translation.getY();
        this.z += translation.getZ();
    }

    public void rotate(Quaternionf rotation) {
        final float length = rotation.length();
        if (Math.abs(length) < GenericMath.FLT_EPSILON) {
            throw new ArithmeticException("Cannot rotate by the zero quaternion");
        }
        final float nx = rotation.getX() / length;
        final float ny = rotation.getY() / length;
        final float nz = rotation.getZ() / length;
        final float nw = rotation.getW() / length;
        final float px = nw * x + ny * z - nz * y;
        final float py = nw * y + nz * x - nx * z;
        final float pz = nw * z + nx * y - ny * x;
        final float pw = -nx * x - ny * y - nz * z;

        this.x = pw * -nx + px * nw - py * nz + pz * ny;
        this.y = pw * -ny + py * nw - pz * nx + px * nz;
        this.z = pw * -nz + pz * nw - px * ny + py * nx;
    }

    public void transform(MatrixM3f t) {
        float lx = x, ly = y;
        this.x = t.m00 * lx + t.m01 * ly + t.m02 * z;
        this.y = t.m10 * lx + t.m11 * ly + t.m12 * z;
        this.z = t.m20 * lx + t.m21 * ly + t.m22 * z;
    }

    public void normalize() {
        final float length = length();
        if (Math.abs(length) < GenericMath.FLT_EPSILON) {
            throw new ArithmeticException("Cannot normalize the zero vector");
        }

        x /= length;
        y /= length;
        z /= length;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    public Vector3f toVector3f() {
        return new Vector3f(this.x, this.y, this.z);
    }

}
