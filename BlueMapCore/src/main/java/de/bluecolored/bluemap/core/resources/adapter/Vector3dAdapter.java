package de.bluecolored.bluemap.core.resources.adapter;

import com.flowpowered.math.vector.Vector3d;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class Vector3dAdapter extends TypeAdapter<Vector3d> {

    @Override
    public void write(JsonWriter out, Vector3d value) throws IOException {
        out.beginArray();
        out.value(value.getX());
        out.value(value.getY());
        out.value(value.getZ());
        out.endArray();
    }

    @Override
    public Vector3d read(JsonReader in) throws IOException {
        in.beginArray();
        Vector3d value = new Vector3d(
                in.nextDouble(),
                in.nextDouble(),
                in.nextDouble()
        );
        in.endArray();
        return value;
    }

}
