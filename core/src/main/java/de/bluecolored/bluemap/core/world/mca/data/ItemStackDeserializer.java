package de.bluecolored.bluemap.core.world.mca.data;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TypeDeserializer;

import java.io.IOException;

public class ItemStackDeserializer implements TypeDeserializer<ItemStackDeserializer.ItemStack> {

    @Override
    public ItemStack read(NBTReader nbtReader) throws IOException {
        nbtReader.beginCompound();

        String id = null;
        int count = 1;

        while (nbtReader.hasNext()) {
            switch (nbtReader.name()) {
                case "id" : id = nbtReader.nextString(); break;
                case "count" : count = nbtReader.nextInt(); break;

                default : nbtReader.skip();
            }
        }

        nbtReader.endCompound();

        if (id == null) throw new IOException("Invalid ItemStack, ID is missing!");

        Key key = Key.parse(id);
        return new ItemStack(key, count);
    }

    public record ItemStack(Key id, int count) {}
}
