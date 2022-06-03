package de.bluecolored.bluemap.core.resources.adapter;

import com.flowpowered.math.vector.Vector2i;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class Vector2iAdapter extends TypeAdapter<Vector2i> {

    @Override
    public void write(JsonWriter out, Vector2i value) throws IOException {
        out.beginArray();
        out.value(value.getX());
        out.value(value.getY());
        out.endArray();
    }

    @Override
    public Vector2i read(JsonReader in) throws IOException {
        in.beginArray();
        Vector2i value = new Vector2i(
                in.nextDouble(),
                in.nextDouble()
        );
        in.endArray();
        return value;
    }

}
