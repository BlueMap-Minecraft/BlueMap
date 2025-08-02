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
package de.bluecolored.bluemap.core.map.mask;

import de.bluecolored.bluemap.core.util.Tristate;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BlurMask implements Mask {

    private final CombinedMask masks;
    private final int size;

    @Override
    public boolean test(int x, int y, int z) {
        return masks.test(
                x + randomOffset(x, y, z, 23948),
                y + randomOffset(x, y, z, 53242),
                z + randomOffset(x, y, z, 75654)
        );
    }

    @Override
    public Tristate test(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return masks.test(
                minX - size, minY - size, minZ - size,
                maxX + size, maxY + size, maxZ + size
        );
    }

    @Override
    public boolean isEdge(int minX, int minZ, int maxX, int maxZ) {
        return masks.isEdge(
                minX - size, minZ - size,
                maxX + size, maxZ + size
        );
    }

    private int randomOffset(int x, int y, int z, long seed) {
        final long hash = x * 73428767L ^ y * 4382893L ^ z * 2937119L ^ seed * 457;
        return (int)((((hash * (hash + 456149) & 0x00ffffff) / (float) 0x01000000) - 0.5f) * 2 * size);
    }

}
