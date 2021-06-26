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

import com.flowpowered.math.TrigMath;

public abstract class TileModel {
    private static final double GROW_MULTIPLIER = 1.5;

    private static final int FI_POSITION = 3 * 3;
    private double[] position;

    private int capacity;
    private int size;

    public TileModel(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity is negative");
        setCapacity(initialCapacity);
        clear();
    }

    public int size() {
        return size;
    }

    public int add(int count) {
        ensureCapacity(count);
        return this.size += count;
    }

    public void setPositions(
            int face,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            double x3, double y3, double z3
    ){
        int index = face * FI_POSITION;

        position[index        ] = x1;
        position[index     + 1] = y1;
        position[index     + 2] = z1;

        position[index + 3    ] = x2;
        position[index + 3 + 1] = y2;
        position[index + 3 + 2] = z2;

        position[index + 6    ] = x3;
        position[index + 6 + 1] = y3;
        position[index + 6 + 2] = z3;
    }

    public void rotate(
            int start, int count,
            float angle, float axisX, float axisY, float axisZ
    ) {

        // create quaternion
        double halfAngle = Math.toRadians(angle) * 0.5;
        double q = TrigMath.sin(halfAngle) / Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

        double   //quaternion
                qx = axisX * q,
                qy = axisY * q,
                qz = axisZ * q,
                qw = TrigMath.cos(halfAngle),
                qLength = Math.sqrt(qx * qx + qy * qy + qz * qz + qw * qw);

        // normalize quaternion
        qx /= qLength;
        qy /= qLength;
        qz /= qLength;
        qw /= qLength;

        rotateWithQuaternion(start, count, qx, qy, qz, qw);
    }

    public void rotate(
            int start, int count,
            float pitch, float yaw, float roll
    ) {

        double
                halfYaw = Math.toRadians(yaw) * 0.5,
                qy1 = TrigMath.sin(halfYaw),
                qw1 = TrigMath.cos(halfYaw),

                halfPitch = Math.toRadians(pitch) * 0.5,
                qx2 = TrigMath.sin(halfPitch),
                qw2 = TrigMath.cos(halfPitch),

                halfRoll = Math.toRadians(roll) * 0.5,
                qz3 = TrigMath.sin(halfRoll),
                qw3 = TrigMath.cos(halfRoll);

        // multiply 1 with 2
        double 
                qxA =   qw1 * qx2,
                qyA =   qy1 * qw2,
                qzA = - qy1 * qx2,
                qwA =   qw1 * qw2;

        // multiply with 3
        double
                qx = qxA * qw3 + qyA * qz3,
                qy = qyA * qw3 - qxA * qz3,
                qz = qwA * qz3 + qzA * qw3,
                qw = qwA * qw3 - qzA * qz3;

        rotateWithQuaternion(start, count, qx, qy, qz, qw);
    }

    private void rotateWithQuaternion(
            int start, int count,
            double qx, double qy, double qz, double qw
    ) {
        double x, y, z, px, py, pz, pw;
        int end = start + count, index;
        for (int face = start; face < end; face++) {
            index = face * FI_POSITION;

            x = position[index    ];
            y = position[index + 1];
            z = position[index + 2];

            px = qw * x + qy * z - qz * y;
            py = qw * y + qz * x - qx * z;
            pz = qw * z + qx * y - qy * x;
            pw = -qx * x - qy * y - qz * z;

            position[index    ] = pw * -qx + px * qw - py * qz + pz * qy;
            position[index + 1] = pw * -qy + py * qw - pz * qx + px * qz;
            position[index + 2] = pw * -qz + pz * qw - px * qy + py * qx;
        }
    }

    public void scale(
            int start, int count,
            float scale
    ) {
        int startIndex = start * FI_POSITION;
        int endIndex = count * FI_POSITION + startIndex;

        for (int i = startIndex; i < endIndex; i++)
            position[i] *= scale;
    }

    public void translate(
            int start, int count,
            double dx, double dy, double dz
    ) {
        int end = start + count, index;
        for (int face = start; face < end; face++) {
            index = face * FI_POSITION;
            for (int i = 0; i < 3; i++) {
                position[index    ] += dx;
                position[index + 1] += dy;
                position[index + 2] += dz;
            }
        }
    }

    public void clear() {
        this.size = 0;
    }

    protected void grow(int count) {
        double[] _position = position;

        int newCapacity = (int) (capacity * GROW_MULTIPLIER) + count;
        setCapacity(newCapacity);

        System.arraycopy(_position, 0, position, 0, size * FI_POSITION);
    }

    private void ensureCapacity(int count) {
        if (size + count > capacity){
            grow(count);
        }
    }

    protected void setCapacity(int capacity) {
        this.capacity = capacity;
        position =      new double  [capacity * FI_POSITION];
    }

}
