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
package de.bluecolored.bluemap.core.render.context;

import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.ChunkNotGeneratedException;
import de.bluecolored.bluemap.core.world.WorldChunk;

public class WorldChunkBlockContext implements ExtendedBlockContext {

	private Vector3i blockPos;
	private WorldChunk chunk;
	
	/**
	 * A BlockContext backed by a WorldChunk.
	 * 
	 * This Context assumes that the world-chunk is generated around that block-position.
	 * If the given world chunk is not generated, using this context will result in a RuntimeException!
	 */
	public WorldChunkBlockContext(WorldChunk worldChunk, Vector3i blockPos) {
		this.blockPos = blockPos;
		this.chunk = worldChunk;
	}

	@Override
	public Vector3i getPosition() {
		return blockPos;
	}
	
	@Override
	public Block getRelativeBlock(int x, int y, int z) {
		Vector3i pos = blockPos.add(x, y, z);
		return getBlock(pos);
	}
	
	@Override
	public Block getRelativeBlock(Vector3i direction) {
		Vector3i pos = blockPos.add(direction);
		return getBlock(pos);
	}
	
	protected Block getBlock(Vector3i position) {
		if (!chunk.containsBlock(position)) {
			return EmptyBlockContext.AIR_BLOCK;
		}
		
		try {
			return chunk.getBlock(position);
		} catch (ChunkNotGeneratedException e) {
			//we assume the chunk being generated
			throw new RuntimeException(e);
		}
	}
	
}
