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
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.debug.StateDumper;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.Grid;
import de.bluecolored.bluemap.core.world.World;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.NBTUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@DebugDump
public class MCAWorld implements World {

    private static final Grid CHUNK_GRID = new Grid(16);
    private static final Grid REGION_GRID = new Grid(32).multiply(CHUNK_GRID);

    private final Path worldFolder;

    private final String name;
    private final Vector3i spawnPoint;

    private final int skyLight;
    private final boolean ignoreMissingLightData;

    private final LoadingCache<Vector2i, MCARegion> regionCache;
    private final LoadingCache<Vector2i, MCAChunk> chunkCache;

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

        try {
            Path levelFile = resolveLevelFile(worldFolder);
            CompoundTag level = (CompoundTag) NBTUtil.readTag(levelFile.toFile());
            CompoundTag levelData = level.getCompoundTag("Data");

            this.name = levelData.getString("LevelName");

            this.spawnPoint = new Vector3i(
                    levelData.getInt("SpawnX"),
                    levelData.getInt("SpawnY"),
                    levelData.getInt("SpawnZ")
            );
        } catch (ClassCastException | NullPointerException ex) {
            throw new IOException("Invalid level.dat format!", ex);
        }
    }

    @Override
    public MCAChunk getChunkAtBlock(int x, int y, int z) {
        return getChunk(x >> 4, z >> 4);
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
        chunkCache.invalidate(vec2i(x, z));
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

    private static final int VEC_2I_CACHE_SIZE = 0x4000;
    private static final int VEC_2I_CACHE_MASK = VEC_2I_CACHE_SIZE - 1;
    private static final Vector2i[] VEC_2I_CACHE = new Vector2i[VEC_2I_CACHE_SIZE];
    private static final HitMissMetric HIT_MISS_METRIC = new HitMissMetric();
    static {
        StateDumper.global().register(HIT_MISS_METRIC);
    }

    private static Vector2i vec2i(int x, int y) {
        int cacheIndex = (x * 1456 ^ y * 948375892) & VEC_2I_CACHE_MASK;
        Vector2i possibleMatch = VEC_2I_CACHE[cacheIndex];

        if (possibleMatch != null && possibleMatch.getX() == x && possibleMatch.getY() == y) {
            HIT_MISS_METRIC.hit();
            return possibleMatch;
        }

        HIT_MISS_METRIC.miss();
        return VEC_2I_CACHE[cacheIndex] = new Vector2i(x, y);
    }

    @DebugDump
    private static class HitMissMetric {

        private final AtomicLong
                hits = new AtomicLong(),
                misses = new AtomicLong();

        public void hit() {
            hits.incrementAndGet();
        }

        public void miss() {
            misses.incrementAndGet();
        }

        public long getHits() {
            return hits.get();
        }

        public long getMisses() {
            return misses.get();
        }

        @DebugDump
        public long getSum() {
            return hits.get() + misses.get();
        }

        @DebugDump
        public double getHitRate() {
            long hits = getHits();
            long misses = getMisses();
            return (double) hits / (hits + misses);
        }

    }

}
