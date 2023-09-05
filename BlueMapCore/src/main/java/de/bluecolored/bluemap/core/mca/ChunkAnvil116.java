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

@SuppressWarnings("FieldMayBeFinal")
public class ChunkAnvil116 /* extends MCAChunk */ {
    private static final long[] EMPTY_LONG_ARRAY = new long[0];

    /*

    private boolean isGenerated;
    private boolean hasLight;

    private long inhabitedTime;

    private int sectionMin, sectionMax;
    private Section[] sections;

    private int[] biomes;

    private long[] oceanFloorHeights = EMPTY_LONG_ARRAY;
    private long[] worldSurfaceHeights = EMPTY_LONG_ARRAY;

    @SuppressWarnings("unchecked")
    public ChunkAnvil116(MCAWorld world, CompoundTag chunkTag) {
        super(world, chunkTag);

        CompoundTag levelData = chunkTag.getCompoundTag("Level");

        String status = levelData.getString("Status");
        this.isGenerated = status.equals("full");
        this.hasLight = isGenerated;

        this.inhabitedTime = levelData.getLong("InhabitedTime");

        if (!isGenerated && getWorld().isIgnoreMissingLightData()) {
            isGenerated = !status.equals("empty");
        }

        if (levelData.containsKey("Heightmaps")) {
            CompoundTag heightmapsTag = levelData.getCompoundTag("Heightmaps");
            this.worldSurfaceHeights = heightmapsTag.getLongArray("WORLD_SURFACE");
            this.oceanFloorHeights = heightmapsTag.getLongArray("OCEAN_FLOOR");
        }

        if (levelData.containsKey("Sections")) {
            this.sectionMin = Integer.MAX_VALUE;
            this.sectionMax = Integer.MIN_VALUE;

            ListTag<CompoundTag> sectionsTag = (ListTag<CompoundTag>) levelData.getListTag("Sections");
            ArrayList<Section> sectionList = new ArrayList<>(sectionsTag.size());

            for (CompoundTag sectionTag : sectionsTag) {
                if (sectionTag.getListTag("Palette") == null) continue; // ignore empty sections

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

        Tag<?> tag = levelData.get("Biomes"); //tag can be byte-array or int-array
        if (tag instanceof ByteArrayTag) {
            byte[] bs = ((ByteArrayTag) tag).getValue();
            this.biomes = new int[bs.length];

            for (int i = 0; i < bs.length; i++) {
                biomes[i] = bs[i] & 0xFF;
            }
        }
        else if (tag instanceof IntArrayTag) {
            this.biomes = ((IntArrayTag) tag).getValue();
        }

        if (biomes == null) {
            this.biomes = new int[0];
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
        if (biomes.length < 16) return Biome.DEFAULT.getFormatted();

        x = (x & 0xF) / 4; // Math.floorMod(pos.getX(), 16)
        z = (z & 0xF) / 4;
        y = y / 4;
        int biomeIntIndex = y * 16 + z * 4 + x; // TODO: fix this for 1.17+ worlds with negative y?

        // shift y up/down if not in range
        if (biomeIntIndex >= biomes.length) biomeIntIndex -= (((biomeIntIndex - biomes.length) >> 4) + 1) * 16;
        if (biomeIntIndex < 0) biomeIntIndex -= (biomeIntIndex >> 4) * 16;

        return LegacyBiomes.idFor(biomes[biomeIntIndex]);
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
        return (int) MCAMath.getValueFromLongArray(this.worldSurfaceHeights, z * 16 + x, 9);
    }

    @Override
    public int getOceanFloorY(int x, int z) {
        if (this.oceanFloorHeights.length < 37) return 0;

        x &= 0xF; z &= 0xF;
        return (int) MCAMath.getValueFromLongArray(this.oceanFloorHeights, z * 16 + x, 9);
    }

    private Section getSection(int y) {
        y -= sectionMin;
        if (y < 0 || y >= this.sections.length) return null;
        return this.sections[y];
    }

    private static class Section {
        private static final String AIR_ID = "minecraft:air";

        private int sectionY;
        private byte[] blockLight;
        private byte[] skyLight;
        private long[] blocks;
        private BlockState[] palette;

        private int bitsPerBlock;

        @SuppressWarnings("unchecked")
        public Section(CompoundTag sectionData) {
            this.sectionY = sectionData.get("Y", NumberTag.class).asInt();
            this.blockLight = sectionData.getByteArray("BlockLight");
            this.skyLight = sectionData.getByteArray("SkyLight");
            this.blocks = sectionData.getLongArray("BlockStates");

            if (blocks.length < 256 && blocks.length > 0) blocks = Arrays.copyOf(blocks, 256);
            if (blockLight.length < 2048 && blockLight.length > 0) blockLight = Arrays.copyOf(blockLight, 2048);
            if (skyLight.length < 2048 && skyLight.length > 0) skyLight = Arrays.copyOf(skyLight, 2048);

            //read block palette
            ListTag<CompoundTag> paletteTag = (ListTag<CompoundTag>) sectionData.getListTag("Palette");
            if (paletteTag != null) {
                this.palette = new BlockState[paletteTag.size()];
                for (int i = 0; i < this.palette.length; i++) {
                    CompoundTag stateTag = paletteTag.get(i);

                    String id = stateTag.getString("Name"); //shortcut to save time and memory
                    if (id.equals(AIR_ID)) {
                        palette[i] = BlockState.AIR;
                        continue;
                    }

                    Map<String, String> properties = new HashMap<>();

                    if (stateTag.containsKey("Properties")) {
                        CompoundTag propertiesTag = stateTag.getCompoundTag("Properties");
                        for (Entry<String, Tag<?>> property : propertiesTag) {
                            properties.put(property.getKey().toLowerCase(), ((StringTag) property.getValue()).getValue().toLowerCase());
                        }
                    }

                    palette[i] = new BlockState(id, properties);
                }
            } else {
                this.palette = new BlockState[0];
            }

            this.bitsPerBlock = this.blocks.length >> 6; // available longs * 64 (bits per long) / 4096 (blocks per section) (floored result)
        }

        public int getSectionY() {
            return sectionY;
        }

        public BlockState getBlockState(int x, int y, int z) {
            if (palette.length == 1) return palette[0];
            if (blocks.length == 0) return BlockState.AIR;

            x &= 0xF; y &= 0xF; z &= 0xF; // Math.floorMod(pos.getX(), 16)

            int blockIndex = y * 256 + z * 16 + x;

            long value = MCAMath.getValueFromLongArray(blocks, blockIndex, bitsPerBlock);
            if (value >= palette.length) {
                Logger.global.noFloodWarning("palettewarning", "Got palette value " + value + " but palette has size of " + palette.length + "! (Future occasions of this error will not be logged)");
                return BlockState.MISSING;
            }

            return palette[(int) value];
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
    }

    */

}
