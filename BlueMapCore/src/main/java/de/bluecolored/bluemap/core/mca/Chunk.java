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

import java.io.IOException;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.LightData;
import net.querz.nbt.CompoundTag;

public abstract class Chunk {
	
	private final MCAWorld world;
	private final Vector2i chunkPos;
	
	protected Chunk(MCAWorld world, CompoundTag chunkTag) {
		this.world = world;
		
		CompoundTag levelData = chunkTag.getCompoundTag("Level");
		
		chunkPos = new Vector2i(
				levelData.getInt("xPos"),
				levelData.getInt("zPos")
			);
	}
	
	public abstract boolean isGenerated();
	
	public Vector2i getChunkPos() {
		return chunkPos;
	}
	
	public MCAWorld getWorld() {
		return world;
	}
	
	public abstract BlockState getBlockState(Vector3i pos);
	
	public abstract LightData getLightData(Vector3i pos);
	
	public abstract Biome getBiome(Vector3i pos);
	
	public static Chunk create(MCAWorld world, CompoundTag chunkTag) throws IOException {
		int version = chunkTag.getInt("DataVersion");
		
		if (version <= 1343) return new ChunkAnvil112(world, chunkTag);
		return new ChunkAnvil113(world, chunkTag);
	}
	
}
