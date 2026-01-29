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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        private static final Pattern VERSION_STRING_PATTERN = Pattern.compile("^(\\d+)(?:\\.(\\d+))?$");

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
            return switch (in.peek()) {
                case STRING -> parseString(in.nextString());
                case NUMBER -> {
                    double version = in.nextDouble();
                    if (version == Math.floor(version)) yield new PackVersion((int) version, defaultMinor);
                    yield parseString("%.9f".formatted(version));
                }
                case BEGIN_ARRAY -> {
                    in.beginArray();
                    int major = in.nextInt();
                    int minor = in.hasNext() ? in.nextInt() : defaultMinor;
                    in.endArray();
                    yield new PackVersion(major, minor);
                }
                default -> throw new IOException("Invalid version format: '%s'!".formatted(in.peek()));
            };
        }

        private PackVersion parseString(String versionString) throws IOException {
            Matcher versionStringMatcher = VERSION_STRING_PATTERN.matcher(versionString);
            if (!versionStringMatcher.matches())
                throw new IOException("Invalid version string: '%s'!".formatted(versionString));

            String major = versionStringMatcher.group(1);
            String minor = versionStringMatcher.group(2);
            return new PackVersion(Integer.parseInt(major), minor != null && !minor.isEmpty() ? Integer.parseInt(minor) : defaultMinor);
        }

    }

    public static class MinAdapter extends Adapter {
        public MinAdapter() { super(0); }
    }

    public static class MaxAdapter extends Adapter {
        public MaxAdapter() { super(Integer.MAX_VALUE); }
    }

}
