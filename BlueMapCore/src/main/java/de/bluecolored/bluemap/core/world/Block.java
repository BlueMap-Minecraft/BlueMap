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
import de.bluecolored.bluemap.core.util.Direction;

public class Block {

	private World world;
	private int x, y, z;

	private Chunk chunk;

	private BlockState blockState;
	private BlockProperties properties;
	private LightData lightData;
	private Biome biome;
	
	private int sunLight;
	private int blockLight;

	private final transient LightData tempLight;
	
	public Block(World world, int x, int y, int z) {
		tempLight = new LightData(0, 0);

		set(world, x, y, z);
	}

	public Block set(World world, int x, int y, int z) {
		if (this.x == x && this.y == y && this.z == z && this.world == world) return this;

		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;

		reset();

		return this;
	}

	public Block set(int x, int y, int z) {
		if (this.x == x && this.y == y && this.z == z) return this;

		this.x = x;
		this.y = y;
		this.z = z;

		reset();

		return this;
	}

	private void reset() {
		this.chunk = null;

		this.blockState = null;
		this.properties = null;
		this.lightData = new LightData(-1, -1);
		this.biome = null;

		this.blockLight = -1;
		this.sunLight = -1;
	}

	public Block add(int dx, int dy, int dz) {
		return set(x + dx, y + dy, z + dz);
	}

	public Block copy(Block source) {
		return set(source.world, source.x, source.y, source.z);
	}

	public World getWorld() {
		return world;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public Chunk getChunk() {
		if (chunk == null) chunk = world.getChunkAtBlock(x, y, z);
		return chunk;
	}

	public BlockState getBlockState() {
		if (blockState == null) blockState = getChunk().getBlockState(x, y, z);
		return blockState;
	}

	public BlockProperties getProperties() {
		if (properties == null) properties = world.getBlockProperties(getBlockState());
		return properties;
	}

	public LightData getLightData() {
		if (lightData.getSkyLight() < 0) getChunk().getLightData(x, y, z, lightData);
		return lightData;
	}

	public Biome getBiome() {
		if (biome == null) biome = getChunk().getBiome(x, y, z);
		return biome;
	}

	public int getSunLightLevel() {
		return getLightData().getSkyLight();
	}
	
	public int getBlockLightLevel() {
		return getLightData().getBlockLight();
	}

	public boolean isCullingNeighborFaces() {
		return getProperties().isCulling();
	}

	public boolean isFlammable() {
		return getProperties().isFlammable();
	}
	
	public boolean isOccludingNeighborFaces(){
		return getProperties().isOccluding();
	}

	/**
	 * This is internally used for light rendering
	 * It is basically the sun light that is projected onto adjacent faces
	 */
	public int getPassedSunLight() {
		if (sunLight < 0) calculateLight();
		return sunLight;
	}
	
	/**
	 * This is internally used for light rendering
	 * It is basically the block light that is projected onto adjacent faces
	 */
	public int getPassedBlockLight() {
		if (blockLight < 0) calculateLight();
		return blockLight;
	}
	
	private void calculateLight() {
		sunLight = getSunLightLevel();
		blockLight = getBlockLightLevel();
		
		if (blockLight > 0 || sunLight > 0) return;

		Vector3i dirV;
		int nx, ny, nz;
		for (Direction direction : Direction.values()) {
			dirV = direction.toVector();
			nx = dirV.getX() + x;
			ny = dirV.getY() + y;
			nz = dirV.getZ() + z;

			world.getLightData(nx, ny, nz, tempLight);

			sunLight = Math.max(tempLight.getSkyLight(), sunLight);
			blockLight = Math.max(tempLight.getBlockLight(), blockLight);
		}
	}

	@Override
	public String toString() {
		return "Block{" +
			   "world=" + world +
			   ", x=" + x +
			   ", y=" + y +
			   ", z=" + z +
			   ", sunLight=" + sunLight +
			   ", blockLight=" + blockLight +
			   '}';
	}

}
