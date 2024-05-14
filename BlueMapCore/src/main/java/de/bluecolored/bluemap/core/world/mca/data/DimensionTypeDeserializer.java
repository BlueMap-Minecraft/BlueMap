package de.bluecolored.bluemap.core.world.mca.data;

import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.pack.datapack.DataPack;
import de.bluecolored.bluemap.core.resources.pack.datapack.dimension.DimensionTypeData;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.DimensionType;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TagType;
import de.bluecolored.bluenbt.TypeDeserializer;

import java.io.IOException;

public class DimensionTypeDeserializer implements TypeDeserializer<DimensionType> {

    private final TypeDeserializer<DimensionTypeData> defaultTypeDeserializer;
    private final DataPack dataPack;

    public DimensionTypeDeserializer(BlueNBT blueNBT, DataPack dataPack) {
        this.defaultTypeDeserializer = blueNBT.getTypeDeserializer(TypeToken.get(DimensionTypeData.class));
        this.dataPack = dataPack;
    }

    @Override
    public DimensionType read(NBTReader reader) throws IOException {

        // try load directly
        if (reader.peek() == TagType.COMPOUND)
            return defaultTypeDeserializer.read(reader);

        // load from datapack
        Key key = Key.parse(reader.nextString(), Key.MINECRAFT_NAMESPACE);

        DimensionType dimensionType = dataPack.getDimensionType(key);
        if (dimensionType == null) {
            Logger.global.logWarning("No dimension-type found with the id '" + key.getFormatted() + "', using fallback.");
            dimensionType = DimensionType.OVERWORLD;
        }

        return dimensionType;
    }

}
