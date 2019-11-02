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

import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.util.AABB;

/**
 * Represents a chunk of a world.<br>
 * <br>
 * <i>The implementation of this class has to be thread-save!</i><br>
 */
public interface WorldChunk {

	/**
	 * Returns the top-level World of this WorldChunk,
	 * If this WorldChunk is already a World, the method returns the same instance (<code>return this;</code>). 
	 */
	World getWorld(); 
	
	/**
	 * Returns the Block on the specified position.<br>
	 * <br>
	 * <i>(The implementation should not invoke the generation of new Terrain, it should rather throw a {@link ChunkNotGeneratedException} if a not generated block is requested)</i><br>
	 */
	Block getBlock(Vector3i pos) throws ChunkNotGeneratedException;
	
	/**
	 * Returns the Block on the specified position.<br>
	 * <br>
	 * <i>(The implementation should not invoke the generation of new Terrain, it should rather throw a {@link ChunkNotGeneratedException} if a not generated block is requested)</i><br>
	 */
	default Block getBlock(int x, int y, int z) throws ChunkNotGeneratedException {
		return getBlock(new Vector3i(x, y, z));
	}
	
	/**
	 * Returns true if this WorldChunk contains the given position.
	 */
	default boolean containsBlock(Vector3i pos){
		return getBoundaries().contains(pos);
	}
	
	/**
	 * Returns the boundaries of the WorldChunk.<br>
	 */
	AABB getBoundaries();
	
	/**
	 * Returns a smaller part of this WorldChunk<br>
	 * <br>
	 * This is used to give the implementation an easy way to optimize thread-save access to this world-chunk.<br>
	 * The {@link #getBlock} method is and should be used in favour to {@link World#getBlock}.<br> 
	 */
	WorldChunk getWorldChunk(AABB boundaries);
	
	/**
	 * Returns true if the complete WorldChunk is generated and populated by Minecraft.<br>
	 */
	boolean isGenerated();
	
}
