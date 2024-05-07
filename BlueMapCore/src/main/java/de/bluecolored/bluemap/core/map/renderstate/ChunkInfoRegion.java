package de.bluecolored.bluemap.core.map.renderstate;

import de.bluecolored.bluenbt.NBTName;
import de.bluecolored.bluenbt.NBTPostDeserialize;
import lombok.Getter;

import static de.bluecolored.bluemap.core.map.renderstate.MapChunkState.SHIFT;

public class ChunkInfoRegion implements CellStorage.Cell {

    static final int REGION_LENGTH = 1 << SHIFT;
    static final int REGION_MASK = REGION_LENGTH - 1;
    static final int CHUNKS_PER_REGION = REGION_LENGTH * REGION_LENGTH;

    @NBTName("chunk-hashes")
    private int[] chunkHashes;

    @Getter
    private transient boolean modified;

    private ChunkInfoRegion() {}

    @NBTPostDeserialize
    public void init() {
        if (chunkHashes == null || chunkHashes.length != CHUNKS_PER_REGION)
            chunkHashes = new int[CHUNKS_PER_REGION];
    }

    public int get(int x, int z) {
        return chunkHashes[index(x, z)];
    }

    public int set(int x, int z, int hash) {
        int index = index(x, z);
        int previous = chunkHashes[index];

        chunkHashes[index] = hash;

        if (previous != hash)
            modified = true;

        return previous;
    }

    private static int index(int x, int z) {
        return (z & REGION_MASK) << SHIFT | (x & REGION_MASK);
    }

    public static ChunkInfoRegion create() {
        ChunkInfoRegion region = new ChunkInfoRegion();
        region.init();
        return region;
    }

}
