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
package de.bluecolored.bluemap.core.render.context;

import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.world.Block;

/**
 * A BlockContext that has a range of TWO blocks instead of one
 */
public interface ExtendedBlockContext extends BlockContext {

	/**
	 * This returns neighbour blocks.<br>
	 * The distance can not be larger than two blocks in each direction!<br>
	 */
	Block getRelativeBlock(Vector3i direction);

	/**
	 * This returns neighbour blocks.<br>
	 * The distance can not be larger than two blocks in each direction!<br>
	 */
	default Block getRelativeBlock(int x, int y, int z){
		return getRelativeBlock(new Vector3i(x, y, z));
	}
	
	/**
	 * Returns a relative view of this ExtendedBlockContext!
	 * The distance can not be larger than two blocks in each direction!<br>
	 */
	default BlockContext getRelativeView(final Vector3i relative) {
		final ExtendedBlockContext parent = this;
		
		return new BlockContext() {
			
			@Override
			public Block getRelativeBlock(Vector3i direction) {
				return parent.getRelativeBlock(direction.add(relative));
			}
			
			@Override
			public Vector3i getPosition() {
				return parent.getPosition().add(relative);
			}
		};
		
	}
	
}
