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

public interface RenderSettings {
	
	/**
	 * The strenght of ao-shading calculated for each vertex.<br>
	 * A value of 0 turns off ao.<br>
	 * The value represents the amount that each occluding face subtracts of the light-multiplier. (There are at most 3 occluding faces)
	 */
	default float getAmbientOcclusionStrenght() {
		return 0.25f;
	}

	/**
	 * Whether faces that have a sky-light-value of 0 will be rendered or not.
	 */
	default boolean isExcludeFacesWithoutSunlight() {
		return true;
	}
	
	/**
	 * A multiplier to how much faces are shaded due to their light value<br>
	 * This can be used to make sure blocks with a light value of 0 are not pitch black
	 */
	default float getLightShadeMultiplier() {
		return 0.8f;
	}
	
	/**
	 * The maximum height of rendered blocks
	 */
	default int getMaxY() {
		return Integer.MAX_VALUE;
	}

	/**
	 * The minimum height of rendered blocks
	 */
	default int getMinY() {
		return 0;
	}
	
	/**
	 * The same as the maximum height, but blocks that are above this value are treated as AIR.<br>
	 * This leads to the top-faces being rendered instead of them being culled.
	 */
	default int getSliceY() {
		return Integer.MAX_VALUE;
	}
	
	

	default RenderSettings copy() {
		return new StaticRenderSettings(
				getAmbientOcclusionStrenght(),
				isExcludeFacesWithoutSunlight(),
				getLightShadeMultiplier(),
				getMaxY(),
				getMinY(),
				getSliceY()
			);
	}
	
}
