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
import java.util.Map.Entry;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Lists;

import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.world.BlockState;

public class DoorExtension implements BlockStateExtension {

	private static final Collection<String> AFFECTED_BLOCK_IDS = Lists.newArrayList(
			"minecraft:oak_door",
			"minecraft:iron_door",
			"minecraft:spruce_door",
			"minecraft:birch_door",
			"minecraft:jungle_door",
			"minecraft:acacia_door",
			"minecraft:dark_oak_door"
		);
	
	@Override
	public BlockState extend(MCAWorld world, Vector3i pos, BlockState state) {
		BlockState otherDoor;
		
		if (state.getProperties().get("half").equals("lower")) {
			otherDoor = world.getBlockState(pos.add(Direction.UP.toVector()));
		} else {
			otherDoor = world.getBlockState(pos.add(Direction.DOWN.toVector()));
		}
		
		//copy all properties from the other door
		for (Entry<String, String> prop : otherDoor.getProperties().entrySet()) {
			if (!state.getProperties().containsKey(prop.getKey())) {
				state = state.with(prop.getKey(), prop.getValue());
			}
		}
		
		return state;
	}

	@Override
	public Collection<String> getAffectedBlockIds() {
		return AFFECTED_BLOCK_IDS;
	}

}
