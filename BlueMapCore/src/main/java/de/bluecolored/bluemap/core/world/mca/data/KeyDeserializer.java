package de.bluecolored.bluemap.core.world.mca.data;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TypeDeserializer;

import java.io.IOException;

public class KeyDeserializer implements TypeDeserializer<Key> {

    @Override
    public Key read(NBTReader reader) throws IOException {
        return new Key(reader.nextString());
    }

}