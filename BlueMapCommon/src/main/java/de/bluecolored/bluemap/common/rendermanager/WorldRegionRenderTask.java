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
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.Grid;
import de.bluecolored.bluemap.core.world.Region;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@DebugDump
public class WorldRegionRenderTask implements RenderTask {

    private final BmMap map;
    private final Vector2i worldRegion;
    private final boolean force;

    private Deque<Vector2i> tiles;
    private int tileCount;
    private long startTime;

    private volatile int atWork;
    private volatile boolean cancelled;

    public WorldRegionRenderTask(BmMap map, Vector2i worldRegion) {
        this(map, worldRegion, false);
    }

    public WorldRegionRenderTask(BmMap map, Vector2i worldRegion, boolean force) {
        this.map = map;
        this.worldRegion = worldRegion;
        this.force = force;

        this.tiles = null;
        this.tileCount = -1;
        this.startTime = -1;

        this.atWork = 0;
        this.cancelled = false;
    }

    private synchronized void init() {
        Set<Vector2l> tileSet = new HashSet<>();
        startTime = System.currentTimeMillis();

        //Logger.global.logInfo("Starting: " + worldRegion);

        long changesSince = 0;
        if (!force) changesSince = map.getRenderState().getRenderTime(worldRegion);

        Region region = map.getWorld().getRegion(worldRegion.getX(), worldRegion.getY());
        Collection<Vector2i> chunks = region.listChunks(changesSince);

        Grid tileGrid = map.getHiresModelManager().getTileGrid();
        Grid chunkGrid = map.getWorld().getChunkGrid();

        for (Vector2i chunk : chunks) {
            Vector2i tileMin = chunkGrid.getCellMin(chunk, tileGrid);
            Vector2i tileMax = chunkGrid.getCellMax(chunk, tileGrid);

            for (int x = tileMin.getX(); x <= tileMax.getX(); x++) {
                for (int z = tileMin.getY(); z <= tileMax.getY(); z++) {
                    tileSet.add(new Vector2l(x, z));
                }
            }
        }

        Predicate<Vector2i> boundsTileFilter = t -> {
            Vector2i cellMin = tileGrid.getCellMin(t);
            if (cellMin.getX() > map.getMapSettings().getMaxPos().getX()) return false;
            if (cellMin.getY() > map.getMapSettings().getMaxPos().getZ()) return false;

            Vector2i cellMax = tileGrid.getCellMax(t);
            if (cellMax.getX() < map.getMapSettings().getMinPos().getX()) return false;
            return cellMax.getY() >= map.getMapSettings().getMinPos().getZ();
        };

        this.tileCount = tileSet.size();
        this.tiles = tileSet.stream()
                .sorted(WorldRegionRenderTask::compareVec2L) //sort with longs to avoid overflow (comparison uses distanceSquared)
                .map(Vector2l::toInt) // back to ints
                .filter(boundsTileFilter)
                .filter(map.getTileFilter())
                .collect(Collectors.toCollection(ArrayDeque::new));

        if (tiles.isEmpty()) complete();
    }

    @Override
    public void doWork() {
        if (cancelled) return;

        Vector2i tile;

        synchronized (this) {
            if (tiles == null) init();
            if (tiles.isEmpty()) return;

            tile = tiles.pollFirst();

            this.atWork++;
        }

        //Logger.global.logInfo("Working on " + worldRegion + " - Tile " + tile);
        if (tileRenderPreconditions(tile)) {
            map.renderTile(tile); // <- actual work
        }

        synchronized (this) {
            this.atWork--;

            if (atWork <= 0 && tiles.isEmpty() && !cancelled) {
                complete();
            }
        }
    }

    private boolean tileRenderPreconditions(Vector2i tile) {
        Grid tileGrid = map.getHiresModelManager().getTileGrid();
        Grid chunkGrid = map.getWorld().getChunkGrid();

        Vector2i minChunk = tileGrid.getCellMin(tile, chunkGrid);
        Vector2i maxChunk = tileGrid.getCellMax(tile, chunkGrid);

        long minInhab = map.getMapSettings().getMinInhabitedTime();
        int minInhabRadius = map.getMapSettings().getMinInhabitedTimeRadius();
        if (minInhabRadius < 0) minInhabRadius = 0;
        if (minInhabRadius > 16) minInhabRadius = 16; // sanity check
        boolean isInhabited = false;

        for (int x = minChunk.getX(); x <= maxChunk.getX(); x++) {
            for (int z = minChunk.getY(); z <= maxChunk.getY(); z++) {
                Chunk chunk = map.getWorld().getChunk(x, z);
                if (!chunk.isGenerated()) return false;
                if (chunk.getInhabitedTime() < minInhab) isInhabited = true;
            }
        }

        if (minInhabRadius > 0) {
            for (int x = minChunk.getX() - minInhabRadius; x <= maxChunk.getX() + minInhabRadius; x++) {
                for (int z = minChunk.getY() - minInhabRadius; z <= maxChunk.getY() + minInhabRadius; z++) {
                    Chunk chunk = map.getWorld().getChunk(x, z);
                    if (chunk.getInhabitedTime() < minInhab) isInhabited = true;
                }
            }
        }

        return isInhabited;
    }

    private void complete() {
        map.getRenderState().setRenderTime(worldRegion, startTime);

        //Logger.global.logInfo("Done with: " + worldRegion);
    }

    @Override
    @DebugDump
    public synchronized boolean hasMoreWork() {
        return !cancelled && (tiles == null || !tiles.isEmpty());
    }

    @Override
    @DebugDump
    public double estimateProgress() {
        if (tiles == null) return 0;
        if (tileCount == 0) return 1;

        double remainingTiles = tiles.size();
        return 1 - (remainingTiles / this.tileCount);
    }

    @Override
    public void cancel() {
        this.cancelled = true;

        synchronized (this) {
            if (tiles != null) this.tiles.clear();
        }
    }

    public BmMap getMap() {
        return map;
    }

    public Vector2i getWorldRegion() {
        return worldRegion;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public String getDescription() {
        return "Update region " + getWorldRegion() + " for map '" + map.getId() + "'";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorldRegionRenderTask that = (WorldRegionRenderTask) o;
        return force == that.force && map.getId().equals(that.map.getId()) && worldRegion.equals(that.worldRegion);
    }

    @Override
    public int hashCode() {
        return worldRegion.hashCode();
    }

    public static Comparator<WorldRegionRenderTask> defaultComparator(final Vector2i centerRegion) {
        return (task1, task2) -> {
            // use long to compare to avoid overflow (comparison uses distanceSquared)
            Vector2l task1Rel = new Vector2l(task1.worldRegion.getX() - centerRegion.getX(), task1.worldRegion.getY() - centerRegion.getY());
            Vector2l task2Rel = new Vector2l(task2.worldRegion.getX() - centerRegion.getX(), task2.worldRegion.getY() - centerRegion.getY());
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
