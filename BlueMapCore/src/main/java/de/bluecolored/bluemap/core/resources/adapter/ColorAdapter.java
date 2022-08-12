package de.bluecolored.bluemap.core.resources.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.core.util.ConfigUtils;
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
            case BEGIN_ARRAY:
                in.beginArray();
                value.set(
                        (float) in.nextDouble(),
                        (float) in.nextDouble(),
                        (float) in.nextDouble(),
                        in.hasNext() ? (float) in.nextDouble() : 1f,
                        false
                );
                in.endArray();
                break;
            case BEGIN_OBJECT:
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
                break;
            case STRING:
                value.set(ConfigUtils.parseColorFromString(in.nextString()));
                break;
            case NUMBER:
                int color = in.nextInt();
                if ((color & 0xFF000000) == 0) color = color | 0xFF000000; // assume full alpha if not specified
                value.set(color);
                break;
            case NULL:
                break;
            default: throw new IOException("Unexpected token while parsing Color:" + token);
        }
        return value;
    }

}
