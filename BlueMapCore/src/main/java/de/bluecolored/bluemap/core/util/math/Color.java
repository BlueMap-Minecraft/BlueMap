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

import de.bluecolored.bluemap.api.debug.DebugDump;

@DebugDump
public class Color {

    public float r, g, b, a;
    public boolean premultiplied;

    public Color set(float r, float g, float b, float a, boolean premultiplied) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.premultiplied = premultiplied;
        return this;
    }

    public Color set(Color color) {
        this.r = color.r;
        this.g = color.g;
        this.b = color.b;
        this.a = color.a;
        this.premultiplied = color.premultiplied;
        return this;
    }

    public Color set(int color) {
        return set(color, false);
    }

    public Color set(int color, boolean premultiplied) {
        this.r = ((color >> 16) & 0xFF) / 255f;
        this.g = ((color >> 8) & 0xFF) / 255f;
        this.b = (color & 0xFF) / 255f;
        this.a = ((color >> 24) & 0xFF) / 255f;
        this.premultiplied = premultiplied;
        return this;
    }

    public int getInt() {
        int r = (int) (this.r * 255) & 0xFF;
        int g = (int) (this.g * 255) & 0xFF;
        int b = (int) (this.b * 255) & 0xFF;
        int a = (int) (this.a * 255) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public Color add(Color color) {
        if (color.a < 1f && !color.premultiplied){
            throw new IllegalArgumentException("Can only add premultiplied colors with alpha!");
        }

        premultiplied();

        this.r += color.r;
        this.g += color.g;
        this.b += color.b;
        this.a += color.a;

        return this;
    }

    public Color div(int divisor) {
        premultiplied();

        float p = 1f / divisor;
        this.r *= p;
        this.g *= p;
        this.b *= p;
        this.a *= p;

        return this;
    }

    public Color multiply(Color color) {
        if (color.premultiplied) premultiplied();
        else straight();

        this.r *= color.r;
        this.g *= color.g;
        this.b *= color.b;
        this.a *= color.a;

        return this;
    }

    public Color overlay(Color color) {
        if (color.a < 1f && !color.premultiplied) throw new IllegalArgumentException("Can only overlay premultiplied colors with alpha!");

        premultiplied();

        float p = 1 - color.a;
        this.a = p * this.a + color.a;
        this.r = p * this.r + color.r;
        this.g = p * this.g + color.g;
        this.b = p * this.b + color.b;

        return this;
    }

    public Color flatten() {
        if (this.a == 1f) return this;

        if (premultiplied && this.a > 0f) {
            float m = 1f / this.a;
            this.r *= m;
            this.g *= m;
            this.b *= m;
        }

        this.a = 1f;

        return this;
    }

    public Color premultiplied() {
        if (!premultiplied) {
            this.r *= this.a;
            this.g *= this.a;
            this.b *= this.a;
            this.premultiplied = true;
        }
        return this;
    }

    public Color straight() {
        if (premultiplied) {
            if (this.a > 0f) {
                float m = 1f / this.a;
                this.r *= m;
                this.g *= m;
                this.b *= m;
            }
            this.premultiplied = false;
        }
        return this;
    }

    @Override
    public String toString() {
        return "Color{" +
               "r=" + r +
               ", g=" + g +
               ", b=" + b +
               ", a=" + a +
               ", premultiplied=" + premultiplied +
               '}';
    }

}
