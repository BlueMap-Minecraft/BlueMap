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
package de.bluecolored.bluemap.core.resources;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Function;

@DebugDump
@JsonAdapter(ResourcePath.Adapter.class)
public class ResourcePath<T> extends Key {

    private T resource = null;

    public ResourcePath(String formatted) {
        super(formatted.toLowerCase(Locale.ROOT));
    }

    public ResourcePath(String namespace, String value) {
        super(namespace.toLowerCase(Locale.ROOT), value.toLowerCase(Locale.ROOT));
    }

    public ResourcePath(Path filePath) {
        super(parsePath(filePath).toLowerCase(Locale.ROOT));
    }

    @Nullable
    public T getResource() {
        return resource;
    }

    @Nullable
    public T getResource(Function<ResourcePath<T>, T> supplier) {
        if (resource == null) resource = supplier.apply(this);
        return resource;
    }

    public void setResource(T resource) {
        this.resource = resource;
    }

    private static String parsePath(Path filePath) {
        if (filePath.getNameCount() < 4)
            throw new IllegalArgumentException("The provided filePath has less than 4 segments!");

        if (!filePath.getName(0).toString().equalsIgnoreCase("assets"))
            throw new IllegalArgumentException("The provided filePath doesn't start with 'assets'!");

        String namespace = filePath.getName(1).toString();
        String path = filePath.subpath(3, filePath.getNameCount()).toString().replace(filePath.getFileSystem().getSeparator(), "/");

        // remove file-ending
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex != -1) path = path.substring(0, dotIndex);

        return namespace + ":" + path;
    }

    static class Adapter implements TypeAdapterFactory {

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (!type.getRawType().isAssignableFrom(ResourcePath.class))
                return null;

            return new TypeAdapter<>() {
                @Override
                public void write(JsonWriter out, T value) throws IOException {
                    out.value(((ResourcePath<?>) value).getFormatted());
                }

                @SuppressWarnings("unchecked")
                @Override
                public T read(JsonReader in) throws IOException {
                    return (T) new ResourcePath<>(in.nextString());
                }
            };
        }

    }
}
