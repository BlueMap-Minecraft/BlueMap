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
import com.google.common.collect.Sets;

import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.world.BlockState;

public class RedstoneExtension implements BlockStateExtension {

	private static final Set<String> AFFECTED_BLOCK_IDS = Sets.newHashSet(
			"minecraft:redstone_wire"
		);
	

	private static final Set<String> CONNECTIBLE = Sets.newHashSet(
			"minecraft:redstone_wire",
			"minecraft:redstone_wall_torch",
			"minecraft:redstone_torch",
			"minecraft:stone_button",
			"minecraft:oak_button",
			"minecraft:stone_button",
			"minecraft:lever",
			"minecraft:stone_pressure_plate",
			"minecraft:oak_pressure_plate",
			"minecraft:light_weighted_pressure_plate",
			"minecraft:heavy_weighted_pressure_plate"
		);
	
	@Override
	public BlockState extend(MCAWorld world, Vector3i pos, BlockState state) {
		BlockState up = world.getBlockState(pos.add(0, 1, 0));
		boolean upBlocking = !up.equals(BlockState.AIR);
		
		state = state
				.with("north", connection(world, pos, upBlocking, Direction.NORTH))
				.with("east", connection(world, pos, upBlocking, Direction.EAST))
				.with("south", connection(world, pos, upBlocking, Direction.SOUTH))
				.with("west", connection(world, pos, upBlocking, Direction.WEST));
		
		return state;
	}

	private String connection(MCAWorld world, Vector3i pos, boolean upBlocking, Direction direction) {
		Vector3i directionVector = direction.toVector();
		
		BlockState next = world.getBlockState(pos.add(directionVector));
		if (CONNECTIBLE.contains(next.getFullId())) return "side";
		
		if (next.equals(BlockState.AIR)) {
			BlockState nextdown = world.getBlockState(pos.add(directionVector.getX(), directionVector.getY() - 1, directionVector.getZ()));
			if (nextdown.getFullId().equals("minecraft:redstone_wire")) return "side";
		}
		
		if (!upBlocking) {
			BlockState nextup = world.getBlockState(pos.add(directionVector.getX(), directionVector.getY() + 1, directionVector.getZ()));
			if (nextup.getFullId().equals("minecraft:redstone_wire")) return "up";
		}
		
		return "none";
	}

	@Override
	public Set<String> getAffectedBlockIds() {
		return AFFECTED_BLOCK_IDS;
	}

}
