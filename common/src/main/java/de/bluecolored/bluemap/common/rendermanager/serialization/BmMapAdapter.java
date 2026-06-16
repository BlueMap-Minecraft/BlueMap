package de.bluecolored.bluemap.common.rendermanager.serialization;

import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.NBTWriter;
import de.bluecolored.bluenbt.TagType;
import de.bluecolored.bluenbt.TypeAdapter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class BmMapAdapter implements TypeAdapter<BmMap> {

    private final BlueMapService blueMap;

    @Override
    public BmMap read(NBTReader reader) throws IOException {
        BmMap map = blueMap.getMaps().get(reader.nextString());
        if (map == null) throw new IOException("No map with id '" + reader.nextString() + "' loaded.");
        return map;
    }

    @Override
    public void write(BmMap value, NBTWriter writer) throws IOException {
        writer.value(value.getId());
    }

    @Override
    public TagType type() {
        return TagType.STRING;
    }

}
