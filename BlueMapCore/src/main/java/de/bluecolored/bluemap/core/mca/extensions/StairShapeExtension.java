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
import java.util.HashSet;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Sets;

import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.util.Direction;
import de.bluecolored.bluemap.core.world.BlockState;

public class StairShapeExtension implements BlockStateExtension {

	private static final HashSet<String> AFFECTED_BLOCK_IDS = Sets.newHashSet(
			"minecraft:oak_stairs",
			"minecraft:cobblestone_stairs",
			"minecraft:brick_stairs",
			"minecraft:stone_brick_stairs",
			"minecraft:nether_brick_stairs",
			"minecraft:sandstone_stairs",
			"minecraft:spruce_stairs",
			"minecraft:birch_stairs",
			"minecraft:jungle_stairs",
			"minecraft:quartz_stairs",
			"minecraft:acacia_stairs",
			"minecraft:dark_oak_stairs",
			"minecraft:red_sandstone_stairs",
			"minecraft:purpur_stairs"
		);
	
	@Override
	public BlockState extend(MCAWorld world, Vector3i pos, BlockState state) {		
		try {
			Direction facing = Direction.fromString(state.getProperties().get("facing"));
			BlockState back = world.getBlockState(pos.add(facing.toVector()));
	
	        if (isStairs(back) && state.getProperties().get("half").equals(back.getProperties().get("half"))) {
				Direction backFacing = Direction.fromString(back.getProperties().get("facing"));
				
	            if (facing.getAxis() != backFacing.getAxis()){
	            	BlockState next = world.getBlockState(pos.add(backFacing.opposite().toVector()));
	            	
	            	if (!isStairs(next) || !isEqualStairs(state, next)) {
	            	
		                if (backFacing == facing.left()){
		        			return state.with("shape", "outer_left");
		                }

	        			return state.with("shape", "outer_right");
	            	}
	            }
	        }
	
	        BlockState front = world.getBlockState(pos.add(facing.opposite().toVector()));
	        
	        if (isStairs(front) && state.getProperties().get("half").equals(front.getProperties().get("half"))) {
				Direction frontFacing = Direction.fromString(front.getProperties().get("facing"));
				
	            if (facing.getAxis() != frontFacing.getAxis()){
	            	BlockState next = world.getBlockState(pos.add(frontFacing.toVector()));
					
	            	if (!isStairs(next) || !isEqualStairs(state, next)) {
		                if (frontFacing == facing.left()){
		        			return state.with("shape", "inner_left");
		                }

	        			return state.with("shape", "inner_right");
	            	}
	            }
	        }

			return state.with("shape", "straight");
		
		} catch (IllegalArgumentException | NullPointerException ex) {
			return state.with("shape", "straight");
		}
	}
	
	private boolean isStairs(BlockState state) {
		return AFFECTED_BLOCK_IDS.contains(state.getFullId());
	}
	
	private boolean isEqualStairs(BlockState stair1, BlockState stair2) {
		return 
				stair1.getProperties().get("facing").equals(stair2.getProperties().get("facing")) &&
				stair1.getProperties().get("half").equals(stair2.getProperties().get("half"));
	}

	@Override
	public Collection<String> getAffectedBlockIds() {
		return AFFECTED_BLOCK_IDS;
	}

}
