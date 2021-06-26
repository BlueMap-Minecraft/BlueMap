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

public class HiresTileModel extends TileModel {

    // attributes         per-vertex * per-face
    private static final int
            FI_UV =                2 * 3,
            FI_AO =                    3,
            FI_COLOR =             3    ,
            FI_SUNLIGHT =       1       ,
            FI_BLOCKLIGHT =     1       ,
            FI_MATERIAL_INDEX = 1       ;

    private float[] color, uv, ao;
    private byte[] sunlight, blocklight;
    private int[] materialIndex;

    public HiresTileModel(int initialCapacity) {
        super(initialCapacity);
    }

    public void setUvs(
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
    }

    public void setAOs(
            int face,
            float ao1, float ao2, float ao3
    ) {
        int index = face * FI_AO;

        ao[index    ] = ao1;
        ao[index + 1] = ao2;
        ao[index + 2] = ao3;
    }

    public void setColor(
            int face,
            float r, float g, float b
    ){
        int index = face * FI_COLOR;

        color[index    ] = r;
        color[index + 1] = g;
        color[index + 2] = b;
    }

    public void setSunlight(int face, int sl) {
        sunlight[face * FI_SUNLIGHT] = (byte) sl;
    }

    public void setBlocklight(int face, int bl) {
        blocklight[face * FI_BLOCKLIGHT] = (byte) bl;
    }

    public void setMaterialIndex(int face, int m) {
        materialIndex[face * FI_MATERIAL_INDEX] = m;
    }

    protected void grow(int count) {
        float[] _color = color, _uv = uv, _ao = ao;
        byte[] _sunlight = sunlight, _blocklight = blocklight;
        int[] _materialIndex = materialIndex;

        super.grow(count);

        int size = size();
        System.arraycopy(_uv,               0, uv,              0, size * FI_UV);
        System.arraycopy(_ao,               0, ao,              0, size * FI_AO);

        System.arraycopy(_color,            0, color,           0, size * FI_COLOR);
        System.arraycopy(_sunlight,         0, sunlight,        0, size * FI_SUNLIGHT);
        System.arraycopy(_blocklight,       0, blocklight,      0, size * FI_BLOCKLIGHT);
        System.arraycopy(_materialIndex,    0, materialIndex,   0, size * FI_MATERIAL_INDEX);
    }

    protected void setCapacity(int capacity) {
        super.setCapacity(capacity);

        // attributes                capacity * per-vertex * per-face
        uv =            new float   [capacity * FI_UV];
        ao =            new float   [capacity * FI_AO];

        color =         new float   [capacity * FI_COLOR];
        sunlight =      new byte    [capacity * FI_SUNLIGHT];
        blocklight =    new byte    [capacity * FI_BLOCKLIGHT];
        materialIndex = new int     [capacity * FI_MATERIAL_INDEX];
    }

}
