package de.bluecolored.bluemap.core.resources.adapter;

import com.flowpowered.math.vector.Vector3f;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class Vector3fAdapter extends TypeAdapter<Vector3f> {

    @Override
    public void write(JsonWriter out, Vector3f value) throws IOException {
        out.beginArray();
        out.value(value.getX());
        out.value(value.getY());
        out.value(value.getZ());
        out.endArray();
    }

    @Override
    public Vector3f read(JsonReader in) throws IOException {
        in.beginArray();
        Vector3f value = new Vector3f(
                in.nextDouble(),
                in.nextDouble(),
                in.nextDouble()
        );
        in.endArray();
        return value;
    }

}
