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

import com.flowpowered.math.TrigMath;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.core.resources.adapter.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.resources.adapter.PostDeserialize;
import de.bluecolored.bluemap.core.util.math.Axis;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;

@SuppressWarnings("FieldMayBeFinal")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class Rotation {
    private static final Vector3f DEFAULT_ORIGIN = new Vector3f(8, 8, 8);
    private static final double FIT_TO_BLOCK_SCALE_MULTIPLIER = 2 - Math.sqrt(2);

    public static final Rotation ZERO = new Rotation();
    static { ZERO.init(); }

    private Vector3f origin = DEFAULT_ORIGIN;
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

    @PostDeserialize
    private void init() {
        Vector3i axisAngle = axis.toVector();

        matrix = new MatrixM4f();
        if (angle != 0f) {
            matrix.translate(-origin.getX(), -origin.getY(), -origin.getZ());
            matrix.rotate(
                    angle,
                    axisAngle.getX(),
                    axisAngle.getY(),
                    axisAngle.getZ()
            );

            if (rescale) {
                float scale = (float) (Math.abs(TrigMath.sin(angle * TrigMath.DEG_TO_RAD)) * FIT_TO_BLOCK_SCALE_MULTIPLIER);
                matrix.scale(
                        (1 - axisAngle.getX()) * scale + 1,
                        (1 - axisAngle.getY()) * scale + 1,
                        (1 - axisAngle.getZ()) * scale + 1
                );
            }

            matrix.translate(origin.getX(), origin.getY(), origin.getZ());
        }
    }

}
