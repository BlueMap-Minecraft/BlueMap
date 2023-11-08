package de.bluecolored.bluemap.core.world.mca.data;

import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TypeDeserializer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class BlockStateDeserializer implements TypeDeserializer<BlockState> {

    @Override
    public BlockState read(NBTReader reader) throws IOException {
        reader.beginCompound();

        String id = null;
        Map<String, String> properties = null;

        while (reader.hasNext()) {
            switch (reader.name()) {
                case "Name" : id = reader.nextString(); break;
                case "Properties" :
                    properties = new LinkedHashMap<>();
                    reader.beginCompound();
                    while (reader.hasNext())
                        properties.put(reader.name(), reader.nextString());
                    reader.endCompound();
                    break;
                default : reader.skip();
            }
        }

        reader.endCompound();

        if (id == null) throw new IOException("Invalid BlockState, Name is missing!");
        return properties == null ? new BlockState(id) : new BlockState(id, properties);
    }

}