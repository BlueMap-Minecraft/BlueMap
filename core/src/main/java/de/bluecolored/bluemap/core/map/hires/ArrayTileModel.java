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

import com.flowpowered.math.TrigMath;
import de.bluecolored.bluemap.core.util.InstancePool;
import de.bluecolored.bluemap.core.util.MergeSort;
import de.bluecolored.bluemap.core.util.math.MatrixM3f;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;

import java.time.Duration;
import java.time.Instant;

public class ArrayTileModel implements TileModel {
    private static final float GROW_MULTIPLIER = 1.5f;
    private static final int MAX_CAPACITY = 1000000;

    private static final float SHRINK_MULTIPLIER = 1 / GROW_MULTIPLIER;
    private static final Duration SHRINK_TIME = Duration.ofMinutes(1);

    private static final InstancePool<ArrayTileModel> INSTANCE_POOL = new InstancePool<>(
            () -> new ArrayTileModel(100),
            model -> {
                Instant now = Instant.now();

                if ((float) model.size / model.capacity > SHRINK_MULTIPLIER)
                    model.lastCapacityUse = now;
                else if (model.lastCapacityUse.plus(SHRINK_TIME).isBefore(now))
                    return null; // drop model

                model.clear();
                return model;
            },
            SHRINK_TIME
    );

    // attributes         per-vertex * per-face
    static final int
            FI_POSITION =          3 * 3,
            FI_UV =                2 * 3,
            FI_AO =                    3,
            FI_COLOR =             3    ,
            FI_SUNLIGHT =       1       ,
            FI_BLOCKLIGHT =     1       ,
            FI_MATERIAL_INDEX = 1       ;

    private int capacity;
    int size;

    float[] position;
    float[] color, uv, ao;
    byte[] sunlight, blocklight;
    int[] materialIndex, materialIndexSort, materialIndexSortSupport;

    private transient Instant lastCapacityUse = Instant.now();

    public ArrayTileModel(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity is negative");
        setCapacity(initialCapacity);
        clear();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int add(int count) {
        ensureCapacity(count);
        int start = this.size;
        this.size += count;
        return start;
    }

    @Override
    public ArrayTileModel setPositions(
            int face,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3
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

        return this;
    }

    @Override
    public ArrayTileModel setUvs(
            int face,
            float u1, float v1,
            float u2, float v2,
            float u3, float v3
    ){
        int index = face * FI_UV;

        uv[index        ] = u1;
        uv[index     + 1] = v1;

        uv[index + 2    ] = u2;
        uv[index + 2 + 1] = v2;

        uv[index + 4    ] = u3;
        uv[index + 4 + 1] = v3;

        return this;
    }

    @Override
    public ArrayTileModel setAOs(
            int face,
            float ao1, float ao2, float ao3
    ) {
        int index = face * FI_AO;

        ao[index    ] = ao1;
        ao[index + 1] = ao2;
        ao[index + 2] = ao3;

        return this;
    }

    @Override
    public ArrayTileModel setColor(
            int face,
            float r, float g, float b
    ){
        int index = face * FI_COLOR;

        color[index    ] = r;
        color[index + 1] = g;
        color[index + 2] = b;

        return this;
    }

    @Override
    public ArrayTileModel setSunlight(int face, int sl) {
        sunlight[face * FI_SUNLIGHT] = (byte) sl;
        return this;
    }

    @Override
    public ArrayTileModel setBlocklight(int face, int bl) {
        blocklight[face * FI_BLOCKLIGHT] = (byte) bl;
        return this;
    }

    @Override
    public ArrayTileModel setMaterialIndex(int face, int m) {
        materialIndex[face * FI_MATERIAL_INDEX] = m;
        return this;
    }

    public ArrayTileModel invertOrientation(int face) {
        int index;
        float x, y, z;

        // swap first and last positions
        index = face * FI_POSITION;

        x = position[index    ];
        y = position[index + 1];
        z = position[index + 2];

        position[index    ] = position[index + 6    ];
        position[index + 1] = position[index + 6 + 1];
        position[index + 2] = position[index + 6 + 2];

        position[index + 6    ] = x;
        position[index + 6 + 1] = y;
        position[index + 6 + 2] = z;

        // swap first and last uvs
        index = face * FI_UV;
        x = uv[index    ];
        y = uv[index + 1];

        uv[index    ] = uv[index + 4    ];
        uv[index + 1] = uv[index + 4 + 1];

        uv[index + 4    ] = x;
        uv[index + 4 + 1] = y;

        return this;
    }

    @Override
    public ArrayTileModel rotate(
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

        return rotateByQuaternion(start, count, qx, qy, qz, qw);
    }

    @Override
    public ArrayTileModel rotate(
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

        return rotateByQuaternion(start, count, qx, qy, qz, qw);
    }

    @Override
    public ArrayTileModel rotateByQuaternion(
            int start, int count,
            double qx, double qy, double qz, double qw
    ) {
        double x, y, z, px, py, pz, pw;
        int end = start + count, index;
        for (int face = start; face < end; face++) {
            for (int i = 0; i < 3; i++) {
                index = face * FI_POSITION + i * 3;

                x = position[index];
                y = position[index + 1];
                z = position[index + 2];

                px = qw * x + qy * z - qz * y;
                py = qw * y + qz * x - qx * z;
                pz = qw * z + qx * y - qy * x;
                pw = -qx * x - qy * y - qz * z;

                position[index] = (float) (pw * -qx + px * qw - py * qz + pz * qy);
                position[index + 1] = (float) (pw * -qy + py * qw - pz * qx + px * qz);
                position[index + 2] = (float) (pw * -qz + pz * qw - px * qy + py * qx);
            }
        }

        return this;
    }

    @Override
    public ArrayTileModel scale(
            int start, int count,
            float sx, float sy, float sz
    ) {
        int end = start + count, index;
        for (int face = start; face < end; face++) {
            for (int i = 0; i < 3; i++) {
                index = face * FI_POSITION + i * 3;
                position[index    ] *= sx;
                position[index + 1] *= sy;
                position[index + 2] *= sz;
            }
        }

        return this;
    }

    @Override
    public ArrayTileModel translate(
            int start, int count,
            float dx, float dy, float dz
    ) {
        int end = start + count, index;
        for (int face = start; face < end; face++) {
            for (int i = 0; i < 3; i++) {
                index = face * FI_POSITION + i * 3;
                position[index    ] += dx;
                position[index + 1] += dy;
                position[index + 2] += dz;
            }
        }

        return this;
    }

    @Override
    public ArrayTileModel transform(int start, int count, MatrixM3f t) {
        return transform(start, count,
            t.m00, t.m01, t.m02,
            t.m10, t.m11, t.m12,
            t.m20, t.m21, t.m22
        );
    }

    @Override
    public ArrayTileModel transform(
            int start, int count,
            float m00, float m01, float m02,
            float m10, float m11, float m12,
            float m20, float m21, float m22
    ) {
        return transform(start, count,
                m00, m01, m02, 0,
                m10, m11, m12, 0,
                m20, m21, m22, 0,
                0, 0, 0, 1
        );
    }

    @Override
    public ArrayTileModel transform(int start, int count, MatrixM4f t) {
        return transform(start, count,
                t.m00, t.m01, t.m02, t.m03,
                t.m10, t.m11, t.m12, t.m13,
                t.m20, t.m21, t.m22, t.m23,
                t.m30, t.m31, t.m32, t.m33
        );
    }

    @Override
    public ArrayTileModel transform(
            int start, int count,
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33
    ) {
        int end = start + count, index;
        float x, y, z;
        for (int face = start; face < end; face++) {
            for (int i = 0; i < 3; i++) {
                index = face * FI_POSITION + i * 3;
                x = position[index    ];
                y = position[index + 1];
                z = position[index + 2];

                position[index    ] = m00 * x + m01 * y + m02 * z + m03;
                position[index + 1] = m10 * x + m11 * y + m12 * z + m13;
                position[index + 2] = m20 * x + m21 * y + m22 * z + m23;
            }
        }

        return this;
    }

    @Override
    public ArrayTileModel reset(int size) {
        this.size = size;
        return this;
    }

    @Override
    public ArrayTileModel clear() {
        this.size = 0;
        return this;
    }

    private void ensureCapacity(int count) {
        if (size + count > capacity){
            float[] _position = position;
            float[] _color = color, _uv = uv, _ao = ao;
            byte[] _sunlight = sunlight, _blocklight = blocklight;
            int[] _materialIndex = materialIndex;

            int newCapacity = (int) (capacity * GROW_MULTIPLIER) + count;
            if (newCapacity > MAX_CAPACITY) newCapacity = MAX_CAPACITY;
            if (size + count > newCapacity)
                throw new MaxCapacityReachedException("Capacity out of range: " + (size + count));
            setCapacity(newCapacity);

            System.arraycopy(_position,         0, position,        0, size * FI_POSITION);
            System.arraycopy(_uv,               0, uv,              0, size * FI_UV);
            System.arraycopy(_ao,               0, ao,              0, size * FI_AO);

            System.arraycopy(_color,            0, color,           0, size * FI_COLOR);
            System.arraycopy(_sunlight,         0, sunlight,        0, size * FI_SUNLIGHT);
            System.arraycopy(_blocklight,       0, blocklight,      0, size * FI_BLOCKLIGHT);
            System.arraycopy(_materialIndex,    0, materialIndex,   0, size * FI_MATERIAL_INDEX);
        }
    }

    private void setCapacity(int capacity) {
        if (capacity > MAX_CAPACITY)
            throw new MaxCapacityReachedException("Capacity out of range: " + capacity);

        this.capacity = capacity;

        // attributes                capacity * per-vertex * per-face
        position =      new float   [capacity * FI_POSITION];
        uv =            new float   [capacity * FI_UV];
        ao =            new float   [capacity * FI_AO];

        color =         new float   [capacity * FI_COLOR];
        sunlight =      new byte    [capacity * FI_SUNLIGHT];
        blocklight =    new byte    [capacity * FI_BLOCKLIGHT];
        materialIndex = new int     [capacity * FI_MATERIAL_INDEX];

        materialIndexSort = new int[materialIndex.length];
        materialIndexSortSupport = new int [materialIndex.length];
    }

    @Override
    public void sort() {
        if (size <= 1) return; // nothing to sort

        // initialize material-index-sort
        for (int i = 0; i < size; i++) {
            materialIndexSort[i] = i;
            materialIndexSortSupport[i] = i;
        }

        // sort
        MergeSort.mergeSortInt(materialIndexSort, 0, size, this::compareMaterialIndex, materialIndexSortSupport);

        // move
        int s, c;
        for (int i = 0; i < size; i++) {
            s = materialIndexSort[i]; c = 0;
            while (s < i) {
                s = materialIndexSort[s];

                // should never happen, just making absolutely sure this can't get stuck in an endless loop
                if (c++ > size) throw new IllegalStateException();
            }
            swap(i, s);
        }
    }

    private int compareMaterialIndex(int i1, int i2) {
        return Integer.compare(materialIndex[i1], materialIndex[i2]);
    }

    private void swap(int face1, int face2) {
        int i, if1, if2, vi;
        float vf;
        byte vb;

        //swap positions
        if1 = face1 * FI_POSITION;
        if2 = face2 * FI_POSITION;
        for (i = 0; i < FI_POSITION; i++){
            vf = position[if1 + i];
            position[if1 + i] = position[if2 + i];
            position[if2 + i] = vf;
        }

        //swap uv
        if1 = face1 * FI_UV;
        if2 = face2 * FI_UV;
        for (i = 0; i < FI_UV; i++){
            vf = uv[if1 + i];
            uv[if1 + i] = uv[if2 + i];
            uv[if2 + i] = vf;
        }

        //swap ao
        if1 = face1 * FI_AO;
        if2 = face2 * FI_AO;
        for (i = 0; i < FI_AO; i++){
            vf = ao[if1 + i];
            ao[if1 + i] = ao[if2 + i];
            ao[if2 + i] = vf;
        }

        //swap color
        if1 = face1 * FI_COLOR;
        if2 = face2 * FI_COLOR;
        for (i = 0; i < FI_COLOR; i++){
            vf = color[if1 + i];
            color[if1 + i] = color[if2 + i];
            color[if2 + i] = vf;
        }

        //swap sunlight (assuming FI_SUNLIGHT = 1)
        vb = sunlight[face1];
        sunlight[face1] = sunlight[face2];
        sunlight[face2] = vb;

        //swap blocklight (assuming FI_BLOCKLIGHT = 1)
        vb = blocklight[face1];
        blocklight[face1] = blocklight[face2];
        blocklight[face2] = vb;

        //swap material-index (assuming FI_MATERIAL_INDEX = 1)
        vi = materialIndex[face1];
        materialIndex[face1] = materialIndex[face2];
        materialIndex[face2] = vi;
    }

    public static InstancePool<ArrayTileModel> instancePool() {
        return INSTANCE_POOL;
    }

}
