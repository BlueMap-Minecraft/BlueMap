package de.bluecolored.bluemap.common.rendermanager.serialization;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.NBTWriter;
import de.bluecolored.bluenbt.TagType;
import de.bluecolored.bluenbt.TypeAdapter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

public class Vector2iAdapter implements TypeAdapter<Vector2i> {

    @Override
    public Vector2i read(NBTReader reader) throws IOException {
        reader.beginList();
        int x = reader.nextInt();
        int y = reader.nextInt();
        reader.endList();
        return new Vector2i(x, y);
    }

    @Override
    public void write(Vector2i value, NBTWriter writer) throws IOException {
        writer.beginList(2);
        writer.value(value.getX());
        writer.value(value.getY());
        writer.endList();
    }

    @Override
    public TagType type() {
        return TagType.LIST;
    }

}
