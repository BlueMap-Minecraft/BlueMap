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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.extensions.BlockStateExtension;
import de.bluecolored.bluemap.core.mca.extensions.DoorExtension;
import de.bluecolored.bluemap.core.mca.extensions.DoublePlantExtension;
import de.bluecolored.bluemap.core.mca.extensions.FireExtension;
import de.bluecolored.bluemap.core.mca.extensions.GlassPaneConnectExtension;
import de.bluecolored.bluemap.core.mca.extensions.NetherFenceConnectExtension;
import de.bluecolored.bluemap.core.mca.extensions.RedstoneExtension;
import de.bluecolored.bluemap.core.mca.extensions.SnowyExtension;
import de.bluecolored.bluemap.core.mca.extensions.StairShapeExtension;
import de.bluecolored.bluemap.core.mca.extensions.TripwireConnectExtension;
import de.bluecolored.bluemap.core.mca.extensions.WallConnectExtension;
import de.bluecolored.bluemap.core.mca.extensions.WoodenFenceConnectExtension;
import de.bluecolored.bluemap.core.mca.mapping.BiomeIdMapper;
import de.bluecolored.bluemap.core.mca.mapping.BlockIdMapper;
import de.bluecolored.bluemap.core.mca.mapping.BlockProperties;
import de.bluecolored.bluemap.core.mca.mapping.BlockPropertyMapper;
import de.bluecolored.bluemap.core.util.AABB;
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.ChunkNotGeneratedException;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.WorldChunk;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.NBTUtil;
import net.querz.nbt.Tag;
import net.querz.nbt.mca.CompressionType;
import net.querz.nbt.mca.MCAUtil;

public class MCAWorld implements World {

	private static final Cache<WorldChunkHash, Chunk> CHUNK_CACHE = CacheBuilder.newBuilder().maximumSize(500).build();	
	private static final Multimap<String, BlockStateExtension> BLOCK_STATE_EXTENSIONS = MultimapBuilder.hashKeys().arrayListValues().build();

	public static final BlockIdMapper DEFAULT_BLOCK_ID_MAPPER;
	public static final BlockPropertyMapper DEFAULT_BLOCK_PROPERTY_MAPPER;
	public static final BiomeIdMapper DEFAULT_BIOME_ID_MAPPER;
	
	static {
		try {
			DEFAULT_BLOCK_ID_MAPPER = BlockIdMapper.create();
			DEFAULT_BLOCK_PROPERTY_MAPPER = BlockPropertyMapper.create();
			DEFAULT_BIOME_ID_MAPPER = BiomeIdMapper.create();
		} catch (IOException e) {
			throw new RuntimeException("Failed to load essential resources!", e);
		}
		
		registerBlockStateExtension(new SnowyExtension());
		registerBlockStateExtension(new StairShapeExtension());
		registerBlockStateExtension(new FireExtension());
		registerBlockStateExtension(new RedstoneExtension());
		registerBlockStateExtension(new DoorExtension());
		registerBlockStateExtension(new NetherFenceConnectExtension());
		registerBlockStateExtension(new TripwireConnectExtension());
		registerBlockStateExtension(new WallConnectExtension());
		registerBlockStateExtension(new WoodenFenceConnectExtension());
		registerBlockStateExtension(new GlassPaneConnectExtension());
		registerBlockStateExtension(new DoublePlantExtension());
	}

	private final UUID uuid;
	private final Path worldFolder;
	private String name;
	private AABB boundaries;
	private int seaLevel;
	private Vector3i spawnPoint;

	private BlockIdMapper blockIdMapper;
	private BlockPropertyMapper blockPropertyMapper;
	private BiomeIdMapper biomeIdMapper;
	
	private MCAWorld(
			Path worldFolder, 
			UUID uuid, 
			String name, 
			int worldHeight, 
			int seaLevel, 
			Vector3i spawnPoint, 
			BlockIdMapper blockIdMapper,
			BlockPropertyMapper blockPropertyMapper, 
			BiomeIdMapper biomeIdMapper
			) {
		this.uuid = uuid;
		this.worldFolder = worldFolder;
		this.name = name;
		this.boundaries = new AABB(new Vector3i(-10000000, 0, -10000000), new Vector3i(10000000, worldHeight, 10000000));
		this.seaLevel = seaLevel;
		this.spawnPoint = spawnPoint;
		
		this.blockIdMapper = blockIdMapper;
		this.blockPropertyMapper = blockPropertyMapper;
		this.biomeIdMapper = biomeIdMapper;
	}

	public BlockState getBlockState(Vector3i pos) {
		try {
			
			Vector2i chunkPos = blockToChunk(pos);
			Chunk chunk = getChunk(chunkPos);
			return chunk.getBlockState(pos);
			
		} catch (Exception ex) {
			return BlockState.AIR;
		}
	}
	
	@Override
	public Block getBlock(Vector3i pos) throws ChunkNotGeneratedException {
		try {
			
			Vector2i chunkPos = blockToChunk(pos);
			Chunk chunk = getChunk(chunkPos);
			BlockState blockState = getExtendedBlockState(chunk, pos);
			LightData lightData = chunk.getLightData(pos);
			Biome biome = chunk.getBiome(pos);
			BlockProperties properties = blockPropertyMapper.map(blockState);
			return new MCABlock(this, blockState, lightData, biome, properties, pos);
			
		} catch (IOException ex) {
			throw new ChunkNotGeneratedException(ex); // to resolve the error, act like the chunk has not been generated yet
		}
	}

	private BlockState getExtendedBlockState(Chunk chunk, Vector3i pos) throws ChunkNotGeneratedException {
		BlockState blockState = chunk.getBlockState(pos);
		
		if (chunk instanceof ChunkAnvil112) { // only use extensions if old format chunk (1.12) in the new format block-states are saved with extensions
			for (BlockStateExtension ext : BLOCK_STATE_EXTENSIONS.get(blockState.getFullId())) {
				blockState = ext.extend(this, pos, blockState);
			}
		}
		
		return blockState;
	}

	@Override
	public AABB getBoundaries() {
		return boundaries;
	}

	@Override
	public WorldChunk getWorldChunk(AABB boundaries) {
		return new MCAWorldChunk(this, boundaries);
	}
	
	public Chunk getChunk(Vector2i chunkPos) throws IOException, ChunkNotGeneratedException {
		try {
			Chunk chunk = CHUNK_CACHE.get(new WorldChunkHash(this, chunkPos), () -> this.loadChunk(chunkPos));
			if (!chunk.isGenerated()) throw new ChunkNotGeneratedException();
			return chunk;
		} catch (UncheckedExecutionException | ExecutionException e) {
			Throwable cause = e.getCause();
			
			if (cause instanceof IOException) {
				throw (IOException) cause;
			}

			else if (cause instanceof ChunkNotGeneratedException) {
				throw (ChunkNotGeneratedException) cause;
			}
			
			else throw new IOException(cause);
		}
	}
	
	private Chunk loadChunk(Vector2i chunkPos) throws IOException, ChunkNotGeneratedException {
		Vector2i regionPos = chunkToRegion(chunkPos);
		Path regionPath = getMCAFilePath(regionPos);
		
		try (RandomAccessFile raf = new RandomAccessFile(regionPath.toFile(), "r")) {
		
			int xzChunk = Math.floorMod(chunkPos.getY(), 32) * 32 + Math.floorMod(chunkPos.getX(), 32);
			
			raf.seek(xzChunk * 4);
			int offset = raf.read() << 16;
			offset |= (raf.read() & 0xFF) << 8;
			offset |= raf.read() & 0xFF;
			offset *= 4096;
			
			int size = raf.readByte() * 4096;
			if (size == 0) throw new ChunkNotGeneratedException();
			
			raf.seek(offset + 4); // +4 skip chunk size
			
			byte compressionTypeByte = raf.readByte();
			CompressionType compressionType = CompressionType.getFromID(compressionTypeByte);
			if (compressionType == null) {
				throw new IOException("invalid compression type " + compressionTypeByte);
			}
			
			DataInputStream dis = new DataInputStream(new BufferedInputStream(compressionType.decompress(new FileInputStream(raf.getFD()))));
			Tag<?> tag = Tag.deserialize(dis, Tag.DEFAULT_MAX_DEPTH);
			if (tag instanceof CompoundTag) {
				return Chunk.create(this, (CompoundTag) tag);
			} else {
				throw new IOException("invalid data tag: " + (tag == null ? "null" : tag.getClass().getName()));
			}
			
		}
	}
	
	public boolean isChunkGenerated(Vector2i chunkPos) {
		try {
			getChunk(chunkPos);
		} catch (ChunkNotGeneratedException | IOException e) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public Collection<Vector2i> getChunkList(long modifiedSinceMillis){
		List<Vector2i> chunks = new ArrayList<>(10000);
		
		if (!getRegionFolder().toFile().isDirectory()) return Collections.emptyList();
		
		for (File file : getRegionFolder().toFile().listFiles()) {
			if (!file.getName().endsWith(".mca")) continue;
			
			try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {

				String[] filenameParts = file.getName().split("\\.");
				int rX = Integer.parseInt(filenameParts[1]);
				int rZ = Integer.parseInt(filenameParts[2]);
				
				for (int x = 0; x < 32; x++) {
					for (int z = 0; z < 32; z++) {
						int xzChunk = z * 32 + x;
						
						raf.seek(xzChunk * 4 + 3);
						int size = raf.readByte() * 4096;

						if (size == 0) continue;
						
						raf.seek(xzChunk * 4 + 4096);
						int timestamp = raf.read() << 24;
						timestamp |= (raf.read() & 0xFF) << 16;
						timestamp |= (raf.read() & 0xFF) << 8;
						timestamp |= raf.read() & 0xFF;
						
						if (timestamp >= (modifiedSinceMillis / 1000)) chunks.add(new Vector2i(rX * 32 + x, rZ * 32 + z));
					}
				}
			} catch (Exception ex) {
				Logger.global.logWarning("Failed to read .mca file: " + file.getAbsolutePath() + " (" + ex.toString() + ")");
			}
		}
		
		return chunks;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public UUID getUUID() {
		return uuid;
	}

	@Override
	public int getSeaLevel() {
		return seaLevel;
	}

	@Override
	public Vector3i getSpawnPoint() {
		return spawnPoint;
	}
	
	@Override
	public void invalidateChunkCache() {
		CHUNK_CACHE.invalidateAll();
	}
	
	@Override
	public void invalidateChunkCache(Vector2i chunk) {
		CHUNK_CACHE.invalidate(new WorldChunkHash(this, chunk));
	}
	
	public BlockIdMapper getBlockIdMapper() {
		return blockIdMapper;
	}
	
	public BlockPropertyMapper getBlockPropertyMapper() {
		return blockPropertyMapper;
	}
	
	public BiomeIdMapper getBiomeIdMapper() {
		return biomeIdMapper;
	}
	
	public void setBlockIdMapper(BlockIdMapper blockIdMapper) {
		this.blockIdMapper = blockIdMapper;
	}

	public void setBlockPropertyMapper(BlockPropertyMapper blockPropertyMapper) {
		this.blockPropertyMapper = blockPropertyMapper;
	}

	public void setBiomeIdMapper(BiomeIdMapper biomeIdMapper) {
		this.biomeIdMapper = biomeIdMapper;
	}

	public Path getWorldFolder() {
		return worldFolder;
	}
	
	private Path getRegionFolder() {
		return worldFolder.resolve("region");
	}
	
	private Path getMCAFilePath(Vector2i region) {
		return getRegionFolder().resolve(MCAUtil.createNameFromRegionLocation(region.getX(), region.getY()));
	}
	
	public static MCAWorld load(Path worldFolder, UUID uuid) throws IOException {
		try {
			CompoundTag level = (CompoundTag) NBTUtil.readTag(worldFolder.resolve("level.dat").toFile());
			CompoundTag levelData = level.getCompoundTag("Data");
			
			String name = levelData.getString("LevelName");
			int worldHeight = 255;
			int seaLevel = 63;
			Vector3i spawnPoint = new Vector3i(
					levelData.getInt("SpawnX"),
					levelData.getInt("SpawnY"),
					levelData.getInt("SpawnZ")
					);
			

			CHUNK_CACHE.invalidateAll();
			return new MCAWorld(
					worldFolder, 
					uuid, 
					name, 
					worldHeight, 
					seaLevel, 
					spawnPoint, 
					DEFAULT_BLOCK_ID_MAPPER,
					DEFAULT_BLOCK_PROPERTY_MAPPER, 
					DEFAULT_BIOME_ID_MAPPER
					);
		} catch (ClassCastException | NullPointerException ex) {
			throw new IOException("Invaid level.dat format!", ex);
		}
	}
	
	public static Vector2i blockToChunk(Vector3i pos) {
		return new Vector2i(
				MCAUtil.blockToChunk(pos.getX()),
				MCAUtil.blockToChunk(pos.getZ())
			);
	}
	
	public static Vector2i blockToRegion(Vector3i pos) {
		return new Vector2i(
				MCAUtil.blockToRegion(pos.getX()),
				MCAUtil.blockToRegion(pos.getZ())
			);
	}
	
	public static Vector2i chunkToRegion(Vector2i pos) {
		return new Vector2i(
				MCAUtil.chunkToRegion(pos.getX()),
				MCAUtil.chunkToRegion(pos.getY())
			);
	}
	
	public static void registerBlockStateExtension(BlockStateExtension extension) {
		for (String id : extension.getAffectedBlockIds()) {
			BLOCK_STATE_EXTENSIONS.put(id, extension);
		}
	}
	
	private static class WorldChunkHash {
		
		private final UUID world;
		private final Vector2i chunk;
		
		public WorldChunkHash(MCAWorld world, Vector2i chunk) {
			this.world = world.getUUID();
			this.chunk = chunk;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(world, chunk);
		}
		
		@Override
		public boolean equals(Object obj) {
			
			if (obj instanceof WorldChunkHash) {
				WorldChunkHash other = (WorldChunkHash) obj;
				return other.chunk.equals(chunk) && world.equals(other.world);
			}
			
			return false;
		}
		
	}
	
}
