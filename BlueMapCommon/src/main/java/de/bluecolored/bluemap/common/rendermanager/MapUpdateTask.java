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
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@DebugDump
public class MapUpdateTask extends CombinedRenderTask<RenderTask> {

    private final BmMap map;
    private final Collection<Vector2i> regions;

    public MapUpdateTask(BmMap map) {
        this(map, getRegions(map));
    }

    public MapUpdateTask(BmMap map, boolean force) {
        this(map, getRegions(map), force);
    }

    public MapUpdateTask(BmMap map, Vector2i center, int radius) {
        this(map, getRegions(map, center, radius));
    }

    public MapUpdateTask(BmMap map, Vector2i center, int radius, boolean force) {
        this(map, getRegions(map, center, radius), force);
    }

    public MapUpdateTask(BmMap map, Collection<Vector2i> regions) {
        this(map, regions, false);
    }

    public MapUpdateTask(BmMap map, Collection<Vector2i> regions, boolean force) {
        super("Update map '" + map.getId() + "'", createTasks(map, regions, force));
        this.map = map;
        this.regions = Collections.unmodifiableCollection(new ArrayList<>(regions));
    }

    public BmMap getMap() {
        return map;
    }

    public Collection<Vector2i> getRegions() {
        return regions;
    }

    private static Collection<RenderTask> createTasks(BmMap map, Collection<Vector2i> regions, boolean force) {
        ArrayList<WorldRegionRenderTask> regionTasks = new ArrayList<>(regions.size());
        regions.forEach(region -> regionTasks.add(new WorldRegionRenderTask(map, region, force)));

        // get spawn region
        World world = map.getWorld();
        Vector2i spawnPoint = world.getSpawnPoint().toVector2(true);
        Grid regionGrid = world.getRegionGrid();
        Vector2i spawnRegion = regionGrid.getCell(spawnPoint);

        // sort tasks by distance to the spawn region
        regionTasks.sort(WorldRegionRenderTask.defaultComparator(spawnRegion));

        // save map before and after the whole update
        ArrayList<RenderTask> tasks = new ArrayList<>(regionTasks.size() + 2);
        tasks.add(new MapSaveTask(map));
        tasks.addAll(regionTasks);
        tasks.add(new MapSaveTask(map));

        return tasks;
    }

    private static List<Vector2i> getRegions(BmMap map) {
        return getRegions(map, null, -1);
    }

    private static List<Vector2i> getRegions(BmMap map, Vector2i center, int radius) {
        World world = map.getWorld();
        Grid regionGrid = world.getRegionGrid();
        Predicate<Vector2i> regionFilter = map.getMapSettings().getRenderBoundariesCellFilter(regionGrid);

        if (center == null || radius < 0) {
            return world.listRegions().stream()
                    .filter(regionFilter)
                    .collect(Collectors.toList());
        }

        List<Vector2i> regions = new ArrayList<>();
        Vector2i halfCell = regionGrid.getGridSize().div(2);
        long increasedRadiusSquared = (long) Math.pow(radius + Math.ceil(halfCell.length()), 2);

        for (Vector2i region : world.listRegions()) {
            if (!regionFilter.test(region)) continue;

            Vector2i min = regionGrid.getCellMin(region);
            Vector2i regionCenter = min.add(halfCell);

            if (regionCenter.toLong().distanceSquared(center.toLong()) <= increasedRadiusSquared)
                regions.add(region);
        }

        return regions;
    }

}
