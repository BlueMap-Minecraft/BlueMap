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
package de.bluecolored.bluemap.common.rendermanager.serialization;

import de.bluecolored.bluemap.common.rendermanager.*;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import de.bluecolored.bluenbt.*;
import lombok.Getter;

import java.io.IOException;

public class RenderTaskAdapter implements TypeAdapter<RenderTask> {

    private SerializableRenderTaskAdapter<MapPurgeTask, ?> mapPurgeTaskAdapter;
    private SerializableRenderTaskAdapter<MapSaveTask, ?> mapSaveTaskAdapter;
    private SerializableRenderTaskAdapter<MapUpdateTask, ?> mapUpdateTaskAdapter;
    private SerializableRenderTaskAdapter<WorldRegionUpdateTask, ?> worldRegionRenderTaskAdapter;

    private Registry<SerializableRenderTaskAdapter<?, ?>> taskAdaperRegistry;

    public RenderTaskAdapter() {}

    public void init(BlueNBT blueNBT) {
        this.taskAdaperRegistry = new Registry<>(
                this.mapPurgeTaskAdapter = new SerializableRenderTaskAdapter<>(Key.bluemap("map-purge"), blueNBT, MapPurgeTask.Serialized.class),
                this.mapSaveTaskAdapter = new SerializableRenderTaskAdapter<>(Key.bluemap("map-save"), blueNBT, MapSaveTask.Serialized.class),
                this.mapUpdateTaskAdapter = new SerializableRenderTaskAdapter<>(Key.bluemap("map-update"), blueNBT, MapUpdateTask.Serialized.class),
                this.worldRegionRenderTaskAdapter = new SerializableRenderTaskAdapter<>(Key.bluemap("region-update"), blueNBT, WorldRegionUpdateTask.Serialized.class)
        );
    }

    @Override
    public void write(RenderTask value, NBTWriter writer) throws IOException {
        switch (value) {
            case MapPurgeTask task -> writeWith(task, mapPurgeTaskAdapter, writer);
            case MapSaveTask task -> writeWith(task, mapSaveTaskAdapter, writer);
            case MapUpdateTask task -> writeWith(task, mapUpdateTaskAdapter, writer);
            case WorldRegionUpdateTask task -> writeWith(task, worldRegionRenderTaskAdapter, writer);
            default -> {} // write nothing, task is ignored
        }
    }

    private <T extends SerializableRenderTask<T, ?>> void writeWith(T value, SerializableRenderTaskAdapter<T, ?> adapter, NBTWriter writer) throws IOException {
        writer.beginCompound();
        writer.name("type").value(adapter.getKey().getFormatted());
        writer.name("data");
        adapter.write(value, writer);
        writer.endCompound();
    }

    @Override
    public RenderTask read(NBTReader reader) throws IOException {
        Key type = null;
        byte[] data = null;

        reader.beginCompound();
        while (reader.hasNext()) {
            switch (reader.name()) {
                case "type" -> type = Key.parse(reader.nextString(), Key.BLUEMAP_NAMESPACE);
                case "data" -> data = reader.raw();
                default -> reader.skip();
            }
        }
        reader.endCompound();

        if (type == null) throw new IOException("Missing type");
        if (data == null) throw new IOException("Missing data");

        SerializableRenderTaskAdapter<?, ?> adapter = taskAdaperRegistry.get(type);
        if (adapter == null) throw new IOException("Unknown render-task type: " + type);

        return adapter.read(new NBTReader(data));
    }

    @Getter
    static class SerializableRenderTaskAdapter<T extends SerializableRenderTask<T, D>, D extends SerializableRenderTask.Serialized<T>> implements TypeAdapter<T>, Keyed {

        private final Key key;
        private final TypeDeserializer<D> deserializer;
        private final TypeSerializer<D> serializer;

        public SerializableRenderTaskAdapter(Key key, BlueNBT blueNBT, Class<D> serializedType) {
            TypeToken<D> typeToken = TypeToken.of(serializedType);

            this.key = key;
            this.deserializer = blueNBT.getTypeDeserializer(typeToken);
            this.serializer = blueNBT.getTypeSerializer(typeToken);
        }

        @Override
        public void write(T value, NBTWriter writer) throws IOException {
            serializer.write(value.serialize(), writer);
        }

        @Override
        public T read(NBTReader reader) throws IOException {
            return deserializer.read(reader).deserialize();
        }
    }

}
