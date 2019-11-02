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

import java.util.Collection;
import java.util.UUID;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

/**
 * Represents a World on the Server<br>
 * <br>
 * <i>The implementation of this class has to be thread-save!</i><br>
 */
public interface World extends WorldChunk {

	String getName();
	
	UUID getUUID();
	
	int getSeaLevel();
	
	Vector3i getSpawnPoint();
	
	/**
	 * Returns itself
	 */
	@Override
	default World getWorld() {
		return this;
	}
	
	/**
	 * Always returns false
	 */
	@Override
	default boolean isGenerated() {
		return false;
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
	
}
