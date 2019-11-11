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
import com.google.common.base.MoreObjects;

import de.bluecolored.bluemap.core.render.context.BlockContext;
import de.bluecolored.bluemap.core.util.Direction;

public abstract class Block {

	private float sunLight;
	private float blockLight;
	
	public Block() {
		sunLight = -1;
		blockLight = -1;
	}
	
	public abstract BlockState getBlock();
	
	public abstract World getWorld();
	
	public abstract Vector3i getPosition();
	
	public abstract double getSunLightLevel();
	
	public abstract double getBlockLightLevel();

	public abstract boolean isCullingNeighborFaces();
	
	public boolean isOccludingNeighborFaces(){
		return isCullingNeighborFaces();
	}
	
	public abstract String getBiome();

	/**
	 * This is internally used for light rendering
	 * It is basically the sun light that is projected onto adjacent faces
	 */
	public float getPassedSunLight(BlockContext context) {
		if (sunLight < 0) calculateLight(context);
		return sunLight;
	}
	
	/**
	 * This is internally used for light rendering
	 * It is basically the block light that is projected onto adjacent faces
	 */
	public float getPassedBlockLight(BlockContext context) {
		if (blockLight < 0) calculateLight(context);
		return blockLight;
	}
	
	private void calculateLight(BlockContext context) {
		sunLight = (float) getSunLightLevel();
		blockLight = (float) getBlockLightLevel();
		
		if (blockLight > 0 || sunLight > 0) return;
		
		for (Direction direction : Direction.values()) {
			Block neighbor = context.getRelativeBlock(direction);
			sunLight = (float) Math.max(neighbor.getSunLightLevel(), sunLight);
			blockLight = (float) Math.max(neighbor.getBlockLightLevel(), blockLight);
		}
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("pos", getPosition())
			.add("biome", getBiome())
			.add("blocklight", getBlockLightLevel())
			.add("sunlight", getSunLightLevel())
			.toString();
	}

}
