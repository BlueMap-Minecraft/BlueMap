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
package de.bluecolored.bluemap.core.world.mca;

import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.block.entity.BlockEntity;
import de.bluecolored.bluemap.core.world.mca.data.BlockStateDeserializer;
import de.bluecolored.bluemap.core.world.mca.data.KeyDeserializer;
import de.bluecolored.bluenbt.BlueNBT;

public class MCAUtil {

    public static final BlueNBT BLUENBT = new BlueNBT();
    static {
        BLUENBT.register(TypeToken.get(BlockState.class), new BlockStateDeserializer());
        BLUENBT.register(TypeToken.get(Key.class), new KeyDeserializer());
        BLUENBT.register(TypeToken.get(BlockEntity.class), new BlockEntity.BlockEntityDeserializer());
    }

    /**
     * Having a long array where each long contains as many values as fit in it without overflowing, returning the "valueIndex"-th value when each value has "bitsPerValue" bits.
     */
    public static long getValueFromLongArray(long[] data, int valueIndex, int bitsPerValue) {
        int valuesPerLong = 64 / bitsPerValue;
        int longIndex = valueIndex / valuesPerLong;
        int bitIndex = (valueIndex % valuesPerLong) * bitsPerValue;

        if (longIndex >= data.length) return 0;
        long value = data[longIndex] >>> bitIndex;

        return value & (0xFFFFFFFFFFFFFFFFL >>> -bitsPerValue);
    }

    /**
     * Treating the long array "data" as a continuous stream of bits, returning the "valueIndex"-th value when each value has "bitsPerValue" bits.
     */
    @SuppressWarnings("ShiftOutOfRange")
    public static long getValueFromLongStream(long[] data, int valueIndex, int bitsPerValue) {
        int bitIndex = valueIndex * bitsPerValue;
        int firstLong = bitIndex >> 6; // index / 64
        int bitOffset = bitIndex & 0x3F; // Math.floorMod(index, 64)

        if (firstLong >= data.length) return 0;
        long value = data[firstLong] >>> bitOffset;

        if (bitOffset > 0 && firstLong + 1 < data.length) {
            long value2 = data[firstLong + 1];
            value2 = value2 << -bitOffset;
            value = value | value2;
        }

        return value & (0xFFFFFFFFFFFFFFFFL >>> -bitsPerValue);
    }

    /**
     * Extracts the 4 bits of the left (largeHalf = <code>true</code>) or the right (largeHalf = <code>false</code>) side of the byte stored in <code>value</code>.<br>
     * The value is treated as an unsigned byte.
     */
    public static int getByteHalf(int value, boolean largeHalf) {
        if (largeHalf) return value >> 4 & 0xF;
        return value & 0xF;
    }

    public static int ceilLog2(int n) {
        return Integer.SIZE - Integer.numberOfLeadingZeros(n - 1);
    }

}
