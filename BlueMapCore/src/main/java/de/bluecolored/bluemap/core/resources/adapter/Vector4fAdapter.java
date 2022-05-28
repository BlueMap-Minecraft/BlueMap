package de.bluecolored.bluemap.core.resources.adapter;

import com.flowpowered.math.vector.Vector4f;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class Vector4fAdapter extends TypeAdapter<Vector4f> {

    @Override
    public void write(JsonWriter out, Vector4f value) throws IOException {
        out.beginArray();
        out.value(value.getX());
        out.value(value.getY());
        out.value(value.getZ());
        out.value(value.getW());
        out.endArray();
    }

    @Override
    public Vector4f read(JsonReader in) throws IOException {
        in.beginArray();
        Vector4f value = new Vector4f(
                in.nextDouble(),
                in.nextDouble(),
                in.nextDouble(),
                in.nextDouble()
        );
        in.endArray();
        return value;
    }

}
