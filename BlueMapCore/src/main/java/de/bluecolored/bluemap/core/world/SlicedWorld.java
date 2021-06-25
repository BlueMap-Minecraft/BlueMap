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
package de.bluecolored.bluemap.core.world;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * A sliced view of a world. Everything outside the defined bounds is seen as "not generated" and "air".
 */
public class SlicedWorld implements World {

	private final World world;
	private final Vector3i min;
	private final Vector3i max;
	
	public SlicedWorld(World world, Vector3i min, Vector3i max) {
		this.world = world;
		this.min = min;
		this.max = max;
	}
	
	@Override
	public String getName() {
		return world.getName();
	}
	
	@Override
	public Path getSaveFolder() {
		return world.getSaveFolder();
	}
	
	@Override
	public UUID getUUID() {
		return world.getUUID();
	}
	
	@Override
	public int getSeaLevel() {
		return world.getSeaLevel();
	}
	
	@Override
	public Vector3i getSpawnPoint() {
		return world.getSpawnPoint();
	}
	
	@Override
	public int getMaxY(int x, int z) {
		return world.getMaxY(x, z);
	}
	
	@Override
	public int getMinY(int x, int z) {
		return world.getMinY(x, z);
	}

	@Override
	public Grid getChunkGrid() {
		return world.getChunkGrid();
	}

	@Override
	public Grid getRegionGrid() {
		return world.getRegionGrid();
	}

	@Override
	public Biome getBiome(int x, int y, int z) {
		return world.getBiome(x, y, z);
	}
	
	@Override
	public Block getBlock(Vector3i pos) {
		if (!isInside(pos)) return createAirBlock(pos);
		
		Block block = world.getBlock(pos);
		block.setWorld(this);
		return block;
	}
	
	@Override
	public Block getBlock(int x, int y, int z) {
		if (!isInside(x, y, z)) return createAirBlock(new Vector3i(x, y, z));

		Block block = world.getBlock(x, y, z);
		block.setWorld(this);
		return block;
	}

	@Override
	public Chunk getChunk(int x, int z) {
		return world.getChunk(x, z);
	}

	@Override
	public Region getRegion(int x, int z) {
		return world.getRegion(x, z);
	}

	@Override
	public Collection<Vector2i> listRegions() {
		Grid regionGrid = getRegionGrid();
		Collection<Vector2i> regions = new ArrayList<>();

		for (Vector2i region : world.listRegions()) {
			Vector2i min = regionGrid.getCellMin(region);
			Vector2i max = regionGrid.getCellMax(region);
			if (isInside(min.getX(), min.getY()) || isInside(max.getX(), max.getY())) regions.add(region);
		}

		return regions;
	}

	@Override
	public void invalidateChunkCache() {
		world.invalidateChunkCache();
	}

	@Override
	public void invalidateChunkCache(int x, int z) {
		world.invalidateChunkCache(x, z);
	}
	
	@Override
	public void cleanUpChunkCache() {
		world.cleanUpChunkCache();
	}
	
	private boolean isInside(Vector3i blockPos) {
		return isInside(blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}

	private boolean isInside(int x, int z) {
		return
				x >= min.getX() &&
				x <= max.getX() &&
				z >= min.getZ() &&
				z <= max.getZ();
	}

	private boolean isInside(int x, int y, int z) {
		return 
				x >= min.getX() &&
				x <= max.getX() &&
				z >= min.getZ() &&
				z <= max.getZ() &&
				y >= min.getY() &&
				y <= max.getY();
	}
	
	private Block createAirBlock(Vector3i pos) {
		return new Block(
				this, 
				BlockState.AIR, 
				pos.getY() < this.min.getY() ? LightData.ZERO : LightData.SKY,
				Biome.DEFAULT, 
				BlockProperties.TRANSPARENT, 
				pos
				);
	}
	
}
