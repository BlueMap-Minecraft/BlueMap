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

import com.flowpowered.math.TrigMath;

public class MatrixM4f {

    public float m00 = 1f, m01, m02, m03;
    public float m10, m11 = 1f, m12, m13;
    public float m20, m21, m22 = 1f, m23;
    public float m30, m31, m32, m33 = 1f;

    public MatrixM4f set(
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33
    ) {
        this.m00 = m00; this.m01 = m01; this.m02 = m02; this.m03 = m03;
        this.m10 = m10; this.m11 = m11; this.m12 = m12; this.m13 = m13;
        this.m20 = m20; this.m21 = m21; this.m22 = m22; this.m23 = m23;
        this.m30 = m30; this.m31 = m31; this.m32 = m32; this.m33 = m33;
        return this;
    }

    public MatrixM4f copy(MatrixM4f m) {
        return set(
                m.m00, m.m01, m.m02, m.m03,
                m.m10, m.m11, m.m12, m.m13,
                m.m20, m.m21, m.m22, m.m23,
                m.m30, m.m31, m.m32, m.m33
        );
    }

    public MatrixM4f identity() {
        return set(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
        );
    }

    public MatrixM4f translate(float x, float y, float z) {
        return multiplyTo(
                1f, 0f, 0f, x,
                0f, 1f, 0f, y,
                0f, 0f, 1f, z,
                0f, 0f, 0f, 1f
        );
    }

    public MatrixM4f scale(float x, float y, float z) {
        return multiplyTo(
                x, 0f, 0f, 0f,
                0f, y, 0f, 0f,
                0f, 0f, z, 0f,
                0f, 0f, 0f, 1f
        );
    }

    public MatrixM4f rotate(float angle, float axisX, float axisY, float axisZ) {

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

        return rotateByQuaternion((float) qx, (float) qy, (float) qz, (float) qw);
    }

    public MatrixM4f rotateXYZ(float pitch, float yaw, float roll) {
        double
                halfPitch = Math.toRadians(pitch) * 0.5,
                sx = TrigMath.sin(halfPitch),
                cx = TrigMath.cos(halfPitch),

                halfYaw = Math.toRadians(yaw) * 0.5,
                sy = TrigMath.sin(halfYaw),
                cy = TrigMath.cos(halfYaw),

                halfRoll = Math.toRadians(roll) * 0.5,
                sz = TrigMath.sin(halfRoll),
                cz = TrigMath.cos(halfRoll),

                cycz = cy * cz,
                sysz = sy * sz,
                sycz = sy * cz,
                cysz = cy * sz;

        return rotateByQuaternion(
                (float) (sx * cycz + cx * sysz),
                (float) (cx * sycz - sx * cysz),
                (float) (cx * cysz + sx * sycz),
                (float) (cx * cycz - sx * sysz)
        );
    }

    public MatrixM4f rotateZYX(float pitch, float yaw, float roll) {
        double
                halfPitch = Math.toRadians(pitch) * 0.5,
                sx = TrigMath.sin(halfPitch),
                cx = TrigMath.cos(halfPitch),

                halfYaw = Math.toRadians(yaw) * 0.5,
                sy = TrigMath.sin(halfYaw),
                cy = TrigMath.cos(halfYaw),

                halfRoll = Math.toRadians(roll) * 0.5,
                sz = TrigMath.sin(halfRoll),
                cz = TrigMath.cos(halfRoll),

                cycz = cy * cz,
                sysz = sy * sz,
                sycz = sy * cz,
                cysz = cy * sz;

        return rotateByQuaternion(
                (float) (cx * cycz + sx * sysz),
                (float) (sx * cycz - cx * sysz),
                (float) (cx * sycz + sx * cysz),
                (float) (cx * cysz - sx * sycz)
        );
    }

    public MatrixM4f rotateYXZ(float pitch, float yaw, float roll) {
        double
                halfPitch = Math.toRadians(pitch) * 0.5,
                sx = TrigMath.sin(halfPitch),
                cx = TrigMath.cos(halfPitch),

                halfYaw = Math.toRadians(yaw) * 0.5,
                sy = TrigMath.sin(halfYaw),
                cy = TrigMath.cos(halfYaw),

                halfRoll = Math.toRadians(roll) * 0.5,
                sz = TrigMath.sin(halfRoll),
                cz = TrigMath.cos(halfRoll),

                cysx = cy * sx,
                sycx = sy * cx,
                sysx = sy * sx,
                cycx = cy * cx;

        return rotateByQuaternion(
                (float) (cysx * cz + sycx * sz),
                (float) (sycx * cz - cysx * sz),
                (float) (cycx * sz - sysx * cz),
                (float) (cycx * cz + sysx * sz)
        );
    }

    public MatrixM4f rotateByQuaternion(float qx, float qy, float qz, float qw) {
        return multiplyTo(
                1 - 2 * qy * qy - 2 * qz * qz,
                2 * qx * qy - 2 * qw * qz,
                2 * qx * qz + 2 * qw * qy,
                0,
                2 * qx * qy + 2 * qw * qz,
                1 - 2 * qx * qx - 2 * qz * qz,
                2 * qy * qz - 2 * qw * qx,
                0,
                2 * qx * qz - 2 * qw * qy,
                2 * qy * qz + 2 * qx * qw,
                1 - 2 * qx * qx - 2 * qy * qy,
                0,
                0, 0, 0, 1
        );
    }

    public MatrixM4f multiply(
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33
    ) {
        return set (
                this.m00 * m00 + this.m01 * m10 + this.m02 * m20 + this.m03 * m30,
                this.m00 * m01 + this.m01 * m11 + this.m02 * m21 + this.m03 * m31,
                this.m00 * m02 + this.m01 * m12 + this.m02 * m22 + this.m03 * m32,
                this.m00 * m03 + this.m01 * m13 + this.m02 * m23 + this.m03 * m33,
                this.m10 * m00 + this.m11 * m10 + this.m12 * m20 + this.m13 * m30,
                this.m10 * m01 + this.m11 * m11 + this.m12 * m21 + this.m13 * m31,
                this.m10 * m02 + this.m11 * m12 + this.m12 * m22 + this.m13 * m32,
                this.m10 * m03 + this.m11 * m13 + this.m12 * m23 + this.m13 * m33,
                this.m20 * m00 + this.m21 * m10 + this.m22 * m20 + this.m23 * m30,
                this.m20 * m01 + this.m21 * m11 + this.m22 * m21 + this.m23 * m31,
                this.m20 * m02 + this.m21 * m12 + this.m22 * m22 + this.m23 * m32,
                this.m20 * m03 + this.m21 * m13 + this.m22 * m23 + this.m23 * m33,
                this.m30 * m00 + this.m31 * m10 + this.m32 * m20 + this.m33 * m30,
                this.m30 * m01 + this.m31 * m11 + this.m32 * m21 + this.m33 * m31,
                this.m30 * m02 + this.m31 * m12 + this.m32 * m22 + this.m33 * m32,
                this.m30 * m03 + this.m31 * m13 + this.m32 * m23 + this.m33 * m33
        );
    }

    public MatrixM4f multiplyTo(
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33
    ) {
        return set (
                m00 * this.m00 + m01 * this.m10 + m02 * this.m20 + m03 * this.m30,
                m00 * this.m01 + m01 * this.m11 + m02 * this.m21 + m03 * this.m31,
                m00 * this.m02 + m01 * this.m12 + m02 * this.m22 + m03 * this.m32,
                m00 * this.m03 + m01 * this.m13 + m02 * this.m23 + m03 * this.m33,
                m10 * this.m00 + m11 * this.m10 + m12 * this.m20 + m13 * this.m30,
                m10 * this.m01 + m11 * this.m11 + m12 * this.m21 + m13 * this.m31,
                m10 * this.m02 + m11 * this.m12 + m12 * this.m22 + m13 * this.m32,
                m10 * this.m03 + m11 * this.m13 + m12 * this.m23 + m13 * this.m33,
                m20 * this.m00 + m21 * this.m10 + m22 * this.m20 + m23 * this.m30,
                m20 * this.m01 + m21 * this.m11 + m22 * this.m21 + m23 * this.m31,
                m20 * this.m02 + m21 * this.m12 + m22 * this.m22 + m23 * this.m32,
                m20 * this.m03 + m21 * this.m13 + m22 * this.m23 + m23 * this.m33,
                m30 * this.m00 + m31 * this.m10 + m32 * this.m20 + m33 * this.m30,
                m30 * this.m01 + m31 * this.m11 + m32 * this.m21 + m33 * this.m31,
                m30 * this.m02 + m31 * this.m12 + m32 * this.m22 + m33 * this.m32,
                m30 * this.m03 + m31 * this.m13 + m32 * this.m23 + m33 * this.m33
        );
    }

    public MatrixM4f multiplyTo(MatrixM3f m) {
        return set (
                m.m00 * this.m00 + m.m01 * this.m10 + m.m02 * this.m20,
                m.m00 * this.m01 + m.m01 * this.m11 + m.m02 * this.m21,
                m.m00 * this.m02 + m.m01 * this.m12 + m.m02 * this.m22,
                m.m00 * this.m03 + m.m01 * this.m13 + m.m02 * this.m23,
                m.m10 * this.m00 + m.m11 * this.m10 + m.m12 * this.m20,
                m.m10 * this.m01 + m.m11 * this.m11 + m.m12 * this.m21,
                m.m10 * this.m02 + m.m11 * this.m12 + m.m12 * this.m22,
                m.m10 * this.m03 + m.m11 * this.m13 + m.m12 * this.m23,
                m.m20 * this.m00 + m.m21 * this.m10 + m.m22 * this.m20,
                m.m20 * this.m01 + m.m21 * this.m11 + m.m22 * this.m21,
                m.m20 * this.m02 + m.m21 * this.m12 + m.m22 * this.m22,
                m.m20 * this.m03 + m.m21 * this.m13 + m.m22 * this.m23,
                this.m30,
                this.m31,
                this.m32,
                this.m33
        );
    }

}
