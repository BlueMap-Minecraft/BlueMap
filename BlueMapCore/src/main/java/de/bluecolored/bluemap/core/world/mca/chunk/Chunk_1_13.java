package de.bluecolored.bluemap.core.world.mca.chunk;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.DimensionType;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.mca.MCAUtil;
import de.bluecolored.bluemap.core.world.mca.region.MCARegion;
import de.bluecolored.bluenbt.NBTName;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class Chunk_1_13 extends MCAChunk {

    private static final Level EMPTY_LEVEL = new Level();
    private static final HeightmapsData EMPTY_HEIGHTMAPS_DATA = new HeightmapsData();

    private static final Key STATUS_EMPTY = new Key("minecraft", "empty");
    private static final Key STATUS_FULL = new Key("minecraft", "full");
    private static final Key STATUS_FULLCHUNK = new Key("minecraft", "fullchunk");
    private static final Key STATUS_POSTPROCESSED = new Key("minecraft", "postprocessed");

    private final boolean generated;
    private final boolean hasLightData;
    private final long inhabitedTime;

    private final int skyLight;

    private final boolean hasWorldSurfaceHeights;
    private final long[] worldSurfaceHeights;
    private final boolean hasOceanFloorHeights;
    private final long[] oceanFloorHeights;

    private final Section[] sections;
    private final int sectionMin, sectionMax;

    final int[] biomes;

    public Chunk_1_13(MCARegion region, Data data) {
        super(region, data);

        Level level = data.level;

        this.generated = !STATUS_EMPTY.equals(level.status);
        this.hasLightData =
                STATUS_FULL.equals(level.status) ||
                STATUS_FULLCHUNK.equals(level.status) ||
                STATUS_POSTPROCESSED.equals(level.status);
        this.inhabitedTime = level.inhabitedTime;

        DimensionType dimensionType = getRegion().getWorld().getDimensionType();
        this.skyLight = dimensionType.hasSkylight() ? 16 : 0;

        this.worldSurfaceHeights = level.heightmaps.worldSurface;
        this.oceanFloorHeights = level.heightmaps.oceanFloor;

        this.hasWorldSurfaceHeights = this.worldSurfaceHeights.length >= 36;
        this.hasOceanFloorHeights = this.oceanFloorHeights.length >= 36;

        this.biomes = level.biomes;

        SectionData[] sectionsData = level.sections;
        if (sectionsData != null && sectionsData.length > 0) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            // find section min/max y
            for (SectionData sectionData : sectionsData) {
                int y = sectionData.getY();
                if (min > y) min = y;
                if (max < y) max = y;
            }

            // load sections into ordered array
            this.sections = new Section[1 + max - min];
            for (SectionData sectionData : sectionsData) {
                Section section = new Section(sectionData);
                int y = section.getSectionY();

                if (min > y) min = y;
                if (max < y) max = y;

                this.sections[section.sectionY - min] = section;
            }

            this.sectionMin = min;
            this.sectionMax = max;
        } else {
            this.sections = new Section[0];
            this.sectionMin = 0;
            this.sectionMax = 0;
        }
    }

    @Override
    public boolean isGenerated() {
        return generated;
    }

    @Override
    public boolean hasLightData() {
        return hasLightData;
    }

    @Override
    public long getInhabitedTime() {
        return inhabitedTime;
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        Section section = getSection(y >> 4);
        if (section == null) return BlockState.AIR;

        return section.getBlockState(x, y, z);
    }

    @Override
    public String getBiome(int x, int y, int z) {
        if (this.biomes.length < 256) return Biome.DEFAULT.getFormatted();

        int biomeIntIndex = (z & 0xF) << 4 | x & 0xF;
        return LegacyBiomes.idFor(biomes[biomeIntIndex]);
    }

    @Override
    public LightData getLightData(int x, int y, int z, LightData target) {
        if (!hasLightData) return target.set(skyLight, 0);

        int sectionY = y >> 4;
        Section section = getSection(sectionY);
        if (section == null) return (sectionY < sectionMin) ? target.set(0, 0) : target.set(skyLight, 0);

        return section.getLightData(x, y, z, target);
    }

    @Override
    public int getMinY(int x, int z) {
        return sectionMin * 16;
    }

    @Override
    public int getMaxY(int x, int z) {
        return sectionMax * 16 + 15;
    }

    @Override
    public boolean hasWorldSurfaceHeights() {
        return hasWorldSurfaceHeights;
    }

    @Override
    public int getWorldSurfaceY(int x, int z) {
        return (int) MCAUtil.getValueFromLongStream(
                worldSurfaceHeights,
                (z & 0xF) << 4 | x & 0xF,
                9
        );
    }

    @Override
    public boolean hasOceanFloorHeights() {
        return hasOceanFloorHeights;
    }

    @Override
    public int getOceanFloorY(int x, int z) {
        return (int) MCAUtil.getValueFromLongStream(
                oceanFloorHeights,
                (z & 0xF) << 4 | x & 0xF,
                9
        );
    }

    private @Nullable Section getSection(int y) {
        y -= sectionMin;
        if (y < 0 || y >= this.sections.length) return null;
        return this.sections[y];
    }

    protected static class Section {

        private final int sectionY;
        private final BlockState[] blockPalette;
        private final long[] blocks;
        private final byte[] blockLight;
        private final byte[] skyLight;

        private final int bitsPerBlock;

        public Section(SectionData sectionData) {
            this.sectionY = sectionData.y;

            this.blockPalette = sectionData.palette;
            this.blocks = sectionData.blockStates;

            this.blockLight = sectionData.getBlockLight();
            this.skyLight = sectionData.getSkyLight();

            this.bitsPerBlock = this.blocks.length >> 6; // available longs * 64 (bits per long) / 4096 (blocks per section) (floored result)
        }

        public BlockState getBlockState(int x, int y, int z) {
            if (blockPalette.length == 1) return blockPalette[0];
            if (blockPalette.length == 0) return BlockState.AIR;

            int id = (int) MCAUtil.getValueFromLongStream(
                    blocks,
                    (y & 0xF) << 8 | (z & 0xF) << 4 | x & 0xF,
                    bitsPerBlock
            );
            if (id >= blockPalette.length) {
                Logger.global.noFloodWarning("palette-warning", "Got block-palette id " + id + " but palette has size of " + blockPalette.length + "! (Future occasions of this error will not be logged)");
                return BlockState.MISSING;
            }

            return blockPalette[id];
        }

        public LightData getLightData(int x, int y, int z, LightData target) {
            if (blockLight.length == 0 && skyLight.length == 0) return target.set(0, 0);

            int blockByteIndex = (y & 0xF) << 8 | (z & 0xF) << 4 | x & 0xF;
            int blockHalfByteIndex = blockByteIndex >> 1;
            boolean largeHalf = (blockByteIndex & 0x1) != 0;

            return target.set(
                    this.skyLight.length > blockHalfByteIndex ? MCAUtil.getByteHalf(this.skyLight[blockHalfByteIndex], largeHalf) : 0,
                    this.blockLight.length > blockHalfByteIndex ? MCAUtil.getByteHalf(this.blockLight[blockHalfByteIndex], largeHalf) : 0
            );
        }

        public int getSectionY() {
            return sectionY;
        }

    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Data extends MCAChunk.Data {
        private Level level = EMPTY_LEVEL;
    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Level {
        private Key status = STATUS_EMPTY;
        private long inhabitedTime = 0;
        private HeightmapsData heightmaps = EMPTY_HEIGHTMAPS_DATA;
        private SectionData @Nullable [] sections = null;
        private int[] biomes = EMPTY_INT_ARRAY;
    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    protected static class HeightmapsData {
        @NBTName("WORLD_SURFACE") private long[] worldSurface = EMPTY_LONG_ARRAY;
        @NBTName("OCEAN_FLOOR") private long[] oceanFloor = EMPTY_LONG_ARRAY;
    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    protected static class SectionData {
        private int y = 0;
        private byte[] blockLight = EMPTY_BYTE_ARRAY;
        private byte[] skyLight = EMPTY_BYTE_ARRAY;
        private BlockState[] palette = EMPTY_BLOCKSTATE_ARRAY;
        private long[] blockStates = EMPTY_LONG_ARRAY;
    }

}
