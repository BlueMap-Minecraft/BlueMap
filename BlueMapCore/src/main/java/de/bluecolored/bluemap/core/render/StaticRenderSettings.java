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
package de.bluecolored.bluemap.core.render;

import com.flowpowered.math.vector.Vector3i;

public class StaticRenderSettings implements RenderSettings {
	
	private boolean excludeFacesWithoutSunlight;
	private Vector3i min, max;
	private boolean renderEdges;
	
	public StaticRenderSettings(
			boolean excludeFacesWithoutSunlight,
			Vector3i min,
			Vector3i max,
			boolean renderEdges
			) {
		this.excludeFacesWithoutSunlight = excludeFacesWithoutSunlight;
		this.min = min;
		this.max = max;
		this.renderEdges = renderEdges;
	}

	@Override
	public boolean isExcludeFacesWithoutSunlight() {
		return excludeFacesWithoutSunlight;
	}
	
	@Override
	public Vector3i getMin() {
		return min;
	}
	
	@Override
	public Vector3i getMax() {
		return max;
	}
	
	@Override
	public boolean isRenderEdges() {
		return renderEdges;
	}
	
}