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
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.Grid;
import de.bluecolored.bluemap.core.world.World;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.NBTUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MCAWorld implements World {

	private static final Grid CHUNK_GRID = new Grid(16);
	private static final Grid REGION_GRID = new Grid(32).multiply(CHUNK_GRID);

	@DebugDump private final UUID uuid;
	@DebugDump private final Path worldFolder;
	@DebugDump private final String name;
	@DebugDump private final Vector3i spawnPoint;

	@DebugDump private final boolean ignoreMissingLightData;

	private final LoadingCache<Vector2i, MCARegion> regionCache;
	private final LoadingCache<Vector2i, MCAChunk> chunkCache;
	
	private MCAWorld(
			Path worldFolder, 
			UUID uuid,
			String name,
			Vector3i spawnPoint,
			boolean ignoreMissingLightData
			) {
		this.uuid = uuid;
		this.worldFolder = worldFolder;
		this.name = name;
		this.spawnPoint = spawnPoint;
		
		this.ignoreMissingLightData = ignoreMissingLightData;

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
		return getChunk(pos.getX() >> 4, pos.getZ() >> 4).getBlockState(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public MCAChunk getChunkAtBlock(int x, int y, int z) {
		return getChunk(new Vector2i(x >> 4, z >> 4));
	}

	@Override
	public MCAChunk getChunk(int x, int z) {
		return getChunk(vec2i(x, z));
	}

	private MCAChunk getChunk(Vector2i pos) {
		return chunkCache.get(pos);
	}

	@Override
	public MCARegion getRegion(int x, int z) {
		return getRegion(vec2i(x, z));
	}

	private MCARegion getRegion(Vector2i pos) {
		return regionCache.get(pos);
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
	public int getMinY(int x, int z) {
		return getChunk(x >> 4, z >> 4).getMinY(x, z);
	}

	@Override
	public int getMaxY(int x, int z) {
		return getChunk(x >> 4, z >> 4).getMaxY(x, z);
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

	public Path getWorldFolder() {
		return worldFolder;
	}
	
	private Path getRegionFolder() {
		return worldFolder.resolve("region");
	}
	
	private File getMCAFile(int regionX, int regionZ) {
		return getRegionFolder().resolve("r." + regionX + "." + regionZ + ".mca").toFile();
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

	public static MCAWorld load(Path worldFolder, UUID uuid) throws IOException {
		return load(worldFolder, uuid, null, false);
	}
	
	public static MCAWorld load(Path worldFolder, UUID uuid, String name, boolean ignoreMissingLightData) throws IOException {
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

			return new MCAWorld(
					worldFolder,
					uuid,
					name,
					spawnPoint,
					ignoreMissingLightData
			);
		} catch (ClassCastException | NullPointerException ex) {
			throw new IOException("Invaid level.dat format!", ex);
		}
	}

	@Override
	public String toString() {
		return "MCAWorld{" +
			   "uuid=" + uuid +
			   ", worldFolder=" + worldFolder +
			   ", name='" + name + '\'' +
			   '}';
	}

	private static final int VEC_2I_CACHE_SIZE = 0x4000;
	private static final int VEC_2I_CACHE_MASK = VEC_2I_CACHE_SIZE - 1;
	private static final Vector2i[] VEC_2I_CACHE = new Vector2i[VEC_2I_CACHE_SIZE];
	private static Vector2i vec2i(int x, int y) {
		int cacheIndex = (x * 1456 ^ y * 948375892) & VEC_2I_CACHE_MASK;
		Vector2i possibleMatch = VEC_2I_CACHE[cacheIndex];

		if (possibleMatch != null && possibleMatch.getX() == x && possibleMatch.getY() == y)
			return possibleMatch;

		return VEC_2I_CACHE[cacheIndex] = new Vector2i(x, y);
	}

}
