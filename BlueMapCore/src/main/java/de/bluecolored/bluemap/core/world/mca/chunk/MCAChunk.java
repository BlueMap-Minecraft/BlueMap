package de.bluecolored.bluemap.core.world.mca.chunk;

import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.mca.region.MCARegion;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public abstract class MCAChunk implements Chunk {

    protected static final int BLOCKS_PER_SECTION = 16 * 16 * 16;
    protected static final int BIOMES_PER_SECTION = 4 * 4 * 4;
    protected static final int VALUES_PER_HEIGHTMAP = 16 * 16;

    protected static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    protected static final int[] EMPTY_INT_ARRAY = new int[0];
    protected static final long[] EMPTY_LONG_ARRAY = new long[0];
    protected static final String[] EMPTY_STRING_ARRAY = new String[0];
    protected static final BlockState[] EMPTY_BLOCKSTATE_ARRAY = new BlockState[0];

    private final MCARegion region;
    private final int dataVersion;

    public MCAChunk(MCARegion region, Data chunkData) {
        this.region = region;
        this.dataVersion = chunkData.getDataVersion();
    }

    @SuppressWarnings("FieldMayBeFinal")
    @Getter
    public static class Data {
        private int dataVersion = 0;
    }

}
