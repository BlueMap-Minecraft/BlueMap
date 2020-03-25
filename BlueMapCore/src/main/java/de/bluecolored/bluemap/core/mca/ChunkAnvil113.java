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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.mapping.BiomeMapper;
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.LightData;
import net.querz.nbt.ByteArrayTag;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.IntArrayTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.StringTag;
import net.querz.nbt.Tag;
import net.querz.nbt.mca.MCAUtil;

public class ChunkAnvil113 extends Chunk {
	private BiomeMapper biomeIdMapper;

	private boolean isGenerated;
	private Section[] sections;
	private int[] biomes;
	
	@SuppressWarnings("unchecked")
	public ChunkAnvil113(MCAWorld world, CompoundTag chunkTag) {
		super(world, chunkTag);
		
		biomeIdMapper = getWorld().getBiomeIdMapper();
		
		CompoundTag levelData = chunkTag.getCompoundTag("Level");
		
		String status = levelData.getString("Status");
		isGenerated = status.equals("full") || status.equals("spawn"); // full is normal fully generated and spawn seems to be converted from old format but not yet loaded if you optimized your world
		
		sections = new Section[32]; //32 supports a max world-height of 512 which is the max that the hightmaps of Minecraft V1.13+ can store with 9 bits, i believe?
		if (levelData.containsKey("Sections")) {
			for (CompoundTag sectionTag : ((ListTag<CompoundTag>) levelData.getListTag("Sections"))) {
				Section section = new Section(sectionTag);
				if (section.getSectionY() >= 0) sections[section.getSectionY()] = section;
			}
		}
		
		Tag<?> tag = levelData.get("Biomes"); //tag can be byte-array or int-array
		if (tag instanceof ByteArrayTag) {
			byte[] bs = ((ByteArrayTag) tag).getValue();
			biomes = new int[bs.length];
			
			for (int i = 0; i < bs.length; i++) {
				biomes[i] = bs[i] & 0xFF;
			}
		}
		else if (tag instanceof IntArrayTag) {
			biomes = ((IntArrayTag) tag).getValue(); 
		}
		
		if (biomes == null || biomes.length == 0) {
			biomes = new int[2048];
		}
	}

	@Override
	public boolean isGenerated() {
		return isGenerated;
	}

	@Override
	public BlockState getBlockState(Vector3i pos) {
		int sectionY = MCAUtil.blockToChunk(pos.getY());
		
		Section section = this.sections[sectionY];
		if (section == null) return BlockState.AIR;
		
		return section.getBlockState(pos);
	}

	@Override
	public LightData getLightData(Vector3i pos) {
		int sectionY = MCAUtil.blockToChunk(pos.getY());
		
		Section section = this.sections[sectionY];
		if (section == null) return LightData.SKY;
		
		return section.getLightData(pos);
	}

	@Override
	public Biome getBiome(Vector3i pos) {
		int x = pos.getX() & 0xF; // Math.floorMod(pos.getX(), 16)
		int z = pos.getZ() & 0xF;
		int biomeIntIndex = z * 16 + x;
		
		return biomeIdMapper.get(biomes[biomeIntIndex]);
	}
	
	private class Section {
		private static final String AIR_ID = "minecraft:air";
		
		private int sectionY;
		private byte[] blockLight;
		private byte[] skyLight;
		private long[] blocks;
		private BlockState[] palette;
		
		@SuppressWarnings("unchecked")
		public Section(CompoundTag sectionData) {
			this.sectionY = sectionData.getByte("Y");
			this.blockLight = sectionData.getByteArray("BlockLight");
			if (blockLight.length == 0) blockLight = new byte[2048];
			this.skyLight = sectionData.getByteArray("SkyLight");
			if (skyLight.length == 0) skyLight = new byte[2048];
			this.blocks = sectionData.getLongArray("BlockStates");
			
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
		}
		
		public int getSectionY() {
			return sectionY;
		}
		
		public BlockState getBlockState(Vector3i pos) {
			if (blocks.length == 0) return BlockState.AIR;
			
			int x = pos.getX() & 0xF; // Math.floorMod(pos.getX(), 16)
			int y = pos.getY() & 0xF;
			int z = pos.getZ() & 0xF;
			int blockIndex = y * 256 + z * 16 + x;
			int bitsPerBlock = blocks.length * 64 / 4096; //64 bits per long and 4096 blocks per section
			int index = blockIndex * bitsPerBlock;
			int firstLong = index >> 6; // index / 64
			int bitoffset = index & 0x3F; // Math.floorMod(index, 64)
			
			long value = blocks[firstLong] >>> bitoffset;
			
			if (bitoffset > 0 && firstLong + 1 < blocks.length) {
				long value2 = blocks[firstLong + 1];
				value2 = value2 << -bitoffset;
				value = value | value2;
			}
			
			value = value & (0xFFFFFFFFFFFFFFFFL >>> -bitsPerBlock);
			
			if (value >= palette.length) {
				Logger.global.noFloodWarning("palettewarning", "Got palette value " + value + " but palette has size of " + palette.length + " (Future occasions of this error will not be logged)");
				return BlockState.MISSING;
			}
			
			return palette[(int) value];
		}
		
		public LightData getLightData(Vector3i pos) {
			int x = pos.getX() & 0xF; // Math.floorMod(pos.getX(), 16)
			int y = pos.getY() & 0xF;
			int z = pos.getZ() & 0xF;
			int blockByteIndex = y * 256 + z * 16 + x;
			int blockHalfByteIndex = blockByteIndex >> 1; // blockByteIndex / 2 
			boolean largeHalf = (blockByteIndex & 0x1) != 0; // (blockByteIndex % 2) == 0

			int blockLight = getByteHalf(this.blockLight[blockHalfByteIndex], largeHalf);
			int skyLight = getByteHalf(this.skyLight[blockHalfByteIndex], largeHalf);
			
			return new LightData(skyLight, blockLight);
		}
		
		/**
		 * Extracts the 4 bits of the left (largeHalf = <code>true</code>) or the right (largeHalf = <code>false</code>) side of the byte stored in <code>value</code>.<br> 
		 * The value is treated as an unsigned byte.
		 */
		private int getByteHalf(int value, boolean largeHalf) {
			value = value & 0xFF;
			if (largeHalf) {
				value = value >> 4;
			}
			value = value & 0xF;
			return value;
		}
	}
	
}
