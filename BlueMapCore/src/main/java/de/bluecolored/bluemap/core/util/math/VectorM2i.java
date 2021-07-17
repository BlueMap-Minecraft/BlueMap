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

public class VectorM2i {

    public int x, y;

    public VectorM2i() {}

    public VectorM2i(VectorM2i from) {
        this.x = from.x;
        this.y = from.y;
    }

    public VectorM2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public VectorM2i set(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public VectorM2i normalize() {
        final float length = length();
        x /= length;
        y /= length;
        return this;
    }

    public VectorM2i add(int x, int y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public VectorM2i div(int x, int y) {
        this.x /= x;
        this.y /= y;
        return this;
    }

    public VectorM2i floorDiv(int x, int y) {
        this.x = Math.floorDiv(this.x, x);
        this.y = Math.floorDiv(this.y, y);
        return this;
    }

    public int length() {
        return (int) Math.sqrt(lengthSquared());
    }

    public int lengthSquared() {
        return x * x + y * y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VectorM2i vectorM2i = (VectorM2i) o;
        return x == vectorM2i.x && y == vectorM2i.y;
    }

    @Override
    public int hashCode() {
        return x ^ (y + 34985735);
    }

}
