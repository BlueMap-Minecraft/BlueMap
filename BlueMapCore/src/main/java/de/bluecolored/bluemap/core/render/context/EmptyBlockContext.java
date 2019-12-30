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

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.util.AABB;
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.WorldChunk;

public class EmptyBlockContext implements ExtendedBlockContext {

	private static final EmptyBlockContext instance = new EmptyBlockContext();
	
	public static final Block AIR_BLOCK = new AirBlock();
	
	private EmptyBlockContext() {}
	
	@Override
	public Block getRelativeBlock(Vector3i direction) {
		return AIR_BLOCK;
	}

	@Override
	public Vector3i getPosition() {
		return Vector3i.ZERO;
	}
	
	public static ExtendedBlockContext instance() {
		return instance;
	}
	
	private static class AirBlock extends Block {

		private BlockState state = BlockState.AIR;
		
		@Override
		public BlockState getBlock() {
			return state;
		}

		@Override
		public World getWorld() {
			return new EmptyWorld();
		}

		@Override
		public Vector3i getPosition() {
			return Vector3i.ZERO;
		}

		@Override
		public double getSunLightLevel() {
			return 0d;
		}

		@Override
		public double getBlockLightLevel() {
			return 0d;
		}

		@Override
		public boolean isCullingNeighborFaces() {
			return false;
		}

		@Override
		public boolean isFlammable() {
			return false;
		}

		@Override
		public Biome getBiome() {
			return Biome.DEFAULT;
		}
		
	}
	
	private static class EmptyWorld implements World {

		private AABB bounds;
		
		public EmptyWorld() {
			this.bounds = new AABB(Vector3d.from(Double.POSITIVE_INFINITY), Vector3d.from(Double.NEGATIVE_INFINITY));
		}
		
		public EmptyWorld(AABB bounds){
			this.bounds = bounds;
		}
		
		@Override
		public World getWorld() {
			return this;
		}

		@Override
		public Block getBlock(Vector3i pos) {
			return new AirBlock();
		}

		@Override
		public AABB getBoundaries() {
			return bounds;
		}

		@Override
		public WorldChunk getWorldChunk(AABB boundaries) {
			return new EmptyWorld(boundaries);
		}

		@Override
		public boolean isGenerated() {
			return false;
		}

		@Override
		public String getName() {
			return "-empty-";
		}

		@Override
		public UUID getUUID() {
			return new UUID(0, 0);
		}

		@Override
		public int getSeaLevel() {
			return 63;
		}
		
		@Override
		public Vector3i getSpawnPoint() {
			return new Vector3i(0, 63, 0);
		}

		@Override
		public Collection<Vector2i> getChunkList(long modifiedSince) {
			return Collections.emptyList();
		}

		@Override
		public void invalidateChunkCache() {}

		@Override
		public void invalidateChunkCache(Vector2i chunk) {}
		
	}

}
