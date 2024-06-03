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
package de.bluecolored.bluemap.core.util;

import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.NBTWriter;
import de.bluecolored.bluenbt.TypeAdapter;
import de.bluecolored.bluenbt.adapter.ArrayAdapterFactory;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;

@RequiredArgsConstructor
public class PalettedArrayAdapter<T> implements TypeAdapter<T[]> {

    private final Class<T> type;
    private final TypeAdapter<T[]> paletteAdapter;

    @SuppressWarnings("unchecked")
    public PalettedArrayAdapter(BlueNBT blueNBT, Class<T> type) {
        this.type = type;
        this.paletteAdapter = ArrayAdapterFactory.INSTANCE.create((TypeToken<T[]>) TypeToken.getArray(type), blueNBT).orElseThrow();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T[] read(NBTReader reader) throws IOException {
        reader.beginCompound();
        T[] palette = null;
        byte[] data = null;
        while (reader.hasNext()) {
            String name = reader.name();
            switch (name) {
                case "palette" -> palette = paletteAdapter.read(reader);
                case "data" -> data = reader.nextArrayAsByteArray();
                default -> reader.skip();
            }
        }
        reader.endCompound();

        if (palette == null || palette.length == 0) throw new IOException("Missing or empty palette");
        if (data == null) return (T[]) Array.newInstance(type, 0);
        T[] result = (T[]) Array.newInstance(type, data.length);
        for (int i = 0; i < data.length; i++) {
            byte index = data[i];
            if (index >= palette.length) throw new IOException("Palette (size: " + palette.length + ") does not contain entry-index (" + index + ")");
            result[i] = palette[data[i]];
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(T[] value, NBTWriter writer) throws IOException {
        HashMap<T, Byte> paletteMap = new HashMap<>();
        byte[] data = new byte[value.length];
        for (int i = 0; i < value.length; i++) {
            byte index = paletteMap.computeIfAbsent(value[i], v -> (byte) paletteMap.size());
            data[i] = index;
        }

        T[] palette = (T[]) Array.newInstance(type, paletteMap.size());
        paletteMap.forEach((k, v) -> palette[v] = k);

        writer.beginCompound();
        writer.name("palette");
        paletteAdapter.write(palette, writer);
        writer.name("data").value(data);
        writer.endCompound();
    }

}
