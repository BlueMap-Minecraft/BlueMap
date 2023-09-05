package de.bluecolored.bluemap.core.mca.data;

import de.bluecolored.bluenbt.NBTName;
import lombok.Getter;

@Getter
@SuppressWarnings("FieldMayBeFinal")
public class HeightmapsData {
    private static final long[] EMPTY_LONG_ARRAY = new long[0];

    @NBTName("WORLD_SURFACE")
    private long[] worldSurface = EMPTY_LONG_ARRAY;

    @NBTName("OCEAN_FLOOR")
    private long[] oceanFloor = EMPTY_LONG_ARRAY;

}
