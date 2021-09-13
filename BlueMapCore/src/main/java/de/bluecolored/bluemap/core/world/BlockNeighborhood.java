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
package de.bluecolored.bluemap.core.world;

import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;

public class BlockNeighborhood<T extends BlockNeighborhood<T>> extends ExtendedBlock<T> {

	private static final int DIAMETER = 8;
	private static final int DIAMETER_MASK = DIAMETER - 1;
	private static final int DIAMETER_SQUARED = DIAMETER * DIAMETER;

	private final ExtendedBlock<?>[] neighborhood;

	private int thisIndex;

	public BlockNeighborhood(ExtendedBlock<?> center) {
		super(center.getResourcePack(), center.getRenderSettings(), null, 0, 0, 0);
		copy(center);

		neighborhood = new ExtendedBlock[DIAMETER * DIAMETER * DIAMETER];
		init();
	}

	public BlockNeighborhood(ResourcePack resourcePack, RenderSettings renderSettings, World world, int x, int y, int z) {
		super(resourcePack, renderSettings, world, x, y, z);

		neighborhood = new ExtendedBlock[DIAMETER * DIAMETER * DIAMETER];
		init();
	}

	@Override
	protected void reset() {
		super.reset();

		this.thisIndex = -1;
	}

	private void init() {
		this.thisIndex = -1;
		for (int i = 0; i < neighborhood.length; i++) {
			neighborhood[i] = new ExtendedBlock<>(this.getResourcePack(), this.getRenderSettings(), null, 0, 0, 0);
		}
	}

	public ExtendedBlock<?> getNeighborBlock(int dx, int dy, int dz) {
		int i = neighborIndex(dx, dy, dz);
		if (i == thisIndex()) return this;
		return neighborhood[i].set(
				getWorld(),
				getX() + dx,
				getY() + dy,
				getZ() + dz
		);
	}

	private int thisIndex() {
		if (thisIndex == -1) thisIndex = neighborIndex(0, 0, 0);
		return thisIndex;
	}

	private int neighborIndex(int dx, int dy, int dz) {
		return ((getX() + dx) & DIAMETER_MASK) * DIAMETER_SQUARED +
			   ((getY() + dy) & DIAMETER_MASK) * DIAMETER +
			   ((getZ() + dz) & DIAMETER_MASK);
	}

}
