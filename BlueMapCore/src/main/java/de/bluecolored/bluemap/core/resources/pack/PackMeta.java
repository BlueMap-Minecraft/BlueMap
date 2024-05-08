package de.bluecolored.bluemap.core.resources.pack;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import de.bluecolored.bluemap.core.resources.AbstractTypeAdapterFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@Getter
@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class PackMeta {

    private Pack pack = new Pack();
    private Overlays overlays = new Overlays();

    @Getter
    public static class Pack {
        private VersionRange packFormat = new VersionRange();
        private @Nullable VersionRange supportedFormats;
    }

    @Getter
    public static class Overlays {
        private Overlay[] entries = new Overlay[0];
    }

    @Getter
    public static class Overlay {
        private VersionRange formats = new VersionRange();
        private @Nullable String directory;
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
