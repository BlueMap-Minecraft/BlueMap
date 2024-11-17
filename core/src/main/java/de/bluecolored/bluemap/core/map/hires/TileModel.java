package de.bluecolored.bluemap.core.map.hires;

import de.bluecolored.bluemap.core.util.math.MatrixM3f;
import de.bluecolored.bluemap.core.util.math.MatrixM4f;

public interface TileModel {

    int size();

    int add(int count);

    TileModel setPositions(
            int face,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3
    );

    TileModel setUvs(
            int face,
            float u1, float v1,
            float u2, float v2,
            float u3, float v3
    );

    TileModel setAOs(
            int face,
            float ao1, float ao2, float ao3
    );

    TileModel setColor(
            int face,
            float r, float g, float b
    );

    TileModel setSunlight(int face, int sl);

    TileModel setBlocklight(int face, int bl);

    TileModel setMaterialIndex(int face, int m);

    TileModel rotate(
            int start, int count,
            float angle, float axisX, float axisY, float axisZ
    );

    TileModel rotate(
            int start, int count,
            float pitch, float yaw, float roll
    );

    TileModel rotateByQuaternion(
            int start, int count,
            double qx, double qy, double qz, double qw
    );

    TileModel scale(
            int start, int count,
            float sx, float sy, float sz
    );

    TileModel translate(
            int start, int count,
            float dx, float dy, float dz
    );

    TileModel transform(int start, int count, MatrixM3f t);

    TileModel transform(
            int start, int count,
            float m00, float m01, float m02,
            float m10, float m11, float m12,
            float m20, float m21, float m22
    );

    TileModel transform(int start, int count, MatrixM4f t);

    TileModel transform(
            int start, int count,
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33
    );

    TileModel reset(int size);

    TileModel clear();

    void sort();

}
