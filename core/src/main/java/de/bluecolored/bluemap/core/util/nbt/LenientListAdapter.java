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
package de.bluecolored.bluemap.core.util.nbt;

import de.bluecolored.bluenbt.*;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class LenientListAdapter<E> implements TypeAdapter<List<E>> {

    private final TypeSerializer<E> entrySerializer;
    private final TypeDeserializer<E> entryDeserializer;
    private final InstanceCreator<? extends List<E>> constructor;
    private final Consumer<IOException> errorHandler;

    public LenientListAdapter(BlueNBT nbt, TypeToken<E> entryType, @Nullable Consumer<IOException> errorHandler) {
        this.entrySerializer = nbt.getTypeSerializer(entryType);
        this.entryDeserializer = nbt.getTypeDeserializer(entryType);
        this.constructor = nbt.getInstanceCreator(new TypeToken<>() {});
        this.errorHandler = errorHandler != null ? errorHandler : _ -> {};
    }

    @Override
    public List<E> read(NBTReader reader) throws IOException {
        List<E> list = constructor.create();
        reader.beginList();
        while (reader.hasNext()) {
            // to achieve error-recovery we need to fully consume the data first, and then parse it separately
            // otherwise the reader might be in an invalid state
            byte[] data = reader.raw();
            try {
                E instance = entryDeserializer.read(new NBTReader(data));
                list.add(instance);
            } catch (IOException e) {
                this.errorHandler.accept(e);
            }
        }
        reader.endList();
        return list;
    }

    @Override
    public void write(List<E> value, NBTWriter writer) throws IOException {
        int size = value.size();
        if (size == 0) {
            writer.beginList(size, entrySerializer.type());
            writer.endList();
        } else {
            writer.beginList(size);
            for (E element : value) {
                if (element == null) continue;
                entrySerializer.write(
                        element,
                        writer
                );
            }
            writer.endList();
        }
    }

    @Override
    public TagType type() {
        return TagType.LIST;
    }

}
