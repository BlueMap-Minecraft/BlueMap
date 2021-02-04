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

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.extensions.*;
import de.bluecolored.bluemap.core.mca.mapping.BiomeMapper;
import de.bluecolored.bluemap.core.mca.mapping.BlockIdMapper;
import de.bluecolored.bluemap.core.mca.mapping.BlockPropertiesMapper;
import de.bluecolored.bluemap.core.world.*;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.NBTUtil;
import net.querz.nbt.Tag;
import net.querz.nbt.mca.CompressionType;
import net.querz.nbt.mca.MCAUtil;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class MCAWorld implements World {

	private final UUID uuid;
	private final Path worldFolder;
	private final MinecraftVersion minecraftVersion;
	private String name;
	private int seaLevel;
	private Vector3i spawnPoint;

	private final LoadingCache<Vector2i, Chunk> chunkCache;
	
	private BlockIdMapper blockIdMapper;
	private BlockPropertiesMapper blockPropertiesMapper;
	private BiomeMapper biomeMapper;

	private final Multimap<String, BlockStateExtension> blockStateExtensions;
	
	private boolean ignoreMissingLightData;
	
	private Map<Integer, String> forgeBlockMappings;
	
	private MCAWorld(
			Path worldFolder, 
			UUID uuid, 
			MinecraftVersion minecraftVersion,
			String name, 
			int worldHeight, 
			int seaLevel, 
			Vector3i spawnPoint, 
			BlockIdMapper blockIdMapper,
			BlockPropertiesMapper blockPropertiesMapper, 
			BiomeMapper biomeMapper,
			boolean ignoreMissingLightData
			) {
		this.uuid = uuid;
		this.worldFolder = worldFolder;
		this.minecraftVersion = minecraftVersion;
		this.name = name;
		this.seaLevel = seaLevel;
		this.spawnPoint = spawnPoint;
		
		this.blockIdMapper = blockIdMapper;
		this.blockPropertiesMapper = blockPropertiesMapper;
		this.biomeMapper = biomeMapper;
		
		this.ignoreMissingLightData = ignoreMissingLightData;
		
		this.forgeBlockMappings = new HashMap<>();
		
		this.blockStateExtensions = MultimapBuilder.hashKeys().arrayListValues().build();
		registerBlockStateExtension(new SnowyExtension(minecraftVersion));
		registerBlockStateExtension(new StairShapeExtension());
		registerBlockStateExtension(new FireExtension());
		registerBlockStateExtension(new RedstoneExtension());
		registerBlockStateExtension(new DoorExtension(minecraftVersion));
		registerBlockStateExtension(new NetherFenceConnectExtension());
		registerBlockStateExtension(new TripwireConnectExtension());
		registerBlockStateExtension(new WallConnectExtension());
		registerBlockStateExtension(new WoodenFenceConnectExtension(minecraftVersion));
		registerBlockStateExtension(new GlassPaneConnectExtension());
		registerBlockStateExtension(new DoublePlantExtension(minecraftVersion));
		registerBlockStateExtension(new DoubleChestExtension());
		
		this.chunkCache = Caffeine.newBuilder()
			.executor(BlueMap.THREAD_POOL)
			.maximumSize(500)
			.expireAfterWrite(1, TimeUnit.MINUTES)
			.build(chunkPos -> this.loadChunkOrEmpty(chunkPos, 2, 1000));
	}
	
	public BlockState getBlockState(Vector3i pos) {
		Vector2i chunkPos = blockToChunk(pos);
		Chunk chunk = getChunk(chunkPos);
		return chunk.getBlockState(pos);
	}
	
	@Override
	public Biome getBiome(Vector3i pos) {
		if (pos.getY() < getMinY()) {
			pos = new Vector3i(pos.getX(), getMinY(), pos.getZ());
		} else if (pos.getY() > getMaxY()) {
			pos = new Vector3i(pos.getX(), getMaxY(), pos.getZ());
		}
		
		Vector2i chunkPos = blockToChunk(pos);
		Chunk chunk = getChunk(chunkPos);
		return chunk.getBiome(pos);
	}
	
	@Override
	public Block getBlock(Vector3i pos) {
		if (pos.getY() < getMinY()) {
			return new Block(this, BlockState.AIR, LightData.ZERO, Biome.DEFAULT, BlockProperties.TRANSPARENT, pos);
		} else if (pos.getY() > getMaxY()) {
			return new Block(this, BlockState.AIR, LightData.SKY, Biome.DEFAULT, BlockProperties.TRANSPARENT, pos);
		}
		
		Vector2i chunkPos = blockToChunk(pos);
		Chunk chunk = getChunk(chunkPos);
		BlockState blockState = getExtendedBlockState(chunk, pos);
		LightData lightData = chunk.getLightData(pos);
		Biome biome = chunk.getBiome(pos);
		BlockProperties properties = blockPropertiesMapper.get(blockState);
		return new Block(this, blockState, lightData, biome, properties, pos);
	}

	private BlockState getExtendedBlockState(Chunk chunk, Vector3i pos) {
		BlockState blockState = chunk.getBlockState(pos);
		
		if (chunk instanceof ChunkAnvil112) { // only use extensions if old format chunk (1.12) in the new format block-states are saved with extensions
			for (BlockStateExtension ext : blockStateExtensions.get(blockState.getFullId())) {
				blockState = ext.extend(this, pos, blockState);
			}
		}
		
		return blockState;
	}
	
	public Chunk getChunk(Vector2i chunkPos) {
		try {
			Chunk chunk = chunkCache.get(chunkPos);
			return chunk;
		} catch (RuntimeException e) {
			if (e.getCause() instanceof InterruptedException) Thread.currentThread().interrupt();
			throw e;
		}
	}
	
	private Chunk loadChunkOrEmpty(Vector2i chunkPos, int tries, long tryInterval) {
		Exception loadException = null;
		for (int i = 0; i < tries; i++) {
			try {
				return loadChunk(chunkPos);
			} catch (IOException | RuntimeException e) {
				if (loadException != null) e.addSuppressed(loadException);
				loadException = e;
				
				if (tryInterval > 0 && i+1 < tries) {
					try {
						Thread.sleep(tryInterval);
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}

		Logger.global.logDebug("Unexpected exception trying to load chunk (" + chunkPos + "):" + loadException);
		return Chunk.empty(this, chunkPos);
	}
	
	private Chunk loadChunk(Vector2i chunkPos) throws IOException {
		Vector2i regionPos = chunkToRegion(chunkPos);
		Path regionPath = getMCAFilePath(regionPos);
		
		File regionFile = regionPath.toFile();
		if (!regionFile.exists() || regionFile.length() <= 0) return Chunk.empty(this, chunkPos);
		
		try (RandomAccessFile raf = new RandomAccessFile(regionFile, "r")) {
		
			int xzChunk = Math.floorMod(chunkPos.getY(), 32) * 32 + Math.floorMod(chunkPos.getX(), 32);
			
			raf.seek(xzChunk * 4);
			int offset = raf.read() << 16;
			offset |= (raf.read() & 0xFF) << 8;
			offset |= raf.read() & 0xFF;
			offset *= 4096;
			
			int size = raf.readByte() * 4096;
			if (size == 0) {
				return Chunk.empty(this, chunkPos);
			}
			
			raf.seek(offset + 4); // +4 skip chunk size
			
			byte compressionTypeByte = raf.readByte();
			CompressionType compressionType = CompressionType.getFromID(compressionTypeByte);
			if (compressionType == null) {
				throw new IOException("Invalid compression type " + compressionTypeByte);
			}
			
			DataInputStream dis = new DataInputStream(new BufferedInputStream(compressionType.decompress(new FileInputStream(raf.getFD()))));
			Tag<?> tag = Tag.deserialize(dis, Tag.DEFAULT_MAX_DEPTH);
			if (tag instanceof CompoundTag) {
				return Chunk.create(this, (CompoundTag) tag, ignoreMissingLightData);
			} else {
				throw new IOException("Invalid data tag: " + (tag == null ? "null" : tag.getClass().getName()));
			}
			
		} catch (RuntimeException e) {
			throw new IOException(e);
		}
	}
	
	@Override
	public boolean isChunkGenerated(Vector2i chunkPos) {
		Chunk chunk = getChunk(chunkPos);
		return chunk.isGenerated();
	}
	
	@Override
	public Collection<Vector2i> getChunkList(long modifiedSinceMillis, Predicate<Vector2i> filter){
		List<Vector2i> chunks = new ArrayList<>(10000);
		
		if (!getRegionFolder().toFile().isDirectory()) return Collections.emptyList();
		
		for (File file : getRegionFolder().toFile().listFiles()) {
			if (!file.getName().endsWith(".mca")) continue;
			if (file.length() <= 0) continue;
			
			try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {

				String[] filenameParts = file.getName().split("\\.");
				int rX = Integer.parseInt(filenameParts[1]);
				int rZ = Integer.parseInt(filenameParts[2]);
				
				for (int x = 0; x < 32; x++) {
					for (int z = 0; z < 32; z++) {
						Vector2i chunk = new Vector2i(rX * 32 + x, rZ * 32 + z);
						if (filter.test(chunk)) {
							
							int xzChunk = z * 32 + x;
							
							raf.seek(xzChunk * 4 + 3);
							int size = raf.readByte() * 4096;
	
							if (size == 0) continue;
							
							raf.seek(xzChunk * 4 + 4096);
							int timestamp = raf.read() << 24;
							timestamp |= (raf.read() & 0xFF) << 16;
							timestamp |= (raf.read() & 0xFF) << 8;
							timestamp |= raf.read() & 0xFF;
							
							if (timestamp >= (modifiedSinceMillis / 1000)) {
								chunks.add(chunk);
							}
							
						}
					}
				}
			} catch (RuntimeException | IOException ex) {
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
	public Path getSaveFolder() {
		return worldFolder;
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
		chunkCache.invalidateAll();
	}
	
	@Override
	public void invalidateChunkCache(Vector2i chunk) {
		chunkCache.invalidate(chunk);
	}
	
	@Override
	public void cleanUpChunkCache() {
		chunkCache.cleanUp();
	}
	
	public BlockIdMapper getBlockIdMapper() {
		return blockIdMapper;
	}
	
	public BlockPropertiesMapper getBlockPropertiesMapper() {
		return blockPropertiesMapper;
	}
	
	public BiomeMapper getBiomeIdMapper() {
		return biomeMapper;
	}
	
	public void setBlockIdMapper(BlockIdMapper blockIdMapper) {
		this.blockIdMapper = blockIdMapper;
	}

	public void setBlockPropertiesMapper(BlockPropertiesMapper blockPropertiesMapper) {
		this.blockPropertiesMapper = blockPropertiesMapper;
	}

	public void setBiomeMapper(BiomeMapper biomeMapper) {
		this.biomeMapper = biomeMapper;
	}

	public Path getWorldFolder() {
		return worldFolder;
	}

	public String getForgeBlockIdMapping(int id) {
		return forgeBlockMappings.get(id);
	}
	
	public MinecraftVersion getMinecraftVersion() {
		return minecraftVersion;
	}
	
	private Path getRegionFolder() {
		return worldFolder.resolve("region");
	}
	
	private Path getMCAFilePath(Vector2i region) {
		return getRegionFolder().resolve(MCAUtil.createNameFromRegionLocation(region.getX(), region.getY()));
	}

	private void registerBlockStateExtension(BlockStateExtension extension) {
		for (String id : extension.getAffectedBlockIds()) {
			this.blockStateExtensions.put(id, extension);
		}
	}
	
	public static MCAWorld load(Path worldFolder, UUID uuid, MinecraftVersion version, BlockIdMapper blockIdMapper, BlockPropertiesMapper blockPropertiesMapper, BiomeMapper biomeIdMapper) throws IOException {
		return load(worldFolder, uuid, version, blockIdMapper, blockPropertiesMapper, biomeIdMapper, null, false);
	}
	
	public static MCAWorld load(Path worldFolder, UUID uuid, MinecraftVersion version, BlockIdMapper blockIdMapper, BlockPropertiesMapper blockPropertiesMapper, BiomeMapper biomeIdMapper, String name, boolean ignoreMissingLightData) throws IOException {
		try {
			StringBuilder subDimensionName = new StringBuilder();

			File levelFolder = worldFolder.toFile();
			File levelFile = new File(levelFolder, "level.dat");
			int searchDepth = 0;

			while (!levelFile.exists() && searchDepth < 4) {
				searchDepth++;
				subDimensionName.insert(0, "/").insert(1, levelFolder.getName());
				levelFolder = levelFolder.getParentFile();
				if (levelFolder == null) break;

				levelFile = new File(levelFolder, "level.dat");
			}

			if (!levelFile.exists()) {
				throw new FileNotFoundException("Could not find a level.dat file for this world!");
			}
			
			CompoundTag level = (CompoundTag) NBTUtil.readTag(levelFile);
			CompoundTag levelData = level.getCompoundTag("Data");
			
			if (name == null) {
				name = levelData.getString("LevelName") + subDimensionName;
			}
			
			int worldHeight = 255;
			int seaLevel = 63;
			Vector3i spawnPoint = new Vector3i(
					levelData.getInt("SpawnX"),
					levelData.getInt("SpawnY"),
					levelData.getInt("SpawnZ")
					);
			
			MCAWorld world = new MCAWorld(
					worldFolder, 
					uuid, 
					version,
					name, 
					worldHeight, 
					seaLevel, 
					spawnPoint,
					blockIdMapper,
					blockPropertiesMapper,
					biomeIdMapper,
					ignoreMissingLightData
					);
			
			try {
				CompoundTag fmlTag = level.getCompoundTag("FML");
				if (fmlTag == null) fmlTag = level.getCompoundTag("fml");
				
				ListTag<? extends Tag<?>> blockIdReg = fmlTag.getCompoundTag("Registries").getCompoundTag("minecraft:blocks").getListTag("ids");
				for (Tag<?> tag : blockIdReg) {
					if (tag instanceof CompoundTag) {
						CompoundTag entry = (CompoundTag) tag;
						String blockId = entry.getString("K");
						int numeralId = entry.getInt("V");
						
						world.forgeBlockMappings.put(numeralId, blockId);
					}
				}
			} catch (NullPointerException ignore) {}
			
			return world;
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
	
}
