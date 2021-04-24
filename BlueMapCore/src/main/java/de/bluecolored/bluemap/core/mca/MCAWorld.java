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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MCAWorld implements World {

	private static final Grid CHUNK_GRID = new Grid(16);
	private static final Grid REGION_GRID = new Grid(32).multiply(CHUNK_GRID);

	private final UUID uuid;
	private final Path worldFolder;
	private final MinecraftVersion minecraftVersion;
	private String name;
	private Vector3i spawnPoint;

	private final LoadingCache<Vector2i, MCARegion> regionCache;
	private final LoadingCache<Vector2i, MCAChunk> chunkCache;

	private BlockIdMapper blockIdMapper;
	private BlockPropertiesMapper blockPropertiesMapper;
	private BiomeMapper biomeMapper;

	private final Multimap<String, BlockStateExtension> blockStateExtensions;
	
	private boolean ignoreMissingLightData;
	
	private final Map<Integer, String> forgeBlockMappings;
	
	private MCAWorld(
			Path worldFolder, 
			UUID uuid, 
			MinecraftVersion minecraftVersion,
			String name,
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
		
		this.regionCache = Caffeine.newBuilder()
				.executor(BlueMap.THREAD_POOL)
				.maximumSize(100)
				.expireAfterWrite(1, TimeUnit.MINUTES)
				.build(this::loadRegion);

		this.chunkCache = Caffeine.newBuilder()
				.executor(BlueMap.THREAD_POOL)
				.maximumSize(500)
				.expireAfterWrite(1, TimeUnit.MINUTES)
				.build(this::loadChunk);
	}

	public BlockState getBlockState(Vector3i pos) {
		return getChunk(blockToChunk(pos)).getBlockState(pos);
	}
	
	@Override
	public Biome getBiome(int x, int y, int z) {
		if (y < getMinY()) {
			y = getMinY();
		} else if (y > getMaxY()) {
			y = getMaxY();
		}

		MCAChunk chunk = getChunk(x >> 4, z >> 4);
		return chunk.getBiome(x, y, z);
	}
	
	@Override
	public Block getBlock(Vector3i pos) {
		if (pos.getY() < getMinY()) {
			return new Block(this, BlockState.AIR, LightData.ZERO, Biome.DEFAULT, BlockProperties.TRANSPARENT, pos);
		} else if (pos.getY() > getMaxY()) {
			return new Block(this, BlockState.AIR, LightData.SKY, Biome.DEFAULT, BlockProperties.TRANSPARENT, pos);
		}

		MCAChunk chunk = getChunk(blockToChunk(pos));
		BlockState blockState = getExtendedBlockState(chunk, pos);
		LightData lightData = chunk.getLightData(pos);
		Biome biome = chunk.getBiome(pos.getX(), pos.getY(), pos.getZ());
		BlockProperties properties = blockPropertiesMapper.get(blockState);
		return new Block(this, blockState, lightData, biome, properties, pos);
	}

	private BlockState getExtendedBlockState(MCAChunk chunk, Vector3i pos) {
		BlockState blockState = chunk.getBlockState(pos);
		
		if (chunk instanceof ChunkAnvil112) { // only use extensions if old format chunk (1.12) in the new format block-states are saved with extensions
			for (BlockStateExtension ext : blockStateExtensions.get(blockState.getFullId())) {
				blockState = ext.extend(this, pos, blockState);
			}
		}
		
		return blockState;
	}

	@Override
	public MCAChunk getChunk(int x, int z) {
		return getChunk(new Vector2i(x, z));
	}

	public MCAChunk getChunk(Vector2i pos) {
		return chunkCache.get(pos);
	}

	@Override
	public MCARegion getRegion(int x, int z) {
		return regionCache.get(new Vector2i(x, z));
	}

	@Override
	public Collection<Vector2i> listRegions() {
		File[] regionFiles = getRegionFolder().toFile().listFiles();
		if (regionFiles == null) return Collections.emptyList();

		List<Vector2i> regions = new ArrayList<>(regionFiles.length);

		for (File file : regionFiles) {
			if (!file.getName().endsWith(".mca")) continue;
			if (file.length() <= 0) continue;

			try {
				String[] filenameParts = file.getName().split("\\.");
				int rX = Integer.parseInt(filenameParts[1]);
				int rZ = Integer.parseInt(filenameParts[2]);

				regions.add(new Vector2i(rX, rZ));
			} catch (NumberFormatException ignore) {}
		}

		return regions;
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
		return 63;
	}

	@Override
	public int getMinY() {
		return 0;
	}

	@Override
	public int getMaxY() {
		return 255;
	}

	@Override
	public Grid getChunkGrid() {
		return CHUNK_GRID;
	}

	@Override
	public Grid getRegionGrid() {
		return REGION_GRID;
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
	public void invalidateChunkCache(int x, int z) {
		chunkCache.invalidate(new Vector2i(x, z));
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
	
	private File getMCAFile(int regionX, int regionZ) {
		return getRegionFolder().resolve("r." + regionX + "." + regionZ + ".mca").toFile();
	}

	private void registerBlockStateExtension(BlockStateExtension extension) {
		for (String id : extension.getAffectedBlockIds()) {
			this.blockStateExtensions.put(id, extension);
		}
	}

	private MCARegion loadRegion(Vector2i regionPos) {
		return loadRegion(regionPos.getX(), regionPos.getY());
	}

	private MCARegion loadRegion(int x, int z) {
		File regionPath = getMCAFile(x, z);
		return new MCARegion(this, regionPath);
	}

	private MCAChunk loadChunk(Vector2i chunkPos) {
		return loadChunk(chunkPos.getX(), chunkPos.getY());
	}

	private MCAChunk loadChunk(int x, int z) {
		final int tries = 3;
		final int tryInterval = 1000;

		Exception loadException = null;
		for (int i = 0; i < tries; i++) {
			try {
				return getRegion(x >> 5, z >> 5)
						.loadChunk(x, z, ignoreMissingLightData);
			} catch (IOException | RuntimeException e) {
				if (loadException != null) e.addSuppressed(loadException);
				loadException = e;

				if (i + 1 < tries) {
					try {
						Thread.sleep(tryInterval);
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}

		Logger.global.logDebug("Unexpected exception trying to load chunk (x:" + x + ", z:" + z + "):" + loadException);
		return MCAChunk.empty();
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
				pos.getX() >> 4,
				pos.getZ() >> 4
			);
	}
	
}
