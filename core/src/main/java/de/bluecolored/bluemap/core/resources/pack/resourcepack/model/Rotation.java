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
package de.bluecolored.bluemap.core.resources.pack.resourcepack.model;

import com.flowpowered.math.vector.Vector3f;
import de.bluecolored.bluemap.core.resources.adapter.PostDeserialize;
import de.bluecolored.bluemap.core.util.math.Axis;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import de.bluecolored.bluemap.core.util.math.VectorM3f;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@SuppressWarnings("FieldMayBeFinal")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Rotation {
    private static final Vector3f DEFAULT_ORIGIN = new Vector3f(8, 8, 8);

    public static final Rotation ZERO = new Rotation();
    static { ZERO.init(); }

    private Vector3f origin = DEFAULT_ORIGIN;
    private float x, y, z;
    private Axis axis = Axis.Y;
    private float angle = 0;
    private boolean rescale = false;

    private transient MatrixM4f matrix;

    public Rotation(Vector3f origin, Axis axis, float angle, boolean rescale) {
        this.origin = origin;
        this.axis = axis;
        this.angle = angle;
        this.rescale = rescale;
        init();
    }

    public Rotation(Vector3f origin, float x, float y, float z, boolean rescale) {
        this.origin = origin;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rescale = rescale;
        init();
    }

    @PostDeserialize
    private void init() {

        // angle/axis notation takes precedence
        if (angle != 0) {
            x = y = z = 0;
            switch (axis) {
                case X -> x = angle;
                case Y -> y = angle;
                case Z -> z = angle;
            }
        }

        matrix = new MatrixM4f();
        if (x != 0 || y != 0 || z != 0) {
            matrix.translate(-origin.getX(), -origin.getY(), -origin.getZ());
            matrix.rotateYXZ(x, y, z);

            if (rescale) {
                VectorM3f axisVector = new VectorM3f(0, 0, 0);
                float sX = 1f / axisVector.set(1, 0, 0).rotateAndScale(matrix).absolute().max();
                float sY = 1f / axisVector.set(0, 1, 0).rotateAndScale(matrix).absolute().max();
                float sZ = 1f / axisVector.set(0, 0, 1).rotateAndScale(matrix).absolute().max();

                matrix.identity();
                matrix.translate(-origin.getX(), -origin.getY(), -origin.getZ());
                matrix.scale(sX, sY, sZ);
                matrix.rotateYXZ(x, y, z);
            }

            matrix.translate(origin.getX(), origin.getY(), origin.getZ());
        }
    }

}
