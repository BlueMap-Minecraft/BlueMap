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
package de.bluecolored.bluemap.core.map.lowres;

import com.flowpowered.math.vector.Vector2i;
import com.github.benmanes.caffeine.cache.*;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.util.Vector2iCache;
import de.bluecolored.bluemap.core.util.math.Color;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class LowresLayer {

    private static final Vector2iCache VECTOR_2_I_CACHE = new Vector2iCache();

    private final GridStorage storage;

    private final Grid tileGrid;
    private final int lodFactor;

    private final int lod;
    private final LoadingCache<Vector2i, LowresTile> tileCache;
    @Nullable private final LowresLayer nextLayer;

    public LowresLayer(
            GridStorage storage, Grid tileGrid, int lodFactor,
            int lod, @Nullable LowresLayer nextLayer
    ) {
        this.storage = storage;

        this.tileGrid = tileGrid;
        this.lodFactor = lodFactor;

        this.lod = lod;
        this.nextLayer = nextLayer;

        // this extra cache makes sure that a tile instance is reused as long as it is still referenced somewhere ..
        // so always only one instance of the same lowres-tile exists
        LoadingCache<Vector2i, LowresTile> tileWeakInstanceCache = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .weakValues()
                .build(this::createTile);

        this.tileCache = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .scheduler(Scheduler.systemScheduler())
                .expireAfterAccess(10, TimeUnit.SECONDS)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .writer(new CacheWriter<Vector2i, LowresTile>() {
                    @Override
                    public void write(@NonNull Vector2i key, @NonNull LowresTile value) {}

                    @Override
                    public void delete(@NonNull Vector2i key, @Nullable LowresTile value, @NonNull RemovalCause cause) {
                        saveTile(key, value);
                    }
                })
                .build(tileWeakInstanceCache::get);
    }

    public void save() {
        tileCache.invalidateAll();
        tileCache.cleanUp();
    }

    private LowresTile createTile(Vector2i tilePos) {
        try (InputStream in = storage.read(tilePos.getX(), tilePos.getY())) {
            if (in != null) return new LowresTile(tileGrid.getGridSize(), in);
        } catch (IOException e) {
            Logger.global.logError("Failed to load tile " + tilePos + " (lod: " + lod + ")", e);
        }

        // if the tile can not be loaded, we create a new one
        return new LowresTile(tileGrid.getGridSize());
    }

    private void saveTile(Vector2i tilePos, @Nullable LowresTile tile) {
        if (tile == null) return;

        // check if storage is closed
        if (storage.isClosed()){
            Logger.global.logDebug("Tried to save tile " + tilePos + " (lod: " + lod + ") but storage is already closed.");
            return;
        }

        // save the tile
        try (OutputStream out = storage.write(tilePos.getX(), tilePos.getY())) {
            tile.save(out);
        } catch (IOException e) {
            Logger.global.logError("Failed to save tile " + tilePos + " (lod: " + lod + ")", e);
        }

        // write to next LOD (prepare for the most confusing grid-math you will ever see)
        if (this.nextLayer == null) return;

        Color averageColor = new Color();
        int averageHeight, averageBlockLight;
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
                averageBlockLight = 0;
                count = 0;
                for (int x = 0; x < lodFactor; x++) {
                    for (int y = 0; y < lodFactor; y++) {
                        count++;
                        averageColor.add(tile.getColor(gX * lodFactor + x, gY * lodFactor + y, color).premultiplied());
                        averageHeight += tile.getHeight(gX * lodFactor + x, gY * lodFactor + y);
                        averageBlockLight += tile.getBlockLight(gX * lodFactor + x, gY * lodFactor + y);
                    }
                }
                averageColor.div(count);
                averageHeight /= count;
                averageBlockLight /= count;

                this.nextLayer.set(
                        nextLodTileX,
                        nextLodTileY,
                        Math.floorMod(tilePos.getX(), lodFactor) * groupCount.getX() + gX,
                        Math.floorMod(tilePos.getY(), lodFactor) * groupCount.getY() + gY,
                        averageColor,
                        averageHeight,
                        averageBlockLight
                );
            }
        }
    }

    private LowresTile getTile(int x, int z) {
        return tileCache.get(VECTOR_2_I_CACHE.get(x, z));
    }

    void set(int cellX, int cellZ, int pixelX, int pixelZ, Color color, int height, int blockLight) {
        getTile(cellX, cellZ)
                .set(pixelX, pixelZ, color, height, blockLight);

        // for seamless edges
        if (pixelX == 0) {
            getTile(cellX - 1, cellZ)
                    .set(tileGrid.getGridSize().getX(), pixelZ, color, height, blockLight);
        }

        if (pixelZ == 0) {
            getTile(cellX, cellZ - 1)
                    .set(pixelX, tileGrid.getGridSize().getY(), color, height, blockLight);
        }

        if (pixelX == 0 && pixelZ == 0) {
            getTile(cellX - 1, cellZ - 1)
                    .set(tileGrid.getGridSize().getX(), tileGrid.getGridSize().getY(), color, height, blockLight);
        }
    }

}
