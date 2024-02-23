package de.bluecolored.bluemap.core.map.hires;

import de.bluecolored.bluemap.core.util.math.VectorM3f;
import de.bluecolored.bluemap.core.util.stream.CountingOutputStream;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public class PRBMWriter implements Closeable {

    private static final int FORMAT_VERSION = 1;
    private static final int HEADER_BITS = 0b0_0_0_00111; // indexed (no) _ indices-type (-) _ endianness (little) _ attribute-nr (7)

    private static final int ATTRIBUTE_TYPE_FLOAT = 0;
    private static final int ATTRIBUTE_TYPE_INTEGER = 1 << 7;

    private static final int ATTRIBUTE_NOT_NORMALIZED = 0;
    private static final int ATTRIBUTE_NORMALIZED = 1 << 6;

    private static final int ATTRIBUTE_CARDINALITY_SCALAR = 0;
    private static final int ATTRIBUTE_CARDINALITY_2D_VEC = 1 << 4;
    private static final int ATTRIBUTE_CARDINALITY_3D_VEC = 2 << 4;
    private static final int ATTRIBUTE_CARDINALITY_4D_VEC = 3 << 4;

    private static final int ATTRIBUTE_ENCODING_SIGNED_32BIT_FLOAT = 1;
    private static final int ATTRIBUTE_ENCODING_SIGNED_8BIT_INT = 3;
    private static final int ATTRIBUTE_ENCODING_SIGNED_16BIT_INT = 4;
    private static final int ATTRIBUTE_ENCODING_SIGNED_32BIT_INT = 6;
    private static final int ATTRIBUTE_ENCODING_UNSIGNED_8BIT_INT = 7;
    private static final int ATTRIBUTE_ENCODING_UNSIGNED_16BIT_INT = 8;
    private static final int ATTRIBUTE_ENCODING_UNSIGNED_32BIT_INT = 10;

    private final CountingOutputStream out;

    public PRBMWriter(OutputStream out) {
        this.out = new CountingOutputStream(out);
    }

    public void write(TileModel model) throws IOException {
        out.write(FORMAT_VERSION); // version - 1 byte
        out.write(HEADER_BITS); // format info - 1 byte
        write3byteValue(model.size * 3); // number of values - 3 bytes
        write3byteValue(0); // number of indices (0 for non-indexed) - 3 bytes

        writePositionArray(model);
        writeNormalArray(model);
        writeColorArray(model);
        writeUvArray(model);
        writeAoArray(model);
        writeBlocklightArray(model);
        writeSunlightArray(model);

        writeMaterialGroups(model);
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    private void writePositionArray(TileModel model) throws IOException {
        float[] position = model.position;

        writeString("position");
        out.write(
                ATTRIBUTE_TYPE_FLOAT |
                ATTRIBUTE_NOT_NORMALIZED |
                ATTRIBUTE_CARDINALITY_3D_VEC |
                ATTRIBUTE_ENCODING_SIGNED_32BIT_FLOAT
        );

        writePadding();

        int posSize = model.size * TileModel.FI_POSITION;
        for (int i = 0; i < posSize; i++) {
            writeFloat(position[i]);
        }
    }

    private void writeNormalArray(TileModel model) throws IOException {
        VectorM3f normal = new VectorM3f(0, 0, 0);
        float[] position = model.position;

        writeString("normal");
        out.write(
                ATTRIBUTE_TYPE_FLOAT |
                ATTRIBUTE_NORMALIZED |
                ATTRIBUTE_CARDINALITY_3D_VEC |
                ATTRIBUTE_ENCODING_SIGNED_8BIT_INT
        );

        writePadding();

        int pi, i, j;
        for (i = 0; i < model.size; i++) {
            pi = i * TileModel.FI_POSITION;
            calculateSurfaceNormal(
                    position[pi], position[pi + 1], position[pi + 2],
                    position[pi + 3], position[pi + 4], position[pi + 5],
                    position[pi + 6], position[pi + 7], position[pi + 8],
                    normal
            );

            for (j = 0; j < 3; j++) { // all 3 points
                writeNormalizedSignedByteValue(normal.x);
                writeNormalizedSignedByteValue(normal.y);
                writeNormalizedSignedByteValue(normal.z);
            }
        }
    }

    private void writeColorArray(TileModel model) throws IOException {
        float[] color = model.color;

        writeString("color");
        out.write(
                ATTRIBUTE_TYPE_FLOAT |
                ATTRIBUTE_NORMALIZED |
                ATTRIBUTE_CARDINALITY_3D_VEC |
                ATTRIBUTE_ENCODING_UNSIGNED_8BIT_INT
        );

        writePadding();

        int colorSize = model.size * TileModel.FI_COLOR, i, j;
        for (i = 0; i < colorSize; i += 3) {
            for (j = 0; j < 3; j++) {
                writeNormalizedUnsignedByteValue(color[i]);
                writeNormalizedUnsignedByteValue(color[i + 1]);
                writeNormalizedUnsignedByteValue(color[i + 2]);
            }
        }
    }

    private void writeUvArray(TileModel model) throws IOException {
        float[] uv = model.uv;

        writeString("uv");
        out.write(
                ATTRIBUTE_TYPE_FLOAT |
                ATTRIBUTE_NOT_NORMALIZED |
                ATTRIBUTE_CARDINALITY_2D_VEC |
                ATTRIBUTE_ENCODING_SIGNED_32BIT_FLOAT
        );

        writePadding();

        int uvSize = model.size * TileModel.FI_UV;
        for (int i = 0; i < uvSize; i++) {
            writeFloat(uv[i]);
        }
    }

    private void writeAoArray(TileModel model) throws IOException {
        float[] ao = model.ao;

        writeString("ao");
        out.write(
                ATTRIBUTE_TYPE_FLOAT |
                ATTRIBUTE_NORMALIZED |
                ATTRIBUTE_CARDINALITY_SCALAR |
                ATTRIBUTE_ENCODING_UNSIGNED_8BIT_INT
        );

        writePadding();

        int uvSize = model.size * TileModel.FI_AO;
        for (int i = 0; i < uvSize; i++) {
            writeNormalizedUnsignedByteValue(ao[i]);
        }
    }

    private void writeBlocklightArray(TileModel model) throws IOException {
        byte[] blocklight = model.blocklight;

        writeString("blocklight");
        out.write(
                ATTRIBUTE_TYPE_FLOAT |
                ATTRIBUTE_NOT_NORMALIZED |
                ATTRIBUTE_CARDINALITY_SCALAR |
                ATTRIBUTE_ENCODING_SIGNED_8BIT_INT
        );

        writePadding();

        int blSize = model.size * TileModel.FI_BLOCKLIGHT;
        for (int i = 0; i < blSize; i++) {
            out.write(blocklight[i]);
            out.write(blocklight[i]);
            out.write(blocklight[i]);
        }
    }

    private void writeSunlightArray(TileModel model) throws IOException {
        byte[] sunlight = model.sunlight;

        writeString("sunlight");
        out.write(
                ATTRIBUTE_TYPE_FLOAT |
                ATTRIBUTE_NOT_NORMALIZED |
                ATTRIBUTE_CARDINALITY_SCALAR |
                ATTRIBUTE_ENCODING_SIGNED_8BIT_INT
        );

        writePadding();

        int slSize = model.size * TileModel.FI_SUNLIGHT;
        for (int i = 0; i < slSize; i++) {
            out.write(sunlight[i]);
            out.write(sunlight[i]);
            out.write(sunlight[i]);
        }
    }

    private void writeMaterialGroups(TileModel model) throws IOException {

        writePadding();

        if (model.size > 0) {
            int[] materialIndex = model.materialIndex;

            int     miSize = model.size * TileModel.FI_MATERIAL_INDEX,
                    lastMaterial = materialIndex[0],
                    material = lastMaterial, groupStart = 0;

            write4byteValue(material);
            write4byteValue(0);

            for (int i = 1; i < miSize; i++) {
                material = materialIndex[i];

                if (material != lastMaterial) {
                    write4byteValue((i - groupStart) * 3);

                    groupStart = i;

                    write4byteValue(material);
                    write4byteValue(groupStart * 3);
                }

                lastMaterial = material;
            }

            write4byteValue((miSize - groupStart) * 3);

        }

        write4byteValue(-1);

    }

    private void writePadding() throws IOException {
        int paddingBytes = (int) (-out.getCount() & 0x3);
        for (int i = 0; i < paddingBytes; i++) {
            out.write(0);
        }
    }

    private void write2byteValue(int value) throws IOException {
        if (value > 0xFFFF) throw new IOException("Value too high: " + value);
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    private void write3byteValue(int value) throws IOException {
        if (value > 0xFFFFFF) throw new IOException("Value too high: " + value);
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
    }

    private void write4byteValue(int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private void writeFloat(float value) throws IOException {
        write4byteValue(Float.floatToIntBits(value));
    }

    private void writeNormalizedSignedByteValue(float value) throws IOException {
        byte normalized = (byte) (value * 0x80 - 0.5);
        out.write(normalized & 0xFF);
    }

    private void writeNormalizedUnsignedByteValue(float value) throws IOException {
        int normalized = (int) (value * 0xFF);
        out.write(normalized & 0xFF);
    }

    private void writeString(String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.US_ASCII));
        out.write(0);
    }

    private void calculateSurfaceNormal(
            float p1x, float p1y, float p1z,
            float p2x, float p2y, float p2z,
            float p3x, float p3y, float p3z,
            VectorM3f target
    ){
        p2x -= p1x; p2y -= p1y; p2z -= p1z;
        p3x -= p1x; p3y -= p1y; p3z -= p1z;

        p1x = p2y * p3z - p2z * p3y;
        p1y = p2z * p3x - p2x * p3z;
        p1z = p2x * p3y - p2y * p3x;

        float length = (float) Math.sqrt(p1x * p1x + p1y * p1y + p1z * p1z);
        p1x /= length;
        p1y /= length;
        p1z /= length;

        target.set(p1x, p1y, p1z);
    }

}
