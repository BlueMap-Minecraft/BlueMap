package de.bluecolored.bluemap.core.map.lowres;

import com.flowpowered.math.vector.Vector2i;
import com.github.benmanes.caffeine.cache.*;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.util.Vector2iCache;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.Grid;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LowresTileManager {

    private final Storage.MapStorage mapStorage;

    private final Grid tileGrid;
    private final int lodFactor, lodCount;

    private final Vector2iCache vector2iCache;
    private final List<LoadingCache<Vector2i, LowresTile>> tileCaches;

    public LowresTileManager(Storage.MapStorage mapStorage, Grid tileGrid, int lodCount, int lodFactor) {
        this.mapStorage = mapStorage;

        this.tileGrid = tileGrid;
        this.lodFactor = lodFactor;
        this.lodCount = lodCount;

        this.vector2iCache = new Vector2iCache();
        List<LoadingCache<Vector2i, LowresTile>> tileCacheList = new ArrayList<>();
        for (int i = 0; i < lodCount; i++) {
            int lod = i + 1;
            tileCacheList.add(Caffeine.newBuilder()
                    .executor(BlueMap.THREAD_POOL)
                    .scheduler(Scheduler.systemScheduler())
                    .expireAfterAccess(10, TimeUnit.SECONDS)
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .writer(new CacheWriter<Vector2i, LowresTile>() {

                        @Override
                        public void write(@NonNull Vector2i key, @NonNull LowresTile value) {}

                        @Override
                        public void delete(@NonNull Vector2i key, @Nullable LowresTile value, @NonNull RemovalCause cause) {
                            if (value != null)
                                saveTile(key, lod, value, cause);
                        }

                    })
                    .build(key -> loadTile(key, lod))
            );
        }
        this.tileCaches = Collections.unmodifiableList(tileCacheList);
    }

    private LowresTile loadTile(Vector2i tilePos, int lod) {
        try (InputStream in = mapStorage.read(lod, tilePos).orElse(null)) {
            if (in == null)
                return new LowresTile(tileGrid.getGridSize());
            return new LowresTile(tileGrid.getGridSize(), in);
        } catch (IOException e) {
            Logger.global.logError("Failed to load tile " + tilePos + " (lod: " + lod + ")", e);
            return null;
        }
    }

    @SuppressWarnings("unused")
    private void saveTile(Vector2i tilePos, int lod, LowresTile tile, RemovalCause removalCause) {
        // close the tile so it can't be edited anymore
        tile.close();

        // save the tile
        try (OutputStream out = mapStorage.write(lod, tilePos)) {
            tile.save(out);
        } catch (IOException e) {
            Logger.global.logError("Failed to save tile " + tilePos + " (lod: " + lod + ")", e);
        }

        if (lod >= lodCount) return;

        // write to next LOD (prepare for the most confusing grid-math you will ever see)
        Color averageColor = new Color();
        int averageHeight;
        int count;

        Color color = new Color();

        int nextLodTileX = Math.floorDiv(tilePos.getX(), lodFactor);
        int nextLodTileY = Math.floorDiv(tilePos.getY(), lodFactor);
        Vector2i groupCount = new Vector2i(
                Math.floorDiv(tileGrid.getGridSize().getX(), lodFactor),
                Math.floorDiv(tileGrid.getGridSize().getY(), lodFactor)
        );

        for (int gX = 0; gX < groupCount.getX(); gX++) {
            for (int gY = 0; gY < groupCount.getY(); gY++) {
                averageColor.set(0, 0, 0, 0, true);
                averageHeight = 0;
                count = 0;
                for (int x = 0; x < lodFactor; x++) {
                    for (int y = 0; y < lodFactor; y++) {
                        count++;
                        averageColor.add(tile.getColor(gX * lodFactor + x, gY * lodFactor + y, color).premultiplied());
                        averageHeight += tile.getHeight(gX * lodFactor + x, gY * lodFactor + y);
                    }
                }
                averageColor.div(count);
                averageHeight /= count;

                set(
                        nextLodTileX,
                        nextLodTileY,
                        lod + 1,
                        Math.floorMod(tilePos.getX(), lodFactor) * groupCount.getX() + gX,
                        Math.floorMod(tilePos.getY(), lodFactor) * groupCount.getY() + gY,
                        averageColor,
                        averageHeight
                );
            }
        }
    }

    public synchronized void save() {
        for (LoadingCache<Vector2i, LowresTile> cache : this.tileCaches) {
            cache.invalidateAll();
            cache.cleanUp();
        }
    }

    public LowresTile getTile(int x, int z, int lod) {
        return tileCaches.get(lod - 1).get(vector2iCache.get(x, z));
    }

    public void set(int x, int z, Color color, int height) {
        int cellX = tileGrid.getCellX(x);
        int cellZ = tileGrid.getCellY(z);
        int localX = tileGrid.getLocalX(x);
        int localZ = tileGrid.getLocalY(z);
        set(cellX, cellZ, 1, localX, localZ, color, height);
    }

    private void set(int cellX, int cellZ, int lod, int pixelX, int pixelZ, Color color, int height) {

        int tries = 0;
        LowresTile.TileClosedException closedException;
        do {
            tries ++;
            closedException = null;
            try {
                getTile(cellX, cellZ, lod)
                        .set(pixelX, pixelZ, color, height);

                // for seamless edges
                if (pixelX == 0) {
                    getTile(cellX - 1, cellZ, lod)
                            .set(tileGrid.getGridSize().getX(), pixelZ, color, height);
                }

                if (pixelZ == 0) {
                    getTile(cellX, cellZ - 1, lod)
                            .set(pixelX, tileGrid.getGridSize().getY(), color, height);
                }

                if (pixelX == 0 && pixelZ == 0) {
                    getTile(cellX - 1, cellZ - 1, lod)
                            .set(tileGrid.getGridSize().getX(), tileGrid.getGridSize().getY(), color, height);
                }
            } catch (LowresTile.TileClosedException ex) {
                closedException = ex;
            }
        } while (closedException != null && tries < 10);

    }

}
