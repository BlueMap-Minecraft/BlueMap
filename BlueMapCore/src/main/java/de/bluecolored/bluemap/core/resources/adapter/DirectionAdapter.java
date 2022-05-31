package de.bluecolored.bluemap.core.resources.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.core.util.Direction;

import java.io.IOException;
import java.util.Locale;

public class DirectionAdapter extends TypeAdapter<Direction>  {

    @Override
    public void write(JsonWriter out, Direction value) throws IOException {
        out.value(value.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public Direction read(JsonReader in) throws IOException {
        String name = in.nextString();
        if (name.equalsIgnoreCase("bottom")) return Direction.DOWN;
        if (name.equalsIgnoreCase("top")) return Direction.UP;
        return Direction.fromString(name);
    }

}
