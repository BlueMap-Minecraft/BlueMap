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
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.data.LevelData;
import de.bluecolored.bluemap.core.mca.region.RegionType;
import de.bluecolored.bluemap.core.util.Vector2iCache;
import de.bluecolored.bluemap.core.world.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

@DebugDump
public class MCAWorld implements World {

    private static final Grid CHUNK_GRID = new Grid(16);
    private static final Grid REGION_GRID = new Grid(32).multiply(CHUNK_GRID);

    private static final Vector2iCache VECTOR_2_I_CACHE = new Vector2iCache();

    private final Path worldFolder;

    private final String name;
    private final Vector3i spawnPoint;

    private final int skyLight;
    private final boolean ignoreMissingLightData;

    private final LoadingCache<Vector2i, Region> regionCache;
    private final LoadingCache<Vector2i, Chunk> chunkCache;

    public MCAWorld(Path worldFolder, int skyLight, boolean ignoreMissingLightData) throws IOException {
        this.worldFolder = worldFolder.toRealPath();
        this.skyLight = skyLight;
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

        Path levelFile = resolveLevelFile(worldFolder);
        try (InputStream in = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(levelFile)))) {
            LevelData level = MCAMath.BLUENBT.read(in, LevelData.class);
            LevelData.Data levelData = level.getData();
            this.name = levelData.getLevelName();
            this.spawnPoint = new Vector3i(
                    levelData.getSpawnX(),
                    levelData.getSpawnY(),
                    levelData.getSpawnZ()
            );
        } catch (IOException ex) {
            throw new IOException("Failed to read level.dat!", ex);
        }
    }

    @Override
    public Chunk getChunkAtBlock(int x, int y, int z) {
        return getChunk(x >> 4, z >> 4);
    }

    @Override
    public Chunk getChunk(int x, int z) {
        return getChunk(VECTOR_2_I_CACHE.get(x, z));
    }

    private Chunk getChunk(Vector2i pos) {
        return chunkCache.get(pos);
    }

    @Override
    public Region getRegion(int x, int z) {
        return getRegion(VECTOR_2_I_CACHE.get(x, z));
    }

    private Region getRegion(Vector2i pos) {
        return regionCache.get(pos);
    }

    @Override
    public Collection<Vector2i> listRegions() {
        File[] regionFiles = getRegionFolder().toFile().listFiles();
        if (regionFiles == null) return Collections.emptyList();

        List<Vector2i> regions = new ArrayList<>(regionFiles.length);

        for (File file : regionFiles) {
            if (RegionType.forFileName(file.getName()) == null) continue;
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
    public Path getSaveFolder() {
        return worldFolder;
    }

    @Override
    public int getSkyLight() {
        return skyLight;
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
        chunkCache.invalidate(VECTOR_2_I_CACHE.get(x, z));
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

    public boolean isIgnoreMissingLightData() {
        return ignoreMissingLightData;
    }

    private Region loadRegion(Vector2i regionPos) {
        return loadRegion(regionPos.getX(), regionPos.getY());
    }

    Region loadRegion(int x, int z) {
        return RegionType.loadRegion(this, getRegionFolder(), x, z);
    }

    private Chunk loadChunk(Vector2i chunkPos) {
        return loadChunk(chunkPos.getX(), chunkPos.getY());
    }

    Chunk loadChunk(int x, int z) {
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
        return EmptyChunk.INSTANCE;
    }



    @Override
    public String toString() {
        return "MCAWorld{" +
                "worldFolder=" + worldFolder +
                ", name='" + name + '\'' +
                ", spawnPoint=" + spawnPoint +
                ", skyLight=" + skyLight +
                ", ignoreMissingLightData=" + ignoreMissingLightData +
                '}';
    }

    private static Path resolveLevelFile(Path worldFolder) throws IOException {
        Path levelFolder = worldFolder.toRealPath();
        Path levelFile = levelFolder.resolve("level.dat");
        int searchDepth = 0;

        while (!Files.isRegularFile(levelFile) && searchDepth < 4) {
            searchDepth++;
            levelFolder = levelFolder.getParent();
            if (levelFolder == null) break;

            levelFile = levelFolder.resolve("level.dat");
        }

        if (!Files.isRegularFile(levelFile))
            throw new FileNotFoundException("Could not find a level.dat file for this world!");

        return levelFile;
    }

}
