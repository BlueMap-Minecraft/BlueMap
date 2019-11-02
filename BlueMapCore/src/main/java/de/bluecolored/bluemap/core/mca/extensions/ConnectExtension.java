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
package de.bluecolored.bluemap.core.mca.extensions;

import java.util.Set;

import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.world.BlockState;

public abstract class ConnectExtension implements BlockStateExtension {
	
	@Override
	public BlockState extend(MCAWorld world, Vector3i pos, BlockState state) {
		return state
				.with("north", String.valueOf(connectsTo(world, pos.add(Direction.NORTH.toVector()))))
				.with("east", String.valueOf(connectsTo(world, pos.add(Direction.EAST.toVector()))))
				.with("south", String.valueOf(connectsTo(world, pos.add(Direction.SOUTH.toVector()))))
				.with("west", String.valueOf(connectsTo(world, pos.add(Direction.WEST.toVector()))));
	}

	public boolean connectsTo(MCAWorld world, Vector3i pos) {
		return connectsTo(world, pos, world.getBlockState(pos));
	}
	
	public boolean connectsTo(MCAWorld world, Vector3i pos, BlockState block) {
		return getAffectedBlockIds().contains(block.getFullId());
	}
	
	@Override
	public abstract Set<String> getAffectedBlockIds();
	
}
