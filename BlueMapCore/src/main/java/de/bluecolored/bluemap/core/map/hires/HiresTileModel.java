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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.core.util.InstancePool;
import de.bluecolored.bluemap.core.util.MergeSort;
import de.bluecolored.bluemap.core.util.math.MatrixM3f;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;
import de.bluecolored.bluemap.core.util.math.VectorM3f;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class HiresTileModel {
    private static final double GROW_MULTIPLIER = 1.5;

    // attributes         per-vertex * per-face
    private static final int
            FI_POSITION =          3 * 3,
            FI_UV =                2 * 3,
            FI_AO =                    3,
            FI_COLOR =             3    ,
            FI_SUNLIGHT =       1       ,
            FI_BLOCKLIGHT =     1       ,
            FI_MATERIAL_INDEX = 1       ;

    private static final InstancePool<HiresTileModel> INSTANCE_POOL = new InstancePool<>(
            () -> new HiresTileModel(100),
            HiresTileModel::clear
    );

    private int capacity;
    private int size;

    private double[] position;
    private float[] color, uv, ao;
    private byte[] sunlight, blocklight;
    private int[] materialIndex, materialIndexSort, materialIndexSortSupport;

    public HiresTileModel(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("initialCapacity is negative");
        setCapacity(initialCapacity);
        clear();
    }

    public int size() {
        return size;
    }

    public int add(int count) {
        ensureCapacity(count);
        int start = this.size;
        this.size += count;
        return start;
    }

    public HiresTileModel setPositions(
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

        return this;
    }

    public HiresTileModel setUvs(
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

    public HiresTileModel setAOs(
            int face,
            float ao1, float ao2, float ao3
    ) {
        int index = face * FI_AO;

        ao[index    ] = ao1;
        ao[index + 1] = ao2;
        ao[index + 2] = ao3;

        return this;
    }

    public HiresTileModel setColor(
            int face,
            float r, float g, float b
    ){
        int index = face * FI_COLOR;

        color[index    ] = r;
        color[index + 1] = g;
        color[index + 2] = b;

        return this;
    }

    public HiresTileModel setSunlight(int face, int sl) {
        sunlight[face * FI_SUNLIGHT] = (byte) sl;
        return this;
    }

    public HiresTileModel setBlocklight(int face, int bl) {
        blocklight[face * FI_BLOCKLIGHT] = (byte) bl;
        return this;
    }

    public HiresTileModel setMaterialIndex(int face, int m) {
        materialIndex[face * FI_MATERIAL_INDEX] = m;
        return this;
    }

    public HiresTileModel rotate(
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

    public HiresTileModel rotate(
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

    public HiresTileModel rotateByQuaternion(
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

                position[index] = pw * -qx + px * qw - py * qz + pz * qy;
                position[index + 1] = pw * -qy + py * qw - pz * qx + px * qz;
                position[index + 2] = pw * -qz + pz * qw - px * qy + py * qx;
            }
        }

        return this;
    }

    public HiresTileModel scale(
            int start, int count,
            double sx, double sy, double sz
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

    public HiresTileModel translate(
            int start, int count,
            double dx, double dy, double dz
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

    public HiresTileModel transform(int start, int count, MatrixM3f t) {
        return transform(start, count,
            t.m00, t.m01, t.m02,
            t.m10, t.m11, t.m12,
            t.m20, t.m21, t.m22
        );
    }

    public HiresTileModel transform(
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

    public HiresTileModel transform(int start, int count, MatrixM4f t) {
        return transform(start, count,
                t.m00, t.m01, t.m02, t.m03,
                t.m10, t.m11, t.m12, t.m13,
                t.m20, t.m21, t.m22, t.m23,
                t.m30, t.m31, t.m32, t.m33
        );
    }

    public HiresTileModel transform(
            int start, int count,
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33
    ) {
        int end = start + count, index;
        double x, y, z;
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

    public HiresTileModel reset(int size) {
        this.size = size;
        return this;
    }

    public HiresTileModel clear() {
        this.size = 0;
        return this;
    }

    private void ensureCapacity(int count) {
        if (size + count > capacity){
            double[] _position = position;
            float[] _color = color, _uv = uv, _ao = ao;
            byte[] _sunlight = sunlight, _blocklight = blocklight;
            int[] _materialIndex = materialIndex;

            int newCapacity = (int) (capacity * GROW_MULTIPLIER) + count;
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
        this.capacity = capacity;

        // attributes                capacity * per-vertex * per-face
        position =      new double  [capacity * FI_POSITION];
        uv =            new float   [capacity * FI_UV];
        ao =            new float   [capacity * FI_AO];

        color =         new float   [capacity * FI_COLOR];
        sunlight =      new byte    [capacity * FI_SUNLIGHT];
        blocklight =    new byte    [capacity * FI_BLOCKLIGHT];
        materialIndex = new int     [capacity * FI_MATERIAL_INDEX];

        materialIndexSort = new int[materialIndex.length];
        materialIndexSortSupport = new int [materialIndex.length];
    }

    public void writeBufferGeometryJson(OutputStream out) throws IOException {
        sort();

        Gson gson = new GsonBuilder().create();
        JsonWriter json = gson.newJsonWriter(new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), 81920));

        json.beginObject(); // main-object
        json.name("tileGeometry").beginObject(); // tile-geometry-object

        // set special values
        json.name("type").value("BufferGeometry");
        json.name("uuid").value(UUID.randomUUID().toString().toUpperCase());

        json.name("data").beginObject(); // data
        json.name("attributes").beginObject(); // attributes

        writePositionArray(json);
        writeNormalArray(json);
        writeColorArray(json);
        writeUvArray(json);
        writeAoArray(json);
        writeBlocklightArray(json);
        writeSunlightArray(json);

        json.endObject(); // attributes

        writeMaterialGroups(json);

        json.endObject(); // data
        json.endObject(); // tile-geometry-object
        json.endObject(); // main-object

        // save and return
        json.flush();
    }

    private void writePositionArray(JsonWriter json) throws IOException {
        json.name("position");
        json.beginObject();

        json.name("type").value("Float32Array");
        json.name("itemSize").value(3);
        json.name("normalized").value(false);

        json.name("array").beginArray();
        int posSize = size * FI_POSITION;
        for (int i = 0; i < posSize; i++) {
            writeRounded(json, position[i]);
        }
        json.endArray();
        json.endObject();
    }

    private void writeNormalArray(JsonWriter json) throws IOException {
        VectorM3f normal = new VectorM3f(0, 0, 0);

        json.name("normal");
        json.beginObject();

        json.name("type").value("Float32Array");
        json.name("itemSize").value(3);
        json.name("normalized").value(false);

        json.name("array").beginArray();

        int pi, i, j;
        for (i = 0; i < size; i++) {
            pi = i * FI_POSITION;
            calculateSurfaceNormal(
                    position[pi    ], position[pi + 1], position[pi + 2],
                    position[pi + 3], position[pi + 4], position[pi + 5],
                    position[pi + 6], position[pi + 7], position[pi + 8],
                    normal
            );

            for (j = 0; j < 3; j++) { // all 3 points
                writeRounded(json, normal.x);
                writeRounded(json, normal.y);
                writeRounded(json, normal.z);
            }
        }
        json.endArray();
        json.endObject();
    }

    private void writeColorArray(JsonWriter json) throws IOException {
        json.name("color");
        json.beginObject();

        json.name("type").value("Float32Array");
        json.name("itemSize").value(3);
        json.name("normalized").value(false);

        json.name("array").beginArray();
        int colorSize = size * FI_COLOR, i, j;
        for (i = 0; i < colorSize; i += 3) {
            for (j = 0; j < 3; j++) {
                writeRounded(json, color[i]);
                writeRounded(json, color[i + 1]);
                writeRounded(json, color[i + 2]);
            }
        }
        json.endArray();
        json.endObject();
    }

    private void writeUvArray(JsonWriter json) throws IOException {
        json.name("uv");
        json.beginObject();

        json.name("type").value("Float32Array");
        json.name("itemSize").value(2);
        json.name("normalized").value(false);

        json.name("array").beginArray();
        int uvSize = size * FI_UV;
        for (int i = 0; i < uvSize; i++) {
            writeRounded(json, uv[i]);
        }
        json.endArray();
        json.endObject();
    }

    private void writeAoArray(JsonWriter json) throws IOException {
        json.name("ao");
        json.beginObject();

        json.name("type").value("Float32Array");
        json.name("itemSize").value(1);
        json.name("normalized").value(false);

        json.name("array").beginArray();
        int aoSize = size * FI_AO;
        for (int i = 0; i < aoSize; i++) {
            writeRounded(json, ao[i]);
        }
        json.endArray();
        json.endObject();
    }

    private void writeBlocklightArray(JsonWriter json) throws IOException {
        json.name("blocklight");
        json.beginObject();

        json.name("type").value("Float32Array");
        json.name("itemSize").value(1);
        json.name("normalized").value(false);

        json.name("array").beginArray();
        int blSize = size * FI_BLOCKLIGHT;
        for (int i = 0; i < blSize; i++) {
            json.value(blocklight[i]);
            json.value(blocklight[i]);
            json.value(blocklight[i]);
        }
        json.endArray();
        json.endObject();
    }

    private void writeSunlightArray(JsonWriter json) throws IOException {
        json.name("sunlight");
        json.beginObject();

        json.name("type").value("Float32Array");
        json.name("itemSize").value(1);
        json.name("normalized").value(false);

        json.name("array").beginArray();
        int blSize = size * FI_SUNLIGHT;
        for (int i = 0; i < blSize; i++) {
            json.value(sunlight[i]);
            json.value(sunlight[i]);
            json.value(sunlight[i]);
        }
        json.endArray();
        json.endObject();
    }

    private void writeMaterialGroups(JsonWriter json) throws IOException {
        json.name("groups").beginArray(); // groups

        if (size > 0) {

            int miSize = size * FI_MATERIAL_INDEX, lastMaterial = materialIndex[0], material = lastMaterial, groupStart = 0;

            json.beginObject();
            json.name("materialIndex").value(material);
            json.name("start").value(0);

            for (int i = 1; i < miSize; i++) {
                material = materialIndex[i];

                if (material != lastMaterial) {
                   json.name("count").value((i - groupStart) * 3L);
                   json.endObject();

                   groupStart = i;

                   json.beginObject();
                   json.name("materialIndex").value(material);
                   json.name("start").value(groupStart * 3L);
                }

                lastMaterial = material;
            }

            json.name("count").value((miSize - groupStart) * 3L);
            json.endObject();

        }

        json.endArray(); // groups
    }

    private void writeRounded(JsonWriter json, double value) throws IOException {
        // rounding and remove ".0" to save string space
        double d = Math.round(value * 10000d) / 10000d;
        if (d == (long) d) json.value((long) d);
        else json.value(d);
    }

    private void sort() {
        if (size <= 1) return; // nothing to sort

        // initialize material-index-sort
        for (int i = 0; i < size; i++) {
            materialIndexSort[i] = i;
            materialIndexSortSupport[i] = i;
        }

        // sort
        MergeSort.mergeSortInt(materialIndexSort, 0, size, this::compareMaterialIndex, materialIndexSortSupport);

        // move
        int s;
        for (int i = 0; i < size; i++) {
            s = materialIndexSort[i];
            while (s < i) s = materialIndexSort[s];
            swap(i, s);
        }
    }

    private int compareMaterialIndex(int i1, int i2) {
        return Integer.compare(materialIndex[i1], materialIndex[i2]);
    }

    private void swap(int face1, int face2) {
        int i, if1, if2, vi;
        double vd;
        float vf;
        byte vb;

        //swap positions
        if1 = face1 * FI_POSITION;
        if2 = face2 * FI_POSITION;
        for (i = 0; i < FI_POSITION; i++){
            vd = position[if1 + i];
            position[if1 + i] = position[if2 + i];
            position[if2 + i] = vd;
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

        //swap material-index-sort (assuming FI_MATERIAL_INDEX = 1)
        //vi = materialIndexSort[face1];
        //materialIndexSort[face1] = materialIndexSort[face2];
        //materialIndexSort[face2] = vi;
    }

    private static void calculateSurfaceNormal(
            double p1x, double p1y, double p1z,
            double p2x, double p2y, double p2z,
            double p3x, double p3y, double p3z,
            VectorM3f target
    ){
        p2x -= p1x; p2y -= p1y; p2z -= p1z;
        p3x -= p1x; p3y -= p1y; p3z -= p1z;

        p1x = p2y * p3z - p2z * p3y;
        p1y = p2z * p3x - p2x * p3z;
        p1z = p2x * p3y - p2y * p3x;

        double length = Math.sqrt(p1x * p1x + p1y * p1y + p1z * p1z);
        p1x /= length;
        p1y /= length;
        p1z /= length;

        target.set((float) p1x, (float) p1y, (float) p1z);
    }

    public static InstancePool<HiresTileModel> instancePool() {
        return INSTANCE_POOL;
    }

}
