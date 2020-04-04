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
import net.querz.nbt.ByteArrayTag;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.IntArrayTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;
import net.querz.nbt.mca.MCAUtil;

public class ChunkAnvil112 extends Chunk {
	private BlockIdMapper blockIdMapper;
	private BiomeMapper biomeIdMapper;
	
	private boolean isGenerated;
	private Section[] sections;
	private int[] biomes;
	
	@SuppressWarnings("unchecked")
	public ChunkAnvil112(MCAWorld world, CompoundTag chunkTag) {
		super(world, chunkTag);
		
		blockIdMapper = getWorld().getBlockIdMapper();
		biomeIdMapper = getWorld().getBiomeIdMapper();
		
		CompoundTag levelData = chunkTag.getCompoundTag("Level");
		
		isGenerated = 
				levelData.getBoolean("LightPopulated") &&
				levelData.getBoolean("TerrainPopulated");
		
		sections = new Section[32]; //32 supports a max world-height of 512 which is the max that the hightmaps of Minecraft V1.13+ can store with 9 bits, i believe?
		for (CompoundTag sectionTag : ((ListTag<CompoundTag>) levelData.getListTag("Sections"))) {
			Section section = new Section(sectionTag);
			sections[section.getSectionY()] = section;
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
	
	public String getBlockIdMeta(Vector3i pos) {
		int sectionY = MCAUtil.blockToChunk(pos.getY());
		
		Section section = this.sections[sectionY];
		if (section == null) return "0:0";
		
		return section.getBlockIdMeta(pos);
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
		int biomeByteIndex = z * 16 + x;
		
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
			this.sectionY = sectionData.getByte("Y");
			this.blocks = sectionData.getByteArray("Blocks");
			this.add = sectionData.getByteArray("Add");
			this.blockLight = sectionData.getByteArray("BlockLight");
			this.skyLight = sectionData.getByteArray("SkyLight");
			this.data = sectionData.getByteArray("Data");
		}

		public int getSectionY() {
			return sectionY;
		}
		
		public BlockState getBlockState(Vector3i pos) {
			int x = pos.getX() & 0xF; // Math.floorMod(pos.getX(), 16)
			int y = pos.getY() & 0xF;
			int z = pos.getZ() & 0xF;
			int blockByteIndex = y * 256 + z * 16 + x;
			int blockHalfByteIndex = blockByteIndex >> 1; // blockByteIndex / 2 
			boolean largeHalf = (blockByteIndex & 0x1) != 0; // (blockByteIndex % 2) == 0
			
			int blockId = this.blocks[blockByteIndex] & 0xFF;
			
			if (this.add.length > 0) {
				blockId = blockId | (getByteHalf(this.add[blockHalfByteIndex], largeHalf) << 8);
			}
			
			int blockData = getByteHalf(this.data[blockHalfByteIndex], largeHalf);
			
			String forgeIdMapping = getWorld().getForgeBlockIdMapping(blockId);
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
			
			if (this.add.length > 0) {
				blockId = blockId | (getByteHalf(this.add[blockHalfByteIndex], largeHalf) << 8);
			}
			
			int blockData = getByteHalf(this.data[blockHalfByteIndex], largeHalf);
			String forgeIdMapping = getWorld().getForgeBlockIdMapping(blockId);
			
			return blockId + ":" + blockData + " " + forgeIdMapping;
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
