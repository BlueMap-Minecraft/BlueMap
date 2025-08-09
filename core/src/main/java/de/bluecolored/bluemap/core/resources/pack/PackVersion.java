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
package de.bluecolored.bluemap.core.resources.pack;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;

@AllArgsConstructor
@Getter
@JsonAdapter(PackVersion.MinAdapter.class)
@ToString
public class PackVersion {
    private int major, minor;

    public boolean isGreaterOrEqual(PackVersion other) {
        if (other.major == this.major) return other.minor >= this.minor;
        return other.major > this.major;
    }

    public boolean isSmallerOrEqual(PackVersion other) {
        if (other.major == this.major) return other.minor <= this.minor;
        return other.major < this.major;
    }

    public static class Adapter extends TypeAdapter<PackVersion> {

        private final int defaultMinor;

        public Adapter(int defaultMinor) {
            this.defaultMinor = defaultMinor;
        }

        @Override
        public void write(JsonWriter out, PackVersion value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PackVersion read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NUMBER) return new PackVersion(in.nextInt(), defaultMinor);
            in.beginArray();
            int major = in.nextInt();
            int minor = in.hasNext() ? in.nextInt() : defaultMinor;
            in.endArray();
            return new PackVersion(major, minor);
        }

    }

    public static class MinAdapter extends Adapter {
        public MinAdapter() { super(0); }
    }

    public static class MaxAdapter extends Adapter {
        public MaxAdapter() { super(Integer.MAX_VALUE); }
    }

}
