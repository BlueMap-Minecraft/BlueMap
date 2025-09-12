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

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.bluecolored.bluemap.core.resources.adapter.AbstractTypeAdapterFactory;
import de.bluecolored.bluemap.core.util.Key;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

@Getter
@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class PackMeta {

    private Pack pack = new Pack();
    private Overlays overlays = new Overlays();
    private Features features = new Features();

    @Getter
    public static class Pack {
        @JsonAdapter(PackVersion.MinAdapter.class) private PackVersion minFormat;
        @JsonAdapter(PackVersion.MaxAdapter.class) private PackVersion maxFormat;

        // <= 1.21.8
        private VersionRange packFormat = new VersionRange();
        private @Nullable VersionRange supportedFormats;

        public boolean includes(PackVersion version) {

            // <= 1.21.8
            if (minFormat == null || maxFormat == null) {
                if (supportedFormats != null && supportedFormats.includes(version.getMajor())) return true;
                return packFormat.includes(version.getMajor());
            }

            return version.isGreaterOrEqual(minFormat) && version.isSmallerOrEqual(maxFormat);
        }
    }

    @Getter
    public static class Overlays {
        private Overlay[] entries = new Overlay[0];
    }

    @Getter
    public static class Overlay {
        @JsonAdapter(PackVersion.MinAdapter.class) private PackVersion minFormat;
        @JsonAdapter(PackVersion.MaxAdapter.class) private PackVersion maxFormat;
        private @Nullable String directory;

        // <= 1.21.8
        private VersionRange formats = new VersionRange();

        public boolean includes(PackVersion version) {

            // <= 1.21.8
            if (minFormat == null || maxFormat == null) {
                return formats.includes(version.getMajor());
            }

            return version.isGreaterOrEqual(minFormat) && version.isSmallerOrEqual(maxFormat);
        }

    }

    @Getter
    public static class Features {
        private Collection<Key> enabled = Set.of();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonAdapter(VersionRange.Adapter.class)
    public static class VersionRange {
        private int minInclusive = Integer.MIN_VALUE;
        private int maxInclusive = Integer.MAX_VALUE;

        public boolean includes(int version) {
            return version >= minInclusive && version <= maxInclusive;
        }

        private static class Adapter extends AbstractTypeAdapterFactory<VersionRange> {

            public Adapter() {
                super(VersionRange.class);
            }

            @Override
            public VersionRange read(JsonReader in, Gson gson) throws IOException {
                return switch (in.peek()) {
                    case NUMBER -> {
                        int version = in.nextInt();
                        yield new VersionRange(version, version);
                    }
                    case BEGIN_ARRAY -> {
                        in.beginArray();
                        VersionRange range = new VersionRange(
                                in.nextInt(),
                                in.nextInt()
                        );

                        while (in.peek() != JsonToken.END_ARRAY)
                            in.skipValue();
                        in.endArray();

                        yield range;
                    }
                    default -> gson
                            .getDelegateAdapter(this, TypeToken.get(VersionRange.class))
                            .read(in);
                };
            }

        }
    }

}
