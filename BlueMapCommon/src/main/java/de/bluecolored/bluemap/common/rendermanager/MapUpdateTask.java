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
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.world.Grid;
import de.bluecolored.bluemap.core.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@DebugDump
public class MapUpdateTask extends CombinedRenderTask<WorldRegionRenderTask> {

	private final BmMap map;
	private final Collection<Vector2i> regions;

	public MapUpdateTask(BmMap map) {
		this(map, getRegions(map.getWorld()));
	}

	public MapUpdateTask(BmMap map, boolean force) {
		this(map, getRegions(map.getWorld()), force);
	}

	public MapUpdateTask(BmMap map, Vector2i center, int radius) {
		this(map, getRegions(map.getWorld(), center, radius));
	}

	public MapUpdateTask(BmMap map, Vector2i center, int radius, boolean force) {
		this(map, getRegions(map.getWorld(), center, radius), force);
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

	private static Collection<WorldRegionRenderTask> createTasks(BmMap map, Collection<Vector2i> regions, boolean force) {
		List<WorldRegionRenderTask> tasks = new ArrayList<>(regions.size());
		regions.forEach(region -> tasks.add(new WorldRegionRenderTask(map, region, force)));

		// get spawn region
		World world = map.getWorld();
		Vector2i spawnPoint = world.getSpawnPoint().toVector2(true);
		Grid regionGrid = world.getRegionGrid();
		Vector2i spawnRegion = regionGrid.getCell(spawnPoint);

		tasks.sort(WorldRegionRenderTask.defaultComparator(spawnRegion));
		return tasks;
	}

	private static List<Vector2i> getRegions(World world) {
		return getRegions(world, null, -1);
	}

	private static List<Vector2i> getRegions(World world, Vector2i center, int radius) {
		if (center == null || radius < 0) return new ArrayList<>(world.listRegions());

		List<Vector2i> regions = new ArrayList<>();

		Grid regionGrid = world.getRegionGrid();
		Vector2i halfCell = regionGrid.getGridSize().div(2);
		int increasedRadiusSquared = (int) Math.pow(radius + Math.ceil(halfCell.length()), 2);

		for (Vector2i region : world.listRegions()) {
			Vector2i min = regionGrid.getCellMin(region);
			Vector2i regionCenter = min.add(halfCell);

			if (regionCenter.distanceSquared(center) <= increasedRadiusSquared)
				regions.add(region);
		}

		return regions;
	}

}
