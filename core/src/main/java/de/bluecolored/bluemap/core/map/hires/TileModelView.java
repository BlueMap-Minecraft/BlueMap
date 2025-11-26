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

public class TileModelView {

    private TileModel tileModel;
    private int start, size;

    public TileModelView(TileModel tileModel) {
        initialize(tileModel);
    }

    public TileModelView initialize(TileModel hiresTile, int start) {
        this.tileModel = hiresTile;
        this.start = start;
        this.size = hiresTile.size() - start;

        return this;
    }

    public TileModelView initialize(TileModel hiresTile) {
        this.tileModel = hiresTile;
        this.start = hiresTile.size();
        this.size = 0;

        return this;
    }

    public TileModelView initialize(int start) {
        this.start = start;
        this.size = tileModel.size() - start;

        return this;
    }

    public TileModelView initialize() {
        this.start = tileModel.size();
        this.size = 0;

        return this;
    }

    public TileModelView reset() {
        tileModel.reset(this.start);
        this.size = 0;

        return this;
    }

    public int add(int count) {
        int s = tileModel.add(count);
        if (s != start + size) throw new IllegalStateException("Size of HiresTileModel had external changes since view-initialisation!");
        this.size += count;
        return s;
    }

    public TileModelView rotate(float angle, float axisX, float axisY, float axisZ) {
        tileModel.rotate(start, size, angle, axisX, axisY, axisZ);
        return this;
    }

    public TileModelView rotateXYZ(float pitch, float yaw, float roll) {
        tileModel.rotateXYZ(start, size, pitch, yaw, roll);
        return this;
    }

    public TileModelView rotateZYX(float pitch, float yaw, float roll) {
        tileModel.rotateZYX(start, size, pitch, yaw, roll);
        return this;
    }

    public TileModelView rotateYXZ(float pitch, float yaw, float roll) {
        tileModel.rotateYXZ(start, size, pitch, yaw, roll);
        return this;
    }

    public TileModelView scale(float sx, float sy, float sz) {
        tileModel.scale(start, size, sx, sy, sz);
        return this;
    }

    public TileModelView translate(float dx, float dy, float dz) {
        tileModel.translate(start, size, dx, dy, dz);
        return this;
    }

    public TileModelView transform(MatrixM3f t) {
        tileModel.transform(start, size, t);
        return this;
    }

    public TileModelView transform(
            float m00, float m01, float m02,
            float m10, float m11, float m12,
            float m20, float m21, float m22
    ) {
        tileModel.transform(start, size,
                m00, m01, m02,
                m10, m11, m12,
                m20, m21, m22
        );
        return this;
    }

    public TileModelView transform(MatrixM4f t) {
        tileModel.transform(start, size, t);
        return this;
    }

    public TileModelView transform(
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33
    ) {
        tileModel.transform(start, size,
                m00, m01, m02, m03,
                m10, m11, m12, m13,
                m20, m21, m22, m23,
                m30, m31, m32, m33
        );
        return this;
    }

    public TileModelView invertOrientation() {
        tileModel.invertOrientation(start, size);
        return this;
    }

    public TileModel getTileModel() {
        return tileModel;
    }

    public int getStart() {
        return start;
    }

    public int getSize() {
        return size;
    }

}
