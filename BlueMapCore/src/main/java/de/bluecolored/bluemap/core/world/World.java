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

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.util.AABB;

/**
 * Represents a World on the Server<br>
 * <br>
 * <i>The implementation of this class has to be thread-save!</i><br>
 */
public interface World {

	String getName();
	
	UUID getUUID();
	
	int getSeaLevel();
	
	Vector3i getSpawnPoint();
	
	default int getMaxY() {
		return 255;
	}
	
	default int getMinY() {
		return 0;
	}
	
	/**
	 * Returns the Block on the specified position.<br>
	 * <br>
	 * <i>(The implementation should not invoke the generation of new Terrain, it should rather throw a {@link ChunkNotGeneratedException} if a not generated block is requested)</i><br>
	 */
	Block getBlock(Vector3i pos);
	
	/**
	 * Returns the Block on the specified position.<br>
	 * <br>
	 * <i>(The implementation should not invoke the generation of new Terrain, it should rather throw a {@link ChunkNotGeneratedException} if a not generated block is requested)</i><br>
	 */
	default Block getBlock(int x, int y, int z) {
		return getBlock(new Vector3i(x, y, z));
	}
	
	/**
	 * Returns a collection of all generated chunks.<br>
	 * <i>(Be aware that the collection is not cached and recollected each time from the world-files!)</i>
	 */
	public default Collection<Vector2i> getChunkList(){
		return getChunkList(0);
	}
	
	/**
	 * Returns a collection of all chunks that have been modified at or after the specified timestamp.<br>
	 * <i>(Be aware that the collection is not cached and recollected each time from the world-files!)</i>
	 */
	public Collection<Vector2i> getChunkList(long modifiedSince);

	/**
	 * Returns true if and only if that chunk is fully generated and no world-generation or lighting has yet to be done.
	 */
	public boolean isChunkGenerated(Vector2i chunkPos) throws IOException;
	
	
	/**
	 * Returns true if and only if all chunks the given area is intersecting are fully generated and no world-generation or lighting has yet to be done.
	 * @param area The area to check
	 * @throws IOException 
	 */
	public default boolean isAreaGenerated(AABB area) throws IOException {
		return isAreaGenerated(area.getMin().toInt(), area.getMax().toInt());
	}
	
	/**
	 * Returns true if and only if all chunks the given area is intersecting are fully generated and no world-generation or lighting has yet to be done.
	 * @param area The area to check
	 * @throws IOException 
	 */
	public default boolean isAreaGenerated(Vector3i blockMin, Vector3i blockMax) throws IOException {
		return isAreaGenerated(blockPosToChunkPos(blockMin), blockPosToChunkPos(blockMax));
	}
	
	/**
	 * Returns true if and only if all chunks in the given range are fully generated and no world-generation or lighting has yet to be done.
	 * @param area The area to check
	 * @throws IOException 
	 */
	public default boolean isAreaGenerated(Vector2i chunkMin, Vector2i chunkMax) throws IOException {
		for (int x = chunkMin.getX(); x <= chunkMax.getX(); x++) {
			for (int z = chunkMin.getY(); z <= chunkMax.getY(); z++) {
				if (!isChunkGenerated(new Vector2i(x, z))) return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Invalidates the complete chunk cache (if there is a cache), so that every chunk has to be reloaded from disk
	 */
	public void invalidateChunkCache();

	/**
	 * Invalidates the chunk from the chunk-cache (if there is a cache), so that the chunk has to be reloaded from disk
	 */
	public void invalidateChunkCache(Vector2i chunk);
	
	/**
	 * Returns the ChunkPosition for a BlockPosition 
	 */
	public default Vector2i blockPosToChunkPos(Vector3i block) {
		return new Vector2i(
			block.getX() >> 4,
			block.getZ() >> 4
		);
	}
	
}
