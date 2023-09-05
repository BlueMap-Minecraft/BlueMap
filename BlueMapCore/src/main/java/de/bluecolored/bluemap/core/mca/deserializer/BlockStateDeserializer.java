package de.bluecolored.bluemap.core.mca.deserializer;

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
            String name = reader.name();
            if (name.equals("Name")){
                id = reader.nextString();
            } else if (name.equals("Properties")) {
                properties = new LinkedHashMap<>();
                reader.beginCompound();
                while (reader.hasNext())
                    properties.put(reader.name(), reader.nextString());
                reader.endCompound();
            } else {
                reader.skip();
            }
        }

        reader.endCompound();

        if (id == null) throw new IOException("Invalid BlockState, Name is missing!");

        if (properties == null)
            return new BlockState(id);
        return new BlockState(id, properties);
    }

}
