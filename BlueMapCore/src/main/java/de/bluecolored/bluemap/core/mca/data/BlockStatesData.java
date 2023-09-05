package de.bluecolored.bluemap.core.mca.data;

import de.bluecolored.bluemap.core.world.BlockState;
import lombok.Getter;

@Getter
@SuppressWarnings("FieldMayBeFinal")
public class BlockStatesData {
    private static final long[] EMPTY_LONG_ARRAY = new long[0];
    private static final BlockState[] EMPTY_BLOCKSTATE_ARRAY = new BlockState[0];

    private BlockState[] palette = EMPTY_BLOCKSTATE_ARRAY;
    private long[] data = EMPTY_LONG_ARRAY;

}
