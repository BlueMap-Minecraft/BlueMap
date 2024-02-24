/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.core.world.mca.chunk;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.DimensionType;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.mca.MCAUtil;
import de.bluecolored.bluemap.core.world.mca.MCAWorld;
import de.bluecolored.bluemap.core.world.mca.PackedIntArrayAccess;
import de.bluecolored.bluenbt.NBTName;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public class Chunk_1_18 extends MCAChunk {

    private static final BlockStatesData EMPTY_BLOCKSTATESDATA = new BlockStatesData();
    private static final BiomesData EMPTY_BIOMESDATA = new BiomesData();
    private static final HeightmapsData EMPTY_HEIGHTMAPS_DATA = new HeightmapsData();

    private static final Key STATUS_EMPTY = new Key("minecraft", "empty");
    private static final Key STATUS_FULL = new Key("minecraft", "full");

    private final boolean generated;
    private final boolean hasLightData;
    private final long inhabitedTime;

    private final int skyLight;
    private final int worldMinY;

    private final boolean hasWorldSurfaceHeights;
    private final PackedIntArrayAccess worldSurfaceHeights;
    private final boolean hasOceanFloorHeights;
    private final PackedIntArrayAccess oceanFloorHeights;

    private final Section[] sections;
    private final int sectionMin, sectionMax;

    public Chunk_1_18(MCAWorld world, Data data) {
        super(world, data);

        this.generated = !STATUS_EMPTY.equals(data.status);
        this.hasLightData = STATUS_FULL.equals(data.status);
        this.inhabitedTime = data.inhabitedTime;

        DimensionType dimensionType = getWorld().getDimensionType();
        this.worldMinY = dimensionType.getMinY();
        this.skyLight = dimensionType.hasSkylight() ? 16 : 0;

        int worldHeight = dimensionType.getHeight();
        int bitsPerHeightmapElement = MCAUtil.ceilLog2(worldHeight + 1);

        this.worldSurfaceHeights = new PackedIntArrayAccess(bitsPerHeightmapElement, data.heightmaps.worldSurface);
        this.oceanFloorHeights = new PackedIntArrayAccess(bitsPerHeightmapElement, data.heightmaps.oceanFloor);

        this.hasWorldSurfaceHeights = this.worldSurfaceHeights.isCorrectSize(VALUES_PER_HEIGHTMAP);
        this.hasOceanFloorHeights = this.oceanFloorHeights.isCorrectSize(VALUES_PER_HEIGHTMAP);

        SectionData[] sectionsData = data.sections;
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
        Section section = getSection(y >> 4);
        if (section == null) return Biome.DEFAULT.getFormatted();

        return section.getBiome(x, y, z);
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
        return worldSurfaceHeights.get((z & 0xF) << 4 | x & 0xF) + worldMinY;
    }

    @Override
    public boolean hasOceanFloorHeights() {
        return hasOceanFloorHeights;
    }

    @Override
    public int getOceanFloorY(int x, int z) {
        return oceanFloorHeights.get((z & 0xF) << 4 | x & 0xF) + worldMinY;
    }

    private @Nullable Section getSection(int y) {
        y -= sectionMin;
        if (y < 0 || y >= this.sections.length) return null;
        return this.sections[y];
    }

    protected static class Section {

        private final int sectionY;
        private final BlockState[] blockPalette;
        private final String[] biomePalette;
        private final PackedIntArrayAccess blocks;
        private final PackedIntArrayAccess biomes;
        private final byte[] blockLight;
        private final byte[] skyLight;

        public Section(SectionData sectionData) {
            this.sectionY = sectionData.y;

            this.blockPalette = sectionData.blockStates.palette;
            this.biomePalette = sectionData.biomes.palette;

            this.blocks = new PackedIntArrayAccess(sectionData.blockStates.data, BLOCKS_PER_SECTION);
            this.biomes = new PackedIntArrayAccess(Math.max(MCAUtil.ceilLog2(this.biomePalette.length), 1), sectionData.biomes.data);

            this.blockLight = sectionData.blockLight;
            this.skyLight = sectionData.skyLight;
        }

        public BlockState getBlockState(int x, int y, int z) {
            if (blockPalette.length == 1) return blockPalette[0];
            if (blockPalette.length == 0) return BlockState.AIR;

            int id = blocks.get((y & 0xF) << 8 | (z & 0xF) << 4 | x & 0xF);
            if (id >= blockPalette.length) {
                Logger.global.noFloodWarning("palette-warning", "Got block-palette id " + id + " but palette has size of " + blockPalette.length + ".");
                return BlockState.MISSING;
            }

            return blockPalette[id];
        }

        public String getBiome(int x, int y, int z) {
            if (biomePalette.length == 1) return biomePalette[0];
            if (biomePalette.length == 0) return Biome.DEFAULT.getValue();

            int id = biomes.get((y & 0b1100) << 2 | z & 0b1100 | (x & 0b1100) >> 2);
            if (id >= biomePalette.length) {
                Logger.global.noFloodWarning("biome-palette-warning", "Got biome-palette id " + id + " but palette has size of " + biomePalette.length + ".");
                return Biome.DEFAULT.getValue();
            }

            return biomePalette[id];
        }

        public LightData getLightData(int x, int y, int z, LightData target) {
            if (blockLight.length == 0 && skyLight.length == 0) return target.set(0, 0);

            int blockByteIndex = (y & 0xF) << 8 | (z & 0xF) << 4 | x & 0xF;
            int blockHalfByteIndex = blockByteIndex >> 1; // blockByteIndex / 2
            boolean largeHalf = (blockByteIndex & 0x1) != 0; // (blockByteIndex % 2) == 0

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
        private Key status = STATUS_EMPTY;
        private long inhabitedTime = 0;
        private HeightmapsData heightmaps = EMPTY_HEIGHTMAPS_DATA;
        private SectionData @Nullable [] sections = null;
    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class HeightmapsData {
        @NBTName("WORLD_SURFACE") private long[] worldSurface = EMPTY_LONG_ARRAY;
        @NBTName("OCEAN_FLOOR") private long[] oceanFloor = EMPTY_LONG_ARRAY;
    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class SectionData {
        private int y = 0;
        private byte[] blockLight = EMPTY_BYTE_ARRAY;
        private byte[] skyLight = EMPTY_BYTE_ARRAY;
        @NBTName("block_states") private BlockStatesData blockStates = EMPTY_BLOCKSTATESDATA;
        private BiomesData biomes = EMPTY_BIOMESDATA;
    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class BlockStatesData {
        private BlockState[] palette = EMPTY_BLOCKSTATE_ARRAY;
        private long[] data = EMPTY_LONG_ARRAY;
    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class BiomesData {
        private String[] palette = EMPTY_STRING_ARRAY;
        private long[] data = EMPTY_LONG_ARRAY;
    }

}
