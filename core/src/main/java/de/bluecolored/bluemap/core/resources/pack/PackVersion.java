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
