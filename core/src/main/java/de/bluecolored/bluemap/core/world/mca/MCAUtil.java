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

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockEntity;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.Entity;
import de.bluecolored.bluemap.core.world.mca.blockentity.SignBlockEntity;
import de.bluecolored.bluemap.core.world.mca.data.*;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.NamingStrategy;
import de.bluecolored.bluenbt.TypeToken;
import org.jetbrains.annotations.Contract;

import java.util.UUID;

public class MCAUtil {

    public static final BlueNBT BLUENBT = addCommonNbtSettings(new BlueNBT());

    @Contract(value = "_ -> param1", mutates = "param1")
    public static BlueNBT addCommonNbtSettings(BlueNBT nbt) {

        nbt.setNamingStrategy(NamingStrategy.lowerCaseWithDelimiter("_"));

        nbt.register(TypeToken.of(BlockState.class), new BlockStateDeserializer());
        nbt.register(TypeToken.of(Key.class), new KeyDeserializer());
        nbt.register(TypeToken.of(UUID.class), new UUIDDeserializer());
        nbt.register(TypeToken.of(Vector3d.class), new Vector3dDeserializer());
        nbt.register(TypeToken.of(Vector2i.class), new Vector2iDeserializer());
        nbt.register(TypeToken.of(Vector2f.class), new Vector2fDeserializer());

        nbt.register(TypeToken.of(BlockEntity.class), new BlockEntityTypeResolver());
        nbt.register(TypeToken.of(SignBlockEntity.class), new SignBlockEntityTypeResolver());
        nbt.register(TypeToken.of(Entity.class), new EntityTypeResolver());

        return nbt;
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
