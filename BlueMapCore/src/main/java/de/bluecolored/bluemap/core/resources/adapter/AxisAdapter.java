package de.bluecolored.bluemap.core.resources.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.core.util.math.Axis;

import java.io.IOException;
import java.util.Locale;

public class AxisAdapter extends TypeAdapter<Axis>  {

    @Override
    public void write(JsonWriter out, Axis value) throws IOException {
        out.value(value.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public Axis read(JsonReader in) throws IOException {
        return Axis.fromString(in.nextString());
    }

}
