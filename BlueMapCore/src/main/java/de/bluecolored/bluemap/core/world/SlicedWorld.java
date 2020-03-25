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

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.util.AABB;

/**
 * A sliced view of a world. Everything outside the defined bounds is seen as "not generated" and "air".
 */
public class SlicedWorld implements World {

	private World world;
	private Vector3i min;
	private Vector3i max;
	
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
	public int getMaxY() {
		return world.getMaxY();
	}
	
	@Override
	public int getMinY() {
		return world.getMinY();
	}
	
	@Override
	public Biome getBiome(Vector3i pos) {
		return world.getBiome(pos);
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
	public Collection<Vector2i> getChunkList(long modifiedSince, Predicate<Vector2i> filter) {
		return world.getChunkList(modifiedSince, filter.and(chunk -> isInside(chunk)));
	}

	@Override
	public boolean isChunkGenerated(Vector2i chunkPos) throws IOException {
		if (!isInside(chunkPos)) return false;
		
		return world.isChunkGenerated(chunkPos);
	}
	
	@Override
	public boolean isAreaGenerated(AABB area) throws IOException {
		if (!isInside(blockPosToChunkPos(area.getMin())) && !isInside(blockPosToChunkPos(area.getMax()))) return false;
		
		return world.isAreaGenerated(area);
	}
	
	@Override
	public boolean isAreaGenerated(Vector3i blockMin, Vector3i blockMax) throws IOException {
		if (!isInside(blockPosToChunkPos(blockMin)) && !isInside(blockPosToChunkPos(blockMax))) return false;
		
		return world.isAreaGenerated(blockMin, blockMax);
	}
	
	@Override
	public boolean isAreaGenerated(Vector2i chunkMin, Vector2i chunkMax) throws IOException {
		if (!isInside(chunkMin) && !isInside(chunkMax)) return false;
		
		return world.isAreaGenerated(chunkMin, chunkMax);
	}

	@Override
	public void invalidateChunkCache() {
		world.invalidateChunkCache();
	}

	@Override
	public void invalidateChunkCache(Vector2i chunk) {
		world.invalidateChunkCache(chunk);
	}
	
	@Override
	public Vector2i blockPosToChunkPos(Vector3i block) {
		return world.blockPosToChunkPos(block);
	}
	
	private boolean isInside(Vector3i blockPos) {
		return isInside(blockPos.getX(), blockPos.getY(), blockPos.getZ());
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
	
	private boolean isInside(Vector2i chunkPos) {
		return isInside(chunkPos.getX(), chunkPos.getY());
	}
	
	private boolean isInside(int chunkX, int chunkZ) {
		return 
				chunkX * 16 >= min.getX() &&
				chunkX * 16 + 15 <= max.getX() &&
				chunkZ * 16 >= min.getZ() &&
				chunkZ * 16 + 15 <= max.getZ();
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
