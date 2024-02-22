package de.bluecolored.bluemap.core.world.mca.chunk;

import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.mca.MCAWorld;

public class Chunk_1_15 extends Chunk_1_13 {

    public Chunk_1_15(MCAWorld world, Data data) {
        super(world, data);
    }

    @Override
    public String getBiome(int x, int y, int z) {
        if (this.biomes.length < 16) return Biome.DEFAULT.getFormatted();

        int biomeIntIndex = (y & 0b1100) << 2 | z & 0b1100 | (x & 0b1100) >> 2;

        // shift y up/down if not in range
        if (biomeIntIndex >= biomes.length) biomeIntIndex -= (((biomeIntIndex - biomes.length) >> 4) + 1) * 16;
        if (biomeIntIndex < 0) biomeIntIndex -= (biomeIntIndex >> 4) * 16;

        return LegacyBiomes.idFor(biomes[biomeIntIndex]);
    }

}
