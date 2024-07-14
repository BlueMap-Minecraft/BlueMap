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

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.TrigMath;

public class VectorM2f {

    public float x, y;

    public VectorM2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public VectorM2f set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public VectorM2f translate(float x, float y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public VectorM2f rotate(float sx, float sy) { //sx,sy should be normalized
        return set(x * sx - y * sy, y * sx + x * sy);
    }

    public VectorM2f transform(MatrixM3f t) {
        return set(
                t.m00 * x + t.m01 * y + t.m02,
                t.m10 * x + t.m11 * y + t.m12
        );
    }

    public VectorM2f normalize() {
        final float length = length();
        x /= length;
        y /= length;
        return this;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public float lengthSquared() {
        return x * x + y * y;
    }

    public float angleTo(float x, float y) {
        return (float) TrigMath.acos(
                (this.x * x + this.y * y) /
                (this.length() * Math.sqrt(x * x + y * y))
        );
    }

}
