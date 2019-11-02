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

public class StaticRenderSettings implements RenderSettings {
	
	private float ambientOcclusion;
	private boolean excludeFacesWithoutSunlight;
	private float lightShade;
	private int maxY, minY, sliceY;
	
	public StaticRenderSettings(
			float ambientOcclusion,
			boolean excludeFacesWithoutSunlight,
			float ligheShade,
			int maxY,
			int minY,
			int sliceY
			) {
		this.ambientOcclusion = ambientOcclusion;
		this.excludeFacesWithoutSunlight = excludeFacesWithoutSunlight;
		this.lightShade = ligheShade;
		this.maxY = maxY;
		this.minY = minY;
		this.sliceY = sliceY;
	}

	@Override
	public float getAmbientOcclusionStrenght() {
		return ambientOcclusion;
	}

	@Override
	public boolean isExcludeFacesWithoutSunlight() {
		return excludeFacesWithoutSunlight;
	}

	@Override
	public float getLightShadeMultiplier() {
		return lightShade;
	}

	@Override
	public int getMaxY() {
		return maxY;
	}

	@Override
	public int getMinY() {
		return minY;
	}

	@Override
	public int getSliceY() {
		return sliceY;
	}
	
}