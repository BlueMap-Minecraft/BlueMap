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
