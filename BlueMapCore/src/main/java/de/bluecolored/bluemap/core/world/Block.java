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

import de.bluecolored.bluemap.core.util.Direction;

public class Block {

	private World world;
	private BlockState blockState;
	private LightData lightData;
	private Biome biome;
	private BlockProperties properties;
	private Vector3i pos;
	
	private float sunLight;
	private float blockLight;
	
	public Block(World world, BlockState blockState, LightData lightData, Biome biome, BlockProperties properties, Vector3i pos) {
		this.world = world;
		this.blockState = blockState;
		this.lightData = lightData;
		this.biome = biome;
		this.properties = properties;
		this.pos = pos;
		
		this.sunLight = -1;
		this.blockLight = -1;
	}
	
	public BlockState getBlockState() {
		return blockState;
	}
	
	public World getWorld() {
		return world;
	}
	
	public Vector3i getPosition() {
		return pos;
	}
	
	public double getSunLightLevel() {
		return lightData.getSkyLight();
	}
	
	public double getBlockLightLevel() {
		return lightData.getBlockLight();
	}

	public boolean isCullingNeighborFaces() {
		return properties.isCulling();
	}

	public boolean isFlammable() {
		return properties.isFlammable();
	}
	
	public boolean isOccludingNeighborFaces(){
		return properties.isOccluding();
	}
	
	public Biome getBiome() {
		return biome;
	}

	/**
	 * This is internally used for light rendering
	 * It is basically the sun light that is projected onto adjacent faces
	 */
	public float getPassedSunLight() {
		if (sunLight < 0) calculateLight();
		return sunLight;
	}
	
	/**
	 * This is internally used for light rendering
	 * It is basically the block light that is projected onto adjacent faces
	 */
	public float getPassedBlockLight() {
		if (blockLight < 0) calculateLight();
		return blockLight;
	}
	
	private void calculateLight() {
		sunLight = (float) getSunLightLevel();
		blockLight = (float) getBlockLightLevel();
		
		if (blockLight > 0 || sunLight > 0) return;
		
		for (Direction direction : Direction.values()) {
			Block neighbor = getRelativeBlock(direction);
			sunLight = (float) Math.max(neighbor.getSunLightLevel(), sunLight);
			blockLight = (float) Math.max(neighbor.getBlockLightLevel(), blockLight);
		}
	}
	
	public Block getRelativeBlock(int x, int y, int z) {
		Vector3i pos = getPosition().add(x, y, z);
		return getWorld().getBlock(pos);
	}
	
	public Block getRelativeBlock(Vector3i direction) {
		Vector3i pos = getPosition().add(direction);
		return getWorld().getBlock(pos);
	}
	
	public Block getRelativeBlock(Direction direction){
		return getRelativeBlock(direction.toVector());
	}
	
	public void setWorld(World world) {
		this.world = world;
	}

	public void setBlockState(BlockState blockState) {
		this.blockState = blockState;
	}

	public void setLightData(LightData lightData) {
		this.lightData = lightData;
		
		this.blockLight = -1f;
		this.sunLight = -1f;
	}

	public void setBiome(Biome biome) {
		this.biome = biome;
	}

	public void setProperties(BlockProperties properties) {
		this.properties = properties;
	}

	public void setPos(Vector3i pos) {
		this.pos = pos;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("pos", getPosition())
			.add("biome", getBiome())
			.add("blocklight", getBlockLightLevel())
			.add("sunlight", getSunLightLevel())
			.add("state", getBlockState())
			.toString();
	}

}
