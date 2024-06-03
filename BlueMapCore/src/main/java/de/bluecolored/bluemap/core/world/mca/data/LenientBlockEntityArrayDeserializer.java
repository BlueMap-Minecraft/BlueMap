package de.bluecolored.bluemap.core.world.mca.data;

import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluemap.core.world.block.entity.BlockEntity;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TagType;
import de.bluecolored.bluenbt.TypeDeserializer;

import java.io.IOException;

/**
 * TypeSerializer that returns a default value instead of failing when the serialized field is of the wrong type
 */
public class LenientBlockEntityArrayDeserializer implements TypeDeserializer<BlockEntity[]> {

    private static final BlockEntity[] EMPTY_BLOCK_ENTITIES_ARRAY = new BlockEntity[0];

    private final TypeDeserializer<BlockEntity[]> delegate;

    public LenientBlockEntityArrayDeserializer(BlueNBT blueNBT) {
        delegate = blueNBT.getTypeDeserializer(new TypeToken<>(){});
    }

    @Override
    public BlockEntity[] read(NBTReader reader) throws IOException {
        if (reader.peek() != TagType.LIST) {
            reader.skip();
            return EMPTY_BLOCK_ENTITIES_ARRAY;
        }
        return delegate.read(reader);
    }

}
