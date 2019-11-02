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

import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.mca.mapping.BlockProperties;
import de.bluecolored.bluemap.core.mca.mapping.LightData;
import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.BlockState;

public class MCABlock extends Block {

	private MCAWorld world;
	private BlockState blockState;
	private LightData lightData;
	private String biome;
	private BlockProperties properties;
	private Vector3i pos;
	
	public MCABlock(MCAWorld world, BlockState blockState, LightData lightData, String biome, BlockProperties properties, Vector3i pos) {
		this.world = world;
		this.blockState = blockState;
		this.lightData = lightData;
		this.biome = biome;
		this.properties = properties;
		this.pos = pos;
	}

	@Override
	public BlockState getBlock() {
		return blockState;
	}

	@Override
	public MCAWorld getWorld() {
		return world;
	}

	@Override
	public Vector3i getPosition() {
		return pos;
	}

	@Override
	public double getSunLightLevel() {
		return lightData.getSkyLight();
	}

	@Override
	public double getBlockLightLevel() {
		return lightData.getBlockLight();
	}

	@Override
	public boolean isCullingNeighborFaces() {
		return properties.isCulling();
	}
	
	@Override
	public boolean isOccludingNeighborFaces() {
		return properties.isOccluding();
	}

	@Override
	public String getBiome() {
		return biome;
	}
	
}
