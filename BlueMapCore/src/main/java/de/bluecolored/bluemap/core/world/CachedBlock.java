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

/**
 * This class wraps another Block to cache all getters.<br>
 * The implementation <b>can</b> use this to make sure all underlying getters are only called once and cached-data is used on the second call.  
 */
public class CachedBlock extends Block {

	private Block block;
	
	private BlockState state;
	private World world;
	private Vector3i position;
	private double sunLight, blockLight;
	private String biome;
	
	private boolean isCullingCached;
	private boolean isCulling;

	private boolean isOccludingCached;
	private boolean isOccluding;
	
	private CachedBlock(Block block) {
		this.block = block;
		
		this.state = null;
		this.world = null;
		this.position = null;
		this.sunLight = -1;
		this.blockLight = -1;
		
		this.isCullingCached = false;
		this.isCulling = false;
		
		this.isOccludingCached = false;
		this.isOccluding = false;
	}

	@Override
	public BlockState getBlock() {
		if (state == null) state = block.getBlock();
		return state;
	}

	@Override
	public World getWorld() {
		if (world == null) world = block.getWorld();
		return world;
	}

	@Override
	public Vector3i getPosition() {
		if (position == null) position = block.getPosition();
		return position;
	}

	@Override
	public double getSunLightLevel() {
		if (sunLight == -1) sunLight = block.getSunLightLevel();
		return sunLight;
	}

	@Override
	public double getBlockLightLevel() {
		if (blockLight == -1) blockLight = block.getBlockLightLevel();
		return blockLight;
	}

	@Override
	public boolean isCullingNeighborFaces() {
		if (!isCullingCached){
			isCulling = block.isCullingNeighborFaces();
			isCullingCached = true;
		}
		
		return isCulling;
	}
	
	@Override
	public boolean isOccludingNeighborFaces() {
		if (!isOccludingCached){
			isOccluding = block.isOccludingNeighborFaces();
			isOccludingCached = true;
		}
		
		return isOccluding;
	}

	@Override
	public String getBiome() {
		if (biome == null){
			biome = block.getBiome();
		}
		
		return biome;
	}
	
	public static CachedBlock of(Block block){
		if (block instanceof CachedBlock) return (CachedBlock) block;
		
		return new CachedBlock(block);
	}

}
