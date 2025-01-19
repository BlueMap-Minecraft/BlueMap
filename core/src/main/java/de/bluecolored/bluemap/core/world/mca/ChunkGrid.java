package de.bluecolored.bluemap.core.world.mca;

import com.flowpowered.math.vector.Vector2i;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.util.Vector2iCache;
import de.bluecolored.bluemap.core.util.WatchService;
import de.bluecolored.bluemap.core.world.ChunkConsumer;
import de.bluecolored.bluemap.core.world.Region;
import de.bluecolored.bluemap.core.world.mca.region.RegionType;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class ChunkGrid<T> {
    private static final Grid CHUNK_GRID = new Grid(16);
    private static final Grid REGION_GRID = new Grid(32).multiply(CHUNK_GRID);

    private static final Vector2iCache VECTOR_2_I_CACHE = new Vector2iCache();

    private final ChunkLoader<T> chunkLoader;
    private final Path regionFolder;

    private final LoadingCache<Vector2i, Region<T>> regionCache = Caffeine.newBuilder()
            .executor(BlueMap.THREAD_POOL)
            .softValues()
            .maximumSize(32)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(this::loadRegion);
    private final LoadingCache<Vector2i, T> chunkCache = Caffeine.newBuilder()
            .executor(BlueMap.THREAD_POOL)
            .softValues()
            .maximumSize(10240) // 10 regions worth of chunks
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(this::loadChunk);

    public Grid getChunkGrid() {
        return CHUNK_GRID;
    }

    public Grid getRegionGrid() {
        return REGION_GRID;
    }

    public T getChunk(int x, int z) {
        return getChunk(VECTOR_2_I_CACHE.get(x, z));
    }

    private T getChunk(Vector2i pos) {
        return chunkCache.get(pos);
    }

    public Region<T> getRegion(int x, int z) {
        return getRegion(VECTOR_2_I_CACHE.get(x, z));
    }

    private Region<T> getRegion(Vector2i pos) {
        return regionCache.get(pos);
    }

    public void preloadRegionChunks(int x, int z, Predicate<Vector2i> chunkFilter) {
        try {
            getRegion(x, z).iterateAllChunks(new ChunkConsumer<>() {
                @Override
                public boolean filter(int chunkX, int chunkZ, int lastModified) {
                    Vector2i chunkPos = VECTOR_2_I_CACHE.get(chunkX, chunkZ);
                    return chunkFilter.test(chunkPos);
                }

                @Override
                public void accept(int chunkX, int chunkZ, T chunk) {
                    Vector2i chunkPos = VECTOR_2_I_CACHE.get(chunkX, chunkZ);
                    chunkCache.put(chunkPos, chunk);
                }
            });
        } catch (IOException ex) {
            Logger.global.logDebug("Unexpected exception trying to load preload region ('%s' -> x:%d, z:%d): %s".formatted(regionFolder, x, z, ex));
        }
    }

    public Collection<Vector2i> listRegions() {
        if (!Files.exists(regionFolder)) return Collections.emptyList();
        try (Stream<Path> stream = Files.list(regionFolder)) {
            return stream
                    .map(file -> {
                        try {
                            if (Files.size(file) <= 0) return null;
                            return RegionType.regionForFileName(file.getFileName().toString());
                        } catch (IOException ex) {
                            Logger.global.logError("Failed to read region-file: " + file, ex);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException ex) {
            Logger.global.logError("Failed to list regions from: '%s'".formatted(regionFolder), ex);
            return List.of();
        }
    }

    public WatchService<Vector2i> createRegionWatchService() throws IOException {
        return new MCAWorldRegionWatchService(this.regionFolder);
    }

    public void invalidateChunkCache() {
        regionCache.invalidateAll();
        chunkCache.invalidateAll();
    }

    public void invalidateChunkCache(int x, int z) {
        regionCache.invalidate(VECTOR_2_I_CACHE.get(x >> 5, z >> 5));
        chunkCache.invalidate(VECTOR_2_I_CACHE.get(x, z));
    }

    private Region<T> loadRegion(Vector2i regionPos) {
        return loadRegion(regionPos.getX(), regionPos.getY());
    }

    private Region<T> loadRegion(int x, int z) {
        return RegionType.loadRegion(chunkLoader, regionFolder, x, z);
    }

    private T loadChunk(Vector2i chunkPos) {
        return loadChunk(chunkPos.getX(), chunkPos.getY());
    }

    private T loadChunk(int x, int z) {
        final int tries = 3;
        final int tryInterval = 1000;

        Exception loadException = null;
        for (int i = 0; i < tries; i++) {
            try {
                return getRegion(x >> 5, z >> 5)
                        .loadChunk(x, z);
            } catch (IOException | RuntimeException e) {
                if (loadException != null && loadException != e)
                    e.addSuppressed(loadException);

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

        Logger.global.logDebug("Unexpected exception trying to load chunk ('%s' -> x:%d, z:%d): %s".formatted(regionFolder, x, z, loadException));
        return chunkLoader.erroredChunk();
    }

}
