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
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.DimensionType;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.biome.Biome;
import de.bluecolored.bluemap.core.world.block.entity.BlockEntity;
import de.bluecolored.bluemap.core.world.mca.MCAUtil;
import de.bluecolored.bluemap.core.world.mca.MCAWorld;
import de.bluecolored.bluemap.core.world.mca.PackedIntArrayAccess;
import de.bluecolored.bluemap.core.world.mca.data.LenientBlockEntityArrayDeserializer;
import de.bluecolored.bluenbt.NBTDeserializer;
import de.bluecolored.bluenbt.NBTName;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Chunk_1_16 extends MCAChunk {

    private static final Key STATUS_EMPTY = new Key("minecraft", "empty");
    private static final Key STATUS_FULL = new Key("minecraft", "full");

    private final boolean generated;
    private final boolean hasLightData;
    private final long inhabitedTime;

    private final int skyLight;

    private final boolean hasWorldSurfaceHeights;
    private final PackedIntArrayAccess worldSurfaceHeights;
    private final boolean hasOceanFloorHeights;
    private final PackedIntArrayAccess oceanFloorHeights;

    private final Section[] sections;
    private final int sectionMin, sectionMax;

    private final int[] biomes;
    private final Map<Long, BlockEntity> blockEntities;

    public Chunk_1_16(MCAWorld world, Data data) {
        super(world, data);

        Level level = data.level;

        this.generated = !STATUS_EMPTY.equals(level.status);
        this.hasLightData = STATUS_FULL.equals(level.status);
        this.inhabitedTime = level.inhabitedTime;

        DimensionType dimensionType = getWorld().getDimensionType();
        this.skyLight = dimensionType.hasSkylight() ? 15 : 0;

        int worldHeight = dimensionType.getHeight();
        int bitsPerHeightmapElement = MCAUtil.ceilLog2(worldHeight + 1);

        this.worldSurfaceHeights = new PackedIntArrayAccess(bitsPerHeightmapElement, level.heightmaps.worldSurface);
        this.oceanFloorHeights = new PackedIntArrayAccess(bitsPerHeightmapElement, level.heightmaps.oceanFloor);

        this.hasWorldSurfaceHeights = this.worldSurfaceHeights.isCorrectSize(VALUES_PER_HEIGHTMAP);
        this.hasOceanFloorHeights = this.oceanFloorHeights.isCorrectSize(VALUES_PER_HEIGHTMAP);

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

        // load block-entities
        this.blockEntities = new HashMap<>();
        for (int i = 0; i < level.blockEntities.length; i++) {
            BlockEntity be = level.blockEntities[i];
            if (be == null) continue;

            long hash = (long) be.getY() << 8 | (be.getX() & 0xF) << 4 | be.getZ() & 0xF;
            blockEntities.put(hash, be);
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
    public Biome getBiome(int x, int y, int z) {
        if (this.biomes.length < 16) return Biome.DEFAULT;

        int biomeIntIndex = (y & 0b1100) << 2 | z & 0b1100 | (x & 0b1100) >> 2;

        // shift y up/down if not in range
        if (biomeIntIndex >= biomes.length) biomeIntIndex -= (((biomeIntIndex - biomes.length) >> 4) + 1) * 16;
        if (biomeIntIndex < 0) biomeIntIndex -= (biomeIntIndex >> 4) * 16;

        Biome biome = getWorld().getDataPack().getBiome(biomes[biomeIntIndex]);
        return biome != null ? biome : Biome.DEFAULT;
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
        return worldSurfaceHeights.get((z & 0xF) << 4 | x & 0xF);
    }

    @Override
    public boolean hasOceanFloorHeights() {
        return hasOceanFloorHeights;
    }

    @Override
    public int getOceanFloorY(int x, int z) {
        return oceanFloorHeights.get((z & 0xF) << 4 | x & 0xF);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(int x, int y, int z) {
        return blockEntities.get((long) y << 8 | (x & 0xF) << 4 | z & 0xF);
    }

    @Override
    public void iterateBlockEntities(Consumer<BlockEntity> consumer) {
        blockEntities.values().forEach(consumer);
    }

    private @Nullable Section getSection(int y) {
        y -= sectionMin;
        if (y < 0 || y >= this.sections.length) return null;
        return this.sections[y];
    }

    protected static class Section {

        private final int sectionY;
        private final BlockState[] blockPalette;
        private final PackedIntArrayAccess blocks;
        private final byte[] blockLight;
        private final byte[] skyLight;

        public Section(SectionData sectionData) {
            this.sectionY = sectionData.y;

            this.blockPalette = sectionData.palette;
            this.blocks = new PackedIntArrayAccess(sectionData.blockStates, BLOCKS_PER_SECTION);

            this.blockLight = sectionData.getBlockLight();
            this.skyLight = sectionData.getSkyLight();
        }

        public BlockState getBlockState(int x, int y, int z) {
            if (blockPalette.length == 1) return blockPalette[0];
            if (blockPalette.length == 0) return BlockState.AIR;

            int id = blocks.get((y & 0xF) << 8 | (z & 0xF) << 4 | x & 0xF);
            if (id >= blockPalette.length) {
                Logger.global.noFloodWarning("palette-warning", "Got block-palette id " + id + " but palette has size of " + blockPalette.length + "! (Future occasions of this error will not be logged)");
                return BlockState.MISSING;
            }

            return blockPalette[id];
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

        @NBTName("Level")
        private Level level = new Level();

    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class Level {

        @NBTName("Status")
        private Key status = STATUS_EMPTY;

        @NBTName("InhabitedTime")
        private long inhabitedTime = 0;

        @NBTName("Heightmaps")
        private HeightmapsData heightmaps = new HeightmapsData();

        @NBTName("Sections")
        private SectionData @Nullable [] sections = null;

        @NBTName("Biomes")
        private int[] biomes = EMPTY_INT_ARRAY;

        @NBTName("TileEntities")
        @NBTDeserializer(LenientBlockEntityArrayDeserializer.class)
        private @Nullable BlockEntity [] blockEntities = EMPTY_BLOCK_ENTITIES_ARRAY;

    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class HeightmapsData {

        @NBTName("WORLD_SURFACE")
        private long[] worldSurface = EMPTY_LONG_ARRAY;

        @NBTName("OCEAN_FLOOR")
        private long[] oceanFloor = EMPTY_LONG_ARRAY;

    }

    @Getter
    @SuppressWarnings("FieldMayBeFinal")
    public static class SectionData {

        @NBTName("Y")
        private int y = 0;

        @NBTName("BlockLight")
        private byte[] blockLight = EMPTY_BYTE_ARRAY;

        @NBTName("SkyLight")
        private byte[] skyLight = EMPTY_BYTE_ARRAY;

        @NBTName("Palette")
        private BlockState[] palette = EMPTY_BLOCKSTATE_ARRAY;

        @NBTName("BlockStates")
        private long[] blockStates = EMPTY_LONG_ARRAY;

    }

}
