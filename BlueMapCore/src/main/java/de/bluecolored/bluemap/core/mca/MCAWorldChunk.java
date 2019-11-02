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
package de.bluecolored.bluemap.core.mca;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.util.AABB;
import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.ChunkNotGeneratedException;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.WorldChunk;

public class MCAWorldChunk implements WorldChunk {

	private MCAWorld world;
	private AABB boundaries, extendedBounds;
	
	public MCAWorldChunk(MCAWorld world, AABB boundaries) {
		this.world = world;
		this.boundaries = boundaries;
		this.extendedBounds = boundaries.expand(2, 2, 2);
	}
	
	@Override
	public World getWorld() {
		return world;
	}

	@Override
	public Block getBlock(Vector3i pos) throws ChunkNotGeneratedException {
		return world.getBlock(pos);
	}

	@Override
	public AABB getBoundaries() {
		return boundaries;
	}

	@Override
	public WorldChunk getWorldChunk(AABB boundaries) {
		return new MCAWorldChunk(world, boundaries);
	}

	@Override
	public boolean isGenerated() {
		
		//check one more block in every direction to make sure that extended block states can be generated!
		Vector2i minChunk = MCAWorld.blockToChunk(extendedBounds.getMin().toInt()); 
		Vector2i maxChunk = MCAWorld.blockToChunk(extendedBounds.getMax().toInt());
		
		for (int x = minChunk.getX(); x <= maxChunk.getX(); x++) {
			for (int z = minChunk.getY(); z <= maxChunk.getY(); z++) {
				if (!world.isChunkGenerated(new Vector2i(x, z))) {
					return false;
				}
			}
		}
		
		return true;
	}

}
