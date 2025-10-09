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
package de.bluecolored.bluemap.core.resources.adapter;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public abstract class AbstractTypeAdapterFactory<T> implements TypeAdapterFactory {
    protected static final String JSON_COMMENT = "__comment";

    private final Class<T> type;

    public AbstractTypeAdapterFactory(Class<T> type) {
        this.type = type;
    }

    public void write(JsonWriter out, T value, Gson gson) throws IOException {
        throw new UnsupportedOperationException();
    }

    public abstract T read(JsonReader in, Gson gson) throws IOException;

    @SuppressWarnings("unchecked")
    @Override
    public <U> TypeAdapter<U> create(Gson gson, TypeToken<U> type) {
        if (!type.getRawType().isAssignableFrom(this.type)) return null;
        return (TypeAdapter<U>) new Adapter(gson);
    }

    public class Adapter extends TypeAdapter<T> {

        private final Gson gson;

        public Adapter(Gson gson) {
            this.gson = gson;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            AbstractTypeAdapterFactory.this.write(out, value, gson);
        }

        @Override
        public T read(JsonReader in) throws IOException {
            return AbstractTypeAdapterFactory.this.read(in, gson);
        }

    }

}
