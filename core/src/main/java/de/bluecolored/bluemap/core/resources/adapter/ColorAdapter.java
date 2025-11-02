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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.core.util.math.Color;

import java.io.IOException;

public class ColorAdapter extends TypeAdapter<Color> {

    @Override
    public void write(JsonWriter out, Color value) throws IOException {
        value.straight();
        out.beginArray();
        out.value(value.r);
        out.value(value.g);
        out.value(value.b);
        out.value(value.a);
        out.endArray();
    }

    @Override
    public Color read(JsonReader in) throws IOException {
        Color value = new Color();
        JsonToken token = in.peek();
        switch (token) {
            case BEGIN_ARRAY -> {
                in.beginArray();
                value.set(
                        (float) in.nextDouble(),
                        (float) in.nextDouble(),
                        (float) in.nextDouble(),
                        in.hasNext() ? (float) in.nextDouble() : 1f,
                        false
                );
                in.endArray();
            }
            case BEGIN_OBJECT -> {
                value.a = 1f;
                in.beginObject();
                while (in.hasNext()) {
                    String n = in.nextName();
                    float v = (float) in.nextDouble();

                    switch (n) {
                        case "r": value.r = v; break;
                        case "g": value.g = v; break;
                        case "b": value.b = v; break;
                        case "a": value.a = v; break;
                    }
                }
                in.endObject();
            }
            case STRING -> value.parse(in.nextString());
            case NUMBER -> {
                int color = in.nextInt();
                if ((color & 0xFF000000) == 0) color = color | 0xFF000000; // assume full alpha if not specified
                value.set(color);
            }
            case NULL -> {}
            default -> throw new IOException("Unexpected token while parsing Color:" + token);
        }
        return value;
    }

}
