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
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.LightData;
import net.querz.nbt.*;

import java.util.*;
import java.util.Map.Entry;

@SuppressWarnings("FieldMayBeFinal")
public class ChunkAnvil118 extends MCAChunk {
    private boolean isGenerated;
    private boolean hasLight;

    private long inhabitedTime;

    private int sectionMin, sectionMax;
    private Section[] sections;

    @SuppressWarnings("unchecked")
    public ChunkAnvil118(MCAWorld world, CompoundTag chunkTag) {
        super(world, chunkTag);

        String status = chunkTag.getString("Status");
        this.isGenerated = status.equals("full");
        this.hasLight = isGenerated;

        this.inhabitedTime = chunkTag.getLong("InhabitedTime");

        if (!isGenerated && getWorld().isIgnoreMissingLightData()) {
            isGenerated = !status.equals("empty");
        }

        if (chunkTag.containsKey("sections")) {
            this.sectionMin = Integer.MAX_VALUE;
            this.sectionMax = Integer.MIN_VALUE;

            ListTag<CompoundTag> sectionsTag = (ListTag<CompoundTag>) chunkTag.getListTag("sections");
            ArrayList<Section> sectionList = new ArrayList<>(sectionsTag.size());

            for (CompoundTag sectionTag : sectionsTag) {

                // skip empty sections
                CompoundTag blockStatesTag = sectionTag.getCompoundTag("block_states");
                if (blockStatesTag == null) continue;
                ListTag<CompoundTag> paletteTag = (ListTag<CompoundTag>) blockStatesTag.getListTag("palette");
                if (paletteTag == null) continue;
                if (paletteTag.size() == 0) continue;
                if (paletteTag.size() == 1 && BlockState.AIR.getFormatted().equals(paletteTag.get(0).getString("Name"))) continue;

                Section section = new Section(sectionTag);
                int y = section.getSectionY();

                if (sectionMin > y) sectionMin = y;
                if (sectionMax < y) sectionMax = y;

                sectionList.add(section);
            }

            sections = new Section[1 + sectionMax - sectionMin];
            for (Section section : sectionList) {
                sections[section.sectionY - sectionMin] = section;
            }
        } else {
            sections = new Section[0];
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

    private Section getSection(int y) {
        y -= sectionMin;
        if (y < 0 || y >= this.sections.length) return null;
        return this.sections[y];
    }

    private static class Section {
        private static final long[] EMPTY_LONG_ARRAY = new long[0];
        private static final BlockState[] EMPTY_BLOCK_STATE_ARRAY = new BlockState[0];
        private static final String[] EMPTY_STRING_ARRAY = new String[0];

        private int sectionY;
        private byte[] blockLight;
        private byte[] skyLight;
        private long[] blocks = EMPTY_LONG_ARRAY;
        private long[] biomes = EMPTY_LONG_ARRAY;
        private BlockState[] blockPalette = EMPTY_BLOCK_STATE_ARRAY;
        private String[] biomePalette = EMPTY_STRING_ARRAY;

        private int bitsPerBlock, bitsPerBiome;

        @SuppressWarnings("unchecked")
        public Section(CompoundTag sectionData) {
            this.sectionY = sectionData.get("Y", NumberTag.class).asInt();
            this.blockLight = sectionData.getByteArray("BlockLight");
            this.skyLight = sectionData.getByteArray("SkyLight");

            // blocks
            CompoundTag blockStatesTag = sectionData.getCompoundTag("block_states");
            if (blockStatesTag != null) {
                // block data
                this.blocks = blockStatesTag.getLongArray("data");

                // block palette
                ListTag<CompoundTag> paletteTag = (ListTag<CompoundTag>) blockStatesTag.getListTag("palette");
                if (paletteTag != null) {
                    this.blockPalette = new BlockState[paletteTag.size()];
                    for (int i = 0; i < this.blockPalette.length; i++) {
                        blockPalette[i] = readBlockStatePaletteEntry(paletteTag.get(i));
                    }
                }
            }

            // biomes
            CompoundTag biomesTag = sectionData.getCompoundTag("biomes");
            if (biomesTag != null) {
                // biomes data
                this.biomes = biomesTag.getLongArray("data");

                // biomes palette
                ListTag<StringTag> paletteTag = (ListTag<StringTag>) biomesTag.getListTag("palette");
                if (paletteTag != null) {
                    this.biomePalette = new String[paletteTag.size()];
                    for (int i = 0; i < this.biomePalette.length; i++) {
                        biomePalette[i] = paletteTag.get(i).getValue();
                    }
                }
            }

            if (blocks.length < 256 && blocks.length > 0) blocks = Arrays.copyOf(blocks, 256);
            if (blockLight.length < 2048 && blockLight.length > 0) blockLight = Arrays.copyOf(blockLight, 2048);
            if (skyLight.length < 2048 && skyLight.length > 0) skyLight = Arrays.copyOf(skyLight, 2048);

            this.bitsPerBlock = this.blocks.length >> 6; // available longs * 64 (bits per long) / 4096 (blocks per section) (floored result)
            this.bitsPerBiome = Integer.SIZE - Integer.numberOfLeadingZeros(this.biomePalette.length - 1);
        }

        private BlockState readBlockStatePaletteEntry(CompoundTag paletteEntry) {
            String id = paletteEntry.getString("Name");
            if (BlockState.AIR.getFormatted().equals(id)) return BlockState.AIR; //shortcut to save time and memory

            Map<String, String> properties = new LinkedHashMap<>();
            if (paletteEntry.containsKey("Properties")) {
                CompoundTag propertiesTag = paletteEntry.getCompoundTag("Properties");
                for (Entry<String, Tag<?>> property : propertiesTag) {
                    properties.put(property.getKey().toLowerCase(), ((StringTag) property.getValue()).getValue().toLowerCase());
                }
            }

            return new BlockState(id, properties);
        }

        public int getSectionY() {
            return sectionY;
        }

        public BlockState getBlockState(int x, int y, int z) {
            if (blocks.length == 0) return BlockState.AIR;
            if (blockPalette.length == 1) return blockPalette[0];

            x &= 0xF; y &= 0xF; z &= 0xF; // Math.floorMod(pos.getX(), 16)

            int blockIndex = y * 256 + z * 16 + x;

            long value = MCAMath.getValueFromLongArray(blocks, blockIndex, bitsPerBlock);
            if (value >= blockPalette.length) {
                Logger.global.noFloodWarning("palettewarning", "Got block-palette value " + value + " but palette has size of " + blockPalette.length + "! (Future occasions of this error will not be logged)");
                return BlockState.MISSING;
            }

            return blockPalette[(int) value];
        }

        public LightData getLightData(int x, int y, int z, LightData target) {
            if (blockLight.length == 0 && skyLight.length == 0) return target.set(0, 0);

            x &= 0xF; y &= 0xF; z &= 0xF; // Math.floorMod(pos.getX(), 16)

            int blockByteIndex = y * 256 + z * 16 + x;
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

            x = (x & 0xF) / 4; // Math.floorMod(pos.getX(), 16) / 4
            z = (z & 0xF) / 4;
            y = (y & 0xF) / 4;
            int biomeIndex = y * 16 + z * 4 + x;

            long value = MCAMath.getValueFromLongArray(biomes, biomeIndex, bitsPerBiome);
            if (value >= biomePalette.length) {
                Logger.global.noFloodWarning("biomepalettewarning", "Got biome-palette value " + value + " but palette has size of " + biomePalette.length + "! (Future occasions of this error will not be logged)");
                return Biome.DEFAULT.getValue();
            }

            return biomePalette[(int) value];
        }
    }

}
