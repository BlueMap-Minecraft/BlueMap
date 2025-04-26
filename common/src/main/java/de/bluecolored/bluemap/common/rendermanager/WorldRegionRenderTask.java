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
package de.bluecolored.bluemap.common.rendermanager;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector2l;
import de.bluecolored.bluemap.common.debug.DebugDump;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.map.renderstate.TileActionResolver.ActionAndNextState;
import de.bluecolored.bluemap.core.map.renderstate.TileActionResolver.BoundsSituation;
import de.bluecolored.bluemap.core.map.renderstate.TileInfoRegion;
import de.bluecolored.bluemap.core.map.renderstate.TileState;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.ChunkConsumer;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import static de.bluecolored.bluemap.core.map.renderstate.TileActionResolver.Action.DELETE;
import static de.bluecolored.bluemap.core.map.renderstate.TileActionResolver.Action.RENDER;

public class WorldRegionRenderTask implements MapRenderTask {

    @Getter private final BmMap map;
    @Getter private final Vector2i regionPos;
    @Getter private final TileUpdateStrategy force;

    private Grid regionGrid, chunkGrid, tileGrid;
    private Vector2i chunkMin, chunkMax, chunksSize;
    private Vector2i tileMin, tileMax, tileSize;

    private int[] chunkHashes;
    private ActionAndNextState[] tileActions;

    private volatile int nextTileX, nextTileZ;
    private volatile int atWork;
    private volatile boolean completed, cancelled;

    public WorldRegionRenderTask(BmMap map, Vector2i regionPos) {
        this(map, regionPos, false);
    }

    public WorldRegionRenderTask(BmMap map, Vector2i regionPos, boolean force) {
        this(map, regionPos, TileUpdateStrategy.fixed(force));
    }

    public WorldRegionRenderTask(BmMap map, Vector2i regionPos, TileUpdateStrategy force) {
        this.map = map;
        this.regionPos = regionPos;
        this.force = force;

        this.nextTileX = 0;
        this.nextTileZ = 0;

        this.atWork = 0;
        this.completed = false;
        this.cancelled = false;
    }

    private synchronized void init() {

        // calculate bounds
        this.regionGrid = map.getWorld().getRegionGrid();
        this.chunkGrid = map.getWorld().getChunkGrid();
        this.tileGrid = map.getHiresModelManager().getTileGrid();
        this.chunkMin = regionGrid.getCellMin(regionPos, chunkGrid);
        this.chunkMax = regionGrid.getCellMax(regionPos, chunkGrid);
        this.chunksSize = chunkMax.sub(chunkMin).add(1, 1);
        this.tileMin = regionGrid.getCellMin(regionPos, tileGrid);
        this.tileMax = regionGrid.getCellMax(regionPos, tileGrid);
        this.tileSize = tileMax.sub(tileMin).add(1, 1);

        // load chunk-hash array
        int chunkMaxCount = chunksSize.getX() * chunksSize.getY();
        try {
            chunkHashes = new int[chunkMaxCount];
            map.getWorld().getRegion(regionPos.getX(), regionPos.getY())
                    .iterateAllChunks( (ChunkConsumer.ListOnly<Chunk>) (x, z, timestamp) -> {
                        chunkHashes[chunkIndex(
                                x - chunkMin.getX(),
                                z - chunkMin.getY()
                        )] = timestamp;
                        map.getWorld().invalidateChunkCache(x, z);
                    });
        } catch (IOException ex) {
            Logger.global.logError("Failed to load chunks for region " + regionPos, ex);
            cancel();
        }

        // check tile actions
        int tileMaxCount = tileSize.getX() * tileSize.getY();
        int tileRenderCount = 0;
        int tileDeleteCount = 0;
        tileActions = new ActionAndNextState[tileMaxCount];
        for (int x = 0; x < tileSize.getX(); x++) {
            for (int z = 0; z < tileSize.getY(); z++) {
                Vector2i tile = new Vector2i(tileMin.getX() + x, tileMin.getY() + z);
                TileState tileState = map.getMapTileState().get(tile.getX(), tile.getY()).getState();

                int tileIndex = tileIndex(x, z);
                tileActions[tileIndex] = tileState.findActionAndNextState(
                        force.test(tileState) || checkChunksHaveChanges(tile),
                        checkTileBounds(tile)
                );

                if (tileActions[tileIndex].action() == RENDER)
                    tileRenderCount++;
                if (tileActions[tileIndex].action() == DELETE)
                    tileDeleteCount++;
            }
        }

        if (tileRenderCount >= tileMaxCount * 0.75)
            map.getWorld().preloadRegionChunks(regionPos.getX(), regionPos.getY());

        if (tileRenderCount + tileDeleteCount == 0)
            completed = true;

    }

    @Override
    public void doWork() {
        if (cancelled || completed) return;

        int tileX, tileZ;

        synchronized (this) {
            if (cancelled || completed) return;

            tileX = nextTileX;
            tileZ = nextTileZ;

            if (tileX == 0 && tileZ == 0) {
                init();
                if (cancelled || completed) return;
            }

            nextTileX = tileX + 1;
            if (nextTileX >= tileSize.getX()) {
                nextTileZ = tileZ + 1;
                nextTileX = 0;
            }
            if (nextTileZ >= tileSize.getY()) {
                completed = true;
            }

            this.atWork++;
        }

        processTile(tileX, tileZ);

        synchronized (this) {
            this.atWork--;

            if (atWork <= 0 && completed && !cancelled) {
                complete();
            }
        }
    }

    private void processTile(int x, int z) {
        Vector2i tile = new Vector2i(tileMin.getX() + x, tileMin.getY() + z);
        ActionAndNextState action = tileActions[tileIndex(x, z)];
        TileState resultState = TileState.RENDER_ERROR;

        try {

            resultState = switch (action.action()) {

                case NONE -> action.state();

                case RENDER -> {
                    TileState failedState = checkTileRenderPreconditions(tile);
                    if (failedState != null){
                        map.unrenderTile(tile);
                        yield failedState;
                    }

                    map.renderTile(tile);
                    yield action.state();
                }

                case DELETE -> {
                    map.unrenderTile(tile);
                    yield action.state();
                }

            };

        } catch (Exception ex) {

            Logger.global.logError("Error while processing map-tile " + tile + " for map '" + map.getId() + "'", ex);

        } finally {

            // mark tile with new state
            map.getMapTileState().set(tile.getX(), tile.getY(), new TileInfoRegion.TileInfo(
                    (int) (System.currentTimeMillis() / 1000),
                    resultState
            ));

        }

    }

    private synchronized void complete() {
        // save chunk-hashes
        if (chunkHashes != null) {
            for (int x = 0; x < chunksSize.getX(); x++) {
                for (int z = 0; z < chunksSize.getY(); z++) {
                    int hash = chunkHashes[chunkIndex(x, z)];
                    map.getMapChunkState().set(chunkMin.getX() + x, chunkMin.getY() + z, hash);
                }
            }
            chunkHashes = null;
        }

        // clear tile-actions
        tileActions = null;

        // save map (at most, every minute)
        map.save(TimeUnit.MINUTES.toMillis(1));
    }

    @Override
    @DebugDump
    public synchronized boolean hasMoreWork() {
        return !completed && !cancelled;
    }

    @Override
    @DebugDump
    public double estimateProgress() {
        if (tileSize == null) return 0;
        return Math.min((double) (nextTileZ * tileSize.getX() + nextTileX) / (tileSize.getX() * tileSize.getY()), 1);
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public String getDescription() {
        return "updating region %s".formatted(regionPos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldRegionRenderTask that = (WorldRegionRenderTask) o;
        return force == that.force && map.getId().equals(that.map.getId()) && regionPos.equals(that.regionPos);
    }

    @Override
    public int hashCode() {
        return regionPos.hashCode();
    }

    private int chunkIndex(int x, int z) {
        return z * chunksSize.getX() + x;
    }

    private int tileIndex(int x, int z) {
        return z * tileSize.getX() + x;
    }

    private boolean checkChunksHaveChanges(Vector2i tile) {
        int     minX = tileGrid.getCellMinX(tile.getX(), chunkGrid),
                maxX = tileGrid.getCellMaxX(tile.getX(), chunkGrid),
                minZ = tileGrid.getCellMinY(tile.getY(), chunkGrid),
                maxZ = tileGrid.getCellMaxY(tile.getY(), chunkGrid);

        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                int dx = chunkX - chunkMin.getX();
                int dz = chunkZ - chunkMin.getY();

                // only check hash for chunks inside the current region
                if (
                        chunkX >= chunkMin.getX() && chunkX <= chunkMax.getX() &&
                        chunkZ >= chunkMin.getY() && chunkZ <= chunkMax.getY()
                ) {
                    int hash = chunkHashes[chunkIndex(dx, dz)];
                    int lastHash = map.getMapChunkState().get(chunkX, chunkZ);

                    if (lastHash != hash) return true;
                }
            }
        }

        return false;
    }

    private BoundsSituation checkTileBounds(Vector2i tile) {
        boolean isInsideBounds = map.getMapSettings().isInsideRenderBoundaries(tile, tileGrid, true);
        if (!isInsideBounds) return BoundsSituation.OUTSIDE;

        boolean isFullyInsideBounds = map.getMapSettings().isInsideRenderBoundaries(tile, tileGrid, false);
        return isFullyInsideBounds ? BoundsSituation.INSIDE : BoundsSituation.EDGE;
    }

    private @Nullable TileState checkTileRenderPreconditions(Vector2i tile) {
        boolean chunksAreInhabited = false;

        long minInhabitedTime = map.getMapSettings().getMinInhabitedTime();
        int minInhabitedTimeRadius = map.getMapSettings().getMinInhabitedTimeRadius();
        boolean requireLight = !map.getMapSettings().isIgnoreMissingLightData();

        int     minX = tileGrid.getCellMinX(tile.getX(), chunkGrid),
                maxX = tileGrid.getCellMaxX(tile.getX(), chunkGrid),
                minZ = tileGrid.getCellMinY(tile.getY(), chunkGrid),
                maxZ = tileGrid.getCellMaxY(tile.getY(), chunkGrid);

        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                Chunk chunk = map.getWorld().getChunk(chunkX, chunkZ);
                if (chunk == Chunk.ERRORED_CHUNK) return TileState.CHUNK_ERROR;
                if (!chunk.isGenerated()) return TileState.NOT_GENERATED;
                if (requireLight && !chunk.hasLightData()) return TileState.MISSING_LIGHT;
                if (chunk.getInhabitedTime() >= minInhabitedTime) chunksAreInhabited = true;
            }
        }

        // second pass for increased inhabited-time-radius
        if (!chunksAreInhabited && minInhabitedTimeRadius > 0) {
            inhabitedRadiusCheck:
            for (int chunkX = minX - minInhabitedTimeRadius; chunkX <= maxX + minInhabitedTimeRadius; chunkX++) {
                for (int chunkZ = minZ - minInhabitedTimeRadius; chunkZ <= maxZ + minInhabitedTimeRadius; chunkZ++) {
                    Chunk chunk = map.getWorld().getChunk(chunkX, chunkZ);
                    if (chunk.getInhabitedTime() >= minInhabitedTime) {
                        chunksAreInhabited = true;
                        break inhabitedRadiusCheck;
                    }
                }
            }
        }

        return chunksAreInhabited ? null : TileState.LOW_INHABITED_TIME;
    }

    public static Comparator<WorldRegionRenderTask> defaultComparator(final Vector2i centerRegion) {
        return (task1, task2) -> {
            // use long to compare to avoid overflow (comparison uses distanceSquared)
            Vector2l task1Rel = new Vector2l(task1.regionPos.getX() - centerRegion.getX(), task1.regionPos.getY() - centerRegion.getY());
            Vector2l task2Rel = new Vector2l(task2.regionPos.getX() - centerRegion.getX(), task2.regionPos.getY() - centerRegion.getY());
            return compareVec2L(task1Rel, task2Rel);
        };
    }

    /**
     * Comparison method that doesn't overflow that easily
     */
    private static int compareVec2L(Vector2l v1, Vector2l v2) {
        return Long.signum(v1.lengthSquared() - v2.lengthSquared());
    }

}
