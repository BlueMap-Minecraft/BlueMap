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
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.map.renderstate.MapTileState;
import de.bluecolored.bluemap.core.map.renderstate.TileInfoRegion;
import de.bluecolored.bluemap.core.map.renderstate.TileState;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.world.World;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class MapUpdatePreparationTask implements MapRenderTask {

    @Getter private final BmMap map;
    private final @Nullable Vector2i center;
    private final @Nullable Integer radius;
    private final TileUpdateStrategy force;
    private final Consumer<MapUpdateTask> taskConsumer;

    private volatile boolean hasMoreWork, cancelled;

    @Builder
    protected MapUpdatePreparationTask(
            @NonNull BmMap map,
            @Nullable Vector2i center,
            @Nullable Integer radius,
            TileUpdateStrategy force,
            @NonNull Consumer<MapUpdateTask> taskConsumer
    ) {
        this.map = map;
        this.center = center;
        this.radius = radius;
        this.force = force != null ? force : TileUpdateStrategy.FORCE_NONE;
        this.taskConsumer = taskConsumer;
        this.hasMoreWork = true;
    }

    @Override
    public void doWork() {
        synchronized (this) {
            if (!hasMoreWork) return;
            hasMoreWork = false;
        }
        if (cancelled) return;

        // do work
        Collection<Vector2i> regions = findRegions();
        Collection<RenderTask> tasks = createTasks(regions);
        MapUpdateTask mapUpdateTask = new MapUpdateTask(map, tasks);

        if (cancelled) return;

        // return created task
        taskConsumer.accept(mapUpdateTask);
    }

    @Override
    public boolean hasMoreWork() {
        return hasMoreWork && !cancelled;
    }

    @Override
    public void cancel() {
        cancelled = true;
    }

    @Override
    public String getDescription() {
        return "preparing map '%s' update".formatted(map.getId());
    }

    private Collection<RenderTask> createTasks(Collection<Vector2i> regions) {
        ArrayList<WorldRegionRenderTask> regionTasks = new ArrayList<>(regions.size());
        regions.forEach(region -> regionTasks.add(new WorldRegionRenderTask(map, region, force)));

        // sort tasks by distance to 0/0
        regionTasks.sort(WorldRegionRenderTask.defaultComparator(Vector2i.ZERO));

        // save map before and after the whole update
        ArrayList<RenderTask> tasks = new ArrayList<>(regionTasks.size() + 2);
        tasks.add(new MapSaveTask(map));
        tasks.addAll(regionTasks);
        tasks.add(new MapSaveTask(map));

        return tasks;
    }

    private Collection<Vector2i> findRegions() {
        World world = map.getWorld();
        Grid regionGrid = world.getRegionGrid();

        Predicate<Vector2i> regionBoundsFilter = map.getMapSettings().getCellRenderBoundariesFilter(regionGrid, true);
        Predicate<Vector2i> regionRadiusFilter;
        if (center == null || radius == null || radius < 0) {
            regionRadiusFilter = r -> true;
        } else {
            Vector2i halfCell = regionGrid.getGridSize().div(2);
            long increasedRadiusSquared = (long) Math.pow(radius + Math.ceil(halfCell.length()), 2);
            regionRadiusFilter = r -> {
                Vector2i min = regionGrid.getCellMin(r);
                Vector2i regionCenter = min.add(halfCell);
                return regionCenter.toLong().distanceSquared(center.toLong()) <= increasedRadiusSquared;
            };
        }

        Set<Vector2i> regions = new HashSet<>();

        // update all regions in the world-files
        world.listRegions().stream()
                .filter(regionBoundsFilter)
                .filter(regionRadiusFilter)
                .forEach(regions::add);

        // also update regions that are present as map-tile-state files (they might have been rendered before but deleted now)
        // (a little hacky as we are operating on raw tile-state files -> maybe find a better way?)
        if (map.getMapSettings().isCheckForRemovedRegions()) {
            Grid tileGrid = map.getHiresModelManager().getTileGrid();
            Grid cellGrid = MapTileState.GRID.multiply(tileGrid);
            try (Stream<GridStorage.Cell> stream = map.getStorage().tileState().stream()) {
                stream
                        .filter(c -> {
                            // filter out files that are fully UNKNOWN/NOT_GENERATED
                            // this avoids unnecessarily converting UNKNOWN tiles into NOT_GENERATED tiles on force-updates
                            try (CompressedInputStream in = c.read()) {
                                if (in == null) return false;
                                TileState[] states = TileInfoRegion.loadPalette(in.decompress());
                                for (TileState state : states) {
                                    if (
                                            state != TileState.UNKNOWN &&
                                                    state != TileState.NOT_GENERATED
                                    ) return true;
                                }
                                return false;
                            } catch (IOException ignore) {
                                return true;
                            }
                        })
                        .map(c -> new Vector2i(c.getX(), c.getZ()))
                        .flatMap(v -> cellGrid.getIntersecting(v, regionGrid).stream())
                        .filter(regionRadiusFilter)
                        .forEach(regions::add);
            } catch (IOException ex) {
                Logger.global.logError("Failed to load map tile state!", ex);
            }
        }

        return regions;
    }

    public static MapUpdatePreparationTask updateMap(BmMap map, RenderManager renderManager) {
        return builder()
                .map(map)
                .taskConsumer(renderManager::scheduleRenderTask)
                .build();
    }

    public static MapUpdatePreparationTask updateMap(BmMap map, TileUpdateStrategy force, RenderManager renderManager) {
        return builder()
                .map(map)
                .force(force)
                .taskConsumer(renderManager::scheduleRenderTask)
                .build();
    }

}
