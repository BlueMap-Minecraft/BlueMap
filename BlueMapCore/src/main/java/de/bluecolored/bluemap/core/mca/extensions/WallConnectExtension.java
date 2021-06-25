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
import java.util.Objects;
import java.util.Set;

public class WallConnectExtension extends ConnectSameOrFullBlockExtension {

	private static final HashSet<String> AFFECTED_BLOCK_IDS = new HashSet<>(Arrays.asList(
			"minecraft:cobblestone_wall",
			"minecraft:mossy_cobblestone_wall"
		));
	
	@Override
	public BlockState extend(MCAWorld world, Vector3i pos, BlockState state) {
		state = super.extend(world, pos, state);
		
		if (
				Objects.equals(state.getProperties().get("north"), state.getProperties().get("south")) &&
				Objects.equals(state.getProperties().get("east"), state.getProperties().get("west")) &&
				!Objects.equals(state.getProperties().get("north"), state.getProperties().get("east")) &&
				!connectsTo(world, pos.add(Direction.UP.toVector()))
		) {
			return state.with("up", "false");
		} else {
			return state.with("up", "true");
		}
	}
	
	@Override
	public Set<String> getAffectedBlockIds() {
		return AFFECTED_BLOCK_IDS;
	}

}
