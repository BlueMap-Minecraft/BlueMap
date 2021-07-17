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

import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.mca.mapping.BiomeMapper;
import de.bluecolored.bluemap.core.mca.mapping.BlockIdMapper;
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.LightData;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.NumberTag;

import java.util.Arrays;
import java.util.function.IntFunction;

public class ChunkAnvil112 extends MCAChunk {
	private final BiomeMapper biomeIdMapper;
	private final BlockIdMapper blockIdMapper;
	private final IntFunction<String> forgeBlockIdMapper;
	
	private boolean isGenerated;
	private boolean hasLight;
	private Section[] sections;
	private byte[] biomes;
	
	@SuppressWarnings("unchecked")
	public ChunkAnvil112(CompoundTag chunkTag, boolean ignoreMissingLightData, BiomeMapper biomeIdMapper, BlockIdMapper blockIdMapper, IntFunction<String> forgeBlockIdMapper) {
		super(chunkTag);
		
		this.blockIdMapper = blockIdMapper;
		this.biomeIdMapper = biomeIdMapper;
		this.forgeBlockIdMapper = forgeBlockIdMapper;
		
		CompoundTag levelData = chunkTag.getCompoundTag("Level");
		
		hasLight = levelData.getBoolean("LightPopulated");
		
		isGenerated = 
				(hasLight || ignoreMissingLightData) &&
				levelData.getBoolean("TerrainPopulated");
		
		sections = new Section[32]; //32 supports a max world-height of 512 which is the max that the hightmaps of Minecraft V1.13+ can store with 9 bits, i believe?
		if (levelData.containsKey("Sections")) {
			for (CompoundTag sectionTag : ((ListTag<CompoundTag>) levelData.getListTag("Sections"))) {
				Section section = new Section(sectionTag);
				if (section.getSectionY() >= 0 && section.getSectionY() < sections.length) sections[section.getSectionY()] = section;
			}
		}
		
		biomes = levelData.getByteArray("Biomes");
		
		if (biomes == null || biomes.length == 0) {
			biomes = new byte[256];
		}
		
		if (biomes.length < 256) {
			biomes = Arrays.copyOf(biomes, 256);
		}
	}

	@Override
	public boolean isGenerated() {
		return isGenerated;
	}

	@Override
	public BlockState getBlockState(int x, int y, int z) {
		int sectionY = y >> 4;
		if (sectionY < 0 || sectionY >= this.sections.length) return BlockState.AIR;
		
		Section section = this.sections[sectionY];
		if (section == null) return BlockState.AIR;
		
		return section.getBlockState(x, y, z);
	}
	
	public String getBlockIdMeta(Vector3i pos) {
		int sectionY = pos.getY() >> 4;
		if (sectionY < 0 || sectionY >= this.sections.length) return "0:0";
		
		Section section = this.sections[sectionY];
		if (section == null) return "0:0";
		
		return section.getBlockIdMeta(pos);
	}
	
	@Override
	public LightData getLightData(int x, int y, int z, LightData target) {
		if (!hasLight) return target.set(15, 0);

		int sectionY = y >> 4;
		if (sectionY < 0 || sectionY >= this.sections.length)
			return (y < 0) ? target.set(0, 0) : target.set(15, 0);
		
		Section section = this.sections[sectionY];
		if (section == null) return target.set(15, 0);
		
		return section.getLightData(x, y, z, target);
	}

	@Override
	public Biome getBiome(int x, int y, int z) {
		x = x & 0xF; // Math.floorMod(pos.getX(), 16)
		z = z & 0xF;
		int biomeByteIndex = z * 16 + x;

		if (biomeByteIndex >= this.biomes.length) return Biome.DEFAULT;

		return biomeIdMapper.get(biomes[biomeByteIndex] & 0xFF);
	}

	private class Section {
		private int sectionY;
		private byte[] blocks;
		private byte[] add;
		private byte[] blockLight;
		private byte[] skyLight;
		private byte[] data;
		
		public Section(CompoundTag sectionData) {
			this.sectionY = sectionData.get("Y", NumberTag.class).asInt();
			this.blocks = sectionData.getByteArray("Blocks");
			this.add = sectionData.getByteArray("Add");
			this.blockLight = sectionData.getByteArray("BlockLight");
			this.skyLight = sectionData.getByteArray("SkyLight");
			this.data = sectionData.getByteArray("Data");
			
			if (blocks.length < 4096) blocks = Arrays.copyOf(blocks, 4096);
			if (blockLight.length < 2048) blockLight = Arrays.copyOf(blockLight, 2048);
			if (skyLight.length < 2048) skyLight = Arrays.copyOf(skyLight, 2048);
			if (data.length < 2048) data = Arrays.copyOf(data, 2048);
		}

		public int getSectionY() {
			return sectionY;
		}
		
		public BlockState getBlockState(int x, int y, int z) {
			x &= 0xF; y &= 0xF; z &= 0xF; // Math.floorMod(pos.getX(), 16)

			int blockByteIndex = y * 256 + z * 16 + x;
			int blockHalfByteIndex = blockByteIndex >> 1; // blockByteIndex / 2 
			boolean largeHalf = (blockByteIndex & 0x1) != 0; // (blockByteIndex % 2) == 0
			
			int blockId = this.blocks[blockByteIndex] & 0xFF;
			
			if (this.add.length > blockHalfByteIndex) {
				blockId = blockId | (getByteHalf(this.add[blockHalfByteIndex], largeHalf) << 8);
			}
			
			int blockData = getByteHalf(this.data[blockHalfByteIndex], largeHalf);
			
			String forgeIdMapping = forgeBlockIdMapper.apply(blockId);
			if (forgeIdMapping != null) {
				return blockIdMapper.get(forgeIdMapping, blockId, blockData);
			} else {
				return blockIdMapper.get(blockId, blockData);
			}
		}
		
		public String getBlockIdMeta(Vector3i pos) {
			int x = pos.getX() & 0xF; // Math.floorMod(pos.getX(), 16)
			int y = pos.getY() & 0xF;
			int z = pos.getZ() & 0xF;
			int blockByteIndex = y * 256 + z * 16 + x;
			int blockHalfByteIndex = blockByteIndex >> 1; // blockByteIndex / 2 
			boolean largeHalf = (blockByteIndex & 0x1) != 0; // (blockByteIndex % 2) == 0
			
			int blockId = this.blocks[blockByteIndex] & 0xFF;
			
			if (this.add.length > blockHalfByteIndex) {
				blockId = blockId | (getByteHalf(this.add[blockHalfByteIndex], largeHalf) << 8);
			}
			
			int blockData = getByteHalf(this.data[blockHalfByteIndex], largeHalf);
			String forgeIdMapping = forgeBlockIdMapper.apply(blockId);
			
			return blockId + ":" + blockData + " " + forgeIdMapping;
		}
		
		public LightData getLightData(int x, int y, int z, LightData target) {
			x &= 0xF; y &= 0xF; z &= 0xF; // Math.floorMod(pos.getX(), 16)

			int blockByteIndex = y * 256 + z * 16 + x;
			int blockHalfByteIndex = blockByteIndex >> 1; // blockByteIndex / 2 
			boolean largeHalf = (blockByteIndex & 0x1) != 0; // (blockByteIndex % 2) == 0

			int blockLight = getByteHalf(this.blockLight[blockHalfByteIndex], largeHalf);
			int skyLight = getByteHalf(this.skyLight[blockHalfByteIndex], largeHalf);
			
			return target.set(skyLight, blockLight);
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
