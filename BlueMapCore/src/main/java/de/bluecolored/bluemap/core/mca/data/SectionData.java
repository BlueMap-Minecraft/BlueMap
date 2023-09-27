package de.bluecolored.bluemap.core.mca.data;

import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluenbt.NBTName;
import lombok.Getter;

@Getter
@SuppressWarnings("FieldMayBeFinal")
public class SectionData {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final BlockStatesData EMPTY_BLOCKSTATESDATA = new BlockStatesData();
    private static final BlockState[] EMPTY_BLOCKSTATE_ARRAY = new BlockState[0];
    private static final BiomesData EMPTY_BIOMESDATA = new BiomesData();

    private int y = 0;
    private byte[] blockLight = EMPTY_BYTE_ARRAY;
    private byte[] skyLight = EMPTY_BYTE_ARRAY;
    @NBTName("block_states")
    private BlockStatesData blockStates = EMPTY_BLOCKSTATESDATA;
    private BiomesData biomes = EMPTY_BIOMESDATA;

    // <= 1.16
    @NBTName("BlockStates")
    private long[] blockStatesData = EMPTY_LONG_ARRAY;
    private BlockState[] palette = EMPTY_BLOCKSTATE_ARRAY;

}
