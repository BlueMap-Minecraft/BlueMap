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

import java.util.Collection;
import java.util.Set;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.world.BlockState;

public class RedstoneExtension implements BlockStateExtension {

	private static final Collection<String> AFFECTED_BLOCK_IDS = Lists.newArrayList(
			"minecraft:redstone_wire"
		);
	

	private static final Set<String> CONNECTIBLE = Sets.newHashSet(
			"minecraft:redstone_wire",
			"minecraft:unlit_redstone_torch",
			"minecraft:redstone_torch",
			"minecraft:stone_button",
			"minecraft:wooden_button",
			"minecraft:stone_button",
			"minecraft:lever",
			"minecraft:stone_pressure_plate",
			"minecraft:wooden_pressure_plate",
			"minecraft:light_weighted_pressure_plate",
			"minecraft:heavy_weighted_pressure_plate"
		);
	
	@Override
	public BlockState extend(MCAWorld world, Vector3i pos, BlockState state) {
		state = state
				.with("north", connection(world, pos, state, Direction.NORTH))
				.with("east", connection(world, pos, state, Direction.EAST))
				.with("south", connection(world, pos, state, Direction.SOUTH))
				.with("west", connection(world, pos, state, Direction.WEST));
		
		return state;
	}

	private String connection(MCAWorld world, Vector3i pos, BlockState state, Direction direction) {
		BlockState next = world.getBlockState(pos.add(direction.toVector()));
		if (CONNECTIBLE.contains(next.getId())) return "side";
		
		//TODO: up
		
		return "none";
	}

	@Override
	public Collection<String> getAffectedBlockIds() {
		return AFFECTED_BLOCK_IDS;
	}

}
