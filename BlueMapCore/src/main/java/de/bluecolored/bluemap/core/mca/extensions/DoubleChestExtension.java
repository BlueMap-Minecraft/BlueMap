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

import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.world.BlockState;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DoubleChestExtension implements BlockStateExtension {

	private static final Set<String> AFFECTED_BLOCK_IDS = new HashSet<>(Arrays.asList(
			"minecraft:chest",
			"minecraft:trapped_chest"
		));
	
	@Override
	public BlockState extend(MCAWorld world, Vector3i pos, BlockState state) {
		Direction dir = Direction.fromString(state.getProperties().getOrDefault("facing", "north"));
		
		BlockState left = world.getBlockState(pos.add(dir.left().toVector()));
		if (left.getFullId().equals(state.getFullId())) return state.with("type", "right");

		BlockState right = world.getBlockState(pos.add(dir.right().toVector()));
		if (right.getFullId().equals(state.getFullId())) return state.with("type", "left");
		
		return state.with("type", "single");
	}

	@Override
	public Set<String> getAffectedBlockIds() {
		return AFFECTED_BLOCK_IDS;
	}

}
