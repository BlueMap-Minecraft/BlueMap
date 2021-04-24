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
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.LightData;
import net.querz.nbt.CompoundTag;

import java.io.IOException;

public abstract class MCAChunk implements Chunk {

	private final int dataVersion;
	
	protected MCAChunk() {
		this.dataVersion = -1;
	}
	
	protected MCAChunk(CompoundTag chunkTag) {
		dataVersion = chunkTag.getInt("DataVersion");
	}

	@Override
	public abstract boolean isGenerated();
	
	public int getDataVersion() {
		return dataVersion;
	}
	
	public abstract BlockState getBlockState(Vector3i pos);
	
	public abstract LightData getLightData(Vector3i pos);
	
	public abstract Biome getBiome(int x, int y, int z);
	
	public static MCAChunk create(MCAWorld world, CompoundTag chunkTag, boolean ignoreMissingLightData) throws IOException {
		int version = chunkTag.getInt("DataVersion");
		
		if (version < 1400) return new ChunkAnvil112(chunkTag, ignoreMissingLightData, world.getBiomeIdMapper(), world.getBlockIdMapper(), world::getForgeBlockIdMapping);
		if (version < 2200) return new ChunkAnvil113(chunkTag, ignoreMissingLightData, world.getBiomeIdMapper());
		if (version < 2500) return new ChunkAnvil115(chunkTag, ignoreMissingLightData, world.getBiomeIdMapper());
		return new ChunkAnvil116(chunkTag, ignoreMissingLightData, world.getBiomeIdMapper());
	}

	public static MCAChunk empty() {
		return EmptyChunk.INSTANCE;
	}

}
