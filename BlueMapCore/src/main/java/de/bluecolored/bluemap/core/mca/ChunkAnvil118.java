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
package de.bluecolored.bluemap.core.mca;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.data.*;
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.LightData;

public class ChunkAnvil118 extends MCAChunk {

    private final boolean isGenerated;
    private final boolean hasLight;

    private final long inhabitedTime;

    private final int sectionMin, sectionMax;
    private final Section[] sections;

    private final long[] oceanFloorHeights;
    private final long[] worldSurfaceHeights;

    public ChunkAnvil118(MCAWorld world, ChunkData chunkData) {
        super(world, chunkData);

        String status = chunkData.getStatus();
        boolean generated = status.equals("minecraft:full") || status.equals("full");
        this.hasLight = generated;
        if (!generated && getWorld().isIgnoreMissingLightData())
            generated = !status.equals("empty") && !status.equals("minecraft:empty");
        this.isGenerated = generated;

        this.inhabitedTime = chunkData.getInhabitedTime();

        HeightmapsData heightmapsData = chunkData.getHeightmaps();
        this.worldSurfaceHeights = heightmapsData.getWorldSurface();
        this.oceanFloorHeights = heightmapsData.getOceanFloor();

        SectionData[] sectionDatas = chunkData.getSections();
        if (sectionDatas != null && sectionDatas.length > 0) {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;

            // find section min/max y
            for (SectionData sectionData : sectionDatas) {
                int y = sectionData.getY();
                if (min > y) min = y;
                if (max < y) max = y;
            }

            // load sections into ordered array
            this.sections = new Section[1 + max - min];
            for (SectionData sectionData : sectionDatas) {
                Section section = new Section(sectionData);
                int y = section.getSectionY();

                if (min > y) min = y;
                if (max < y) max = y;

                sections[section.sectionY - min] = section;
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
        return isGenerated;
    }

    @Override
    public long getInhabitedTime() {
        return inhabitedTime;
    }

    @Override
    public BlockState getBlockState(int x, int y, int z) {
        int sectionY = y >> 4;

        Section section = getSection(sectionY);
        if (section == null) return BlockState.AIR;

        return section.getBlockState(x, y, z);
    }

    @Override
    public LightData getLightData(int x, int y, int z, LightData target) {
        if (!hasLight) return target.set(getWorld().getSkyLight(), 0);

        int sectionY = y >> 4;

        Section section = getSection(sectionY);
        if (section == null) return (sectionY < sectionMin) ? target.set(0, 0) : target.set(getWorld().getSkyLight(), 0);

        return section.getLightData(x, y, z, target);
    }

    @Override
    public String getBiome(int x, int y, int z) {
        int sectionY = y >> 4;

        Section section = getSection(sectionY);
        if (section == null) return Biome.DEFAULT.getFormatted();

        return section.getBiome(x, y, z);
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
    public int getWorldSurfaceY(int x, int z) {
        if (this.worldSurfaceHeights.length < 37) return 0;

        x &= 0xF; z &= 0xF;
        return (int) MCAMath.getValueFromLongArray(this.worldSurfaceHeights, z * 16 + x, 9) - 64;
    }

    @Override
    public int getOceanFloorY(int x, int z) {
        if (this.oceanFloorHeights.length < 37) return 0;

        x &= 0xF; z &= 0xF;
        return (int) MCAMath.getValueFromLongArray(this.oceanFloorHeights, z * 16 + x, 9) - 64;
    }

    private Section getSection(int y) {
        y -= sectionMin;
        if (y < 0 || y >= this.sections.length) return null;
        return this.sections[y];
    }

    private static class Section {
        private final int sectionY;
        private final byte[] blockLight;
        private final byte[] skyLight;
        private final long[] blocks;
        private final long[] biomes;
        private final BlockState[] blockPalette;
        private final String[] biomePalette;

        private final int bitsPerBlock, bitsPerBiome;

        public Section(SectionData sectionData) {
            this.sectionY = sectionData.getY();
            this.blockLight = sectionData.getBlockLight();
            this.skyLight = sectionData.getSkyLight();

            BlockStatesData blockStates = sectionData.getBlockStates();
            this.blocks = blockStates.getData();
            this.blockPalette = blockStates.getPalette();

            BiomesData biomesData = sectionData.getBiomes();
            this.biomes = biomesData.getData();
            this.biomePalette = biomesData.getPalette();

            this.bitsPerBlock = this.blocks.length >> 6; // available longs * 64 (bits per long) / 4096 (blocks per section) (floored result)
            this.bitsPerBiome = MCAMath.ceilLog2(this.biomePalette.length);
        }

        public int getSectionY() {
            return sectionY;
        }

        public BlockState getBlockState(int x, int y, int z) {
            if (blockPalette.length == 1) return blockPalette[0];
            if (blocks.length == 0) return BlockState.AIR;

            int blockIndex = (y & 0xF) << 8 | (z & 0xF) << 4 | x & 0xF;
            long value = MCAMath.getValueFromLongArray(blocks, blockIndex, bitsPerBlock);
            if (value >= blockPalette.length) {
                Logger.global.noFloodWarning("palettewarning", "Got block-palette value " + value + " but palette has size of " + blockPalette.length + "! (Future occasions of this error will not be logged)");
                return BlockState.MISSING;
            }

            return blockPalette[(int) value];
        }

        public LightData getLightData(int x, int y, int z, LightData target) {
            if (blockLight.length == 0 && skyLight.length == 0) return target.set(0, 0);

            int blockByteIndex = (y & 0xF) << 8 | (z & 0xF) << 4 | x & 0xF;
            int blockHalfByteIndex = blockByteIndex >> 1; // blockByteIndex / 2
            boolean largeHalf = (blockByteIndex & 0x1) != 0; // (blockByteIndex % 2) == 0

            return target.set(
                    this.skyLight.length > 0 ? MCAMath.getByteHalf(this.skyLight[blockHalfByteIndex], largeHalf) : 0,
                    this.blockLight.length > 0 ? MCAMath.getByteHalf(this.blockLight[blockHalfByteIndex], largeHalf) : 0
            );
        }

        public String getBiome(int x, int y, int z) {
            if (biomePalette.length == 0) return Biome.DEFAULT.getValue();
            if (biomePalette.length == 1 || biomes.length == 0) return biomePalette[0];

            int biomeIndex = (y & 0b1100) << 2 | z & 0b1100 | (x & 0b1100) >> 2;
            long value = MCAMath.getValueFromLongArray(biomes, biomeIndex, bitsPerBiome);
            if (value >= biomePalette.length) {
                Logger.global.noFloodWarning("biomepalettewarning", "Got biome-palette value " + value + " but palette has size of " + biomePalette.length + "! (Future occasions of this error will not be logged)");
                return Biome.DEFAULT.getValue();
            }

            return biomePalette[(int) value];
        }
    }

    private static PackedIntArrayAccess heightmap(int worldHeight, long[] data) {
        return new PackedIntArrayAccess(MCAMath.ceilLog2(worldHeight + 1), data);
    }

}
