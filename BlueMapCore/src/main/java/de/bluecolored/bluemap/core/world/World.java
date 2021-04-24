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
import java.util.Collection;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Represents a World on the Server<br>
 * <br>
 * <i>The implementation of this class has to be thread-save!</i><br>
 */
public interface World {

	String getName();
	
	UUID getUUID();
	
	Path getSaveFolder();
	
	int getSeaLevel();
	
	Vector3i getSpawnPoint();
	
	int getMaxY();
	
	int getMinY();

	Grid getChunkGrid();

	Grid getRegionGrid();

	/**
	 * Returns the {@link Biome} on the specified position or the default biome if the block is not generated yet.
	 */
	Biome getBiome(int x, int y, int z);
	
	/**
	 * Returns the {@link Block} on the specified position or an air-block if the block is not generated yet.
	 */
	Block getBlock(Vector3i pos);
	
	/**
	 * Returns the {@link Block} on the specified position or an air-block if the block is not generated yet.
	 */
	default Block getBlock(int x, int y, int z) {
		return getBlock(new Vector3i(x, y, z));
	}

	/**
	 * Returns the {@link Chunk} on the specified chunk-position
	 */
	Chunk getChunk(int x, int z);

	/**
	 * Returns the Chunk on the specified chunk-position
	 */
	Region getRegion(int x, int z);

	/**
	 * Returns a collection of all regions in this world.
	 * <i>(Be aware that the collection is not cached and recollected each time from the world-files!)</i>
	 */
	Collection<Vector2i> listRegions();

	/**
	 * Invalidates the complete chunk cache (if there is a cache), so that every chunk has to be reloaded from disk
	 */
	void invalidateChunkCache();

	/**
	 * Invalidates the chunk from the chunk-cache (if there is a cache), so that the chunk has to be reloaded from disk
	 */
	void invalidateChunkCache(int x, int z);
	
	/**
	 * Cleans up invalid cache-entries to free up memory
	 */
	void cleanUpChunkCache();
	
}
