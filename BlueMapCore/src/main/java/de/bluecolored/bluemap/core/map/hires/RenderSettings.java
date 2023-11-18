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
package de.bluecolored.bluemap.core.map.hires;

import com.flowpowered.math.vector.Vector3i;

public interface RenderSettings {

    Vector3i DEFAULT_MIN = Vector3i.from(Integer.MIN_VALUE);
    Vector3i DEFAULT_MAX = Vector3i.from(Integer.MAX_VALUE);

    /**
     * The y-level below which "caves" will not be rendered
     */
    int getRemoveCavesBelowY();

    /**
     * The y-level relative to the ocean-floor heightmap below which caves will not be rendered
     */
    int getCaveDetectionOceanFloor();

    /**
     * If blocklight should be used instead of sky light to detect "caves"
     */
    boolean isCaveDetectionUsesBlockLight();

    /**
     * The minimum position of blocks to render
     */
    default Vector3i getMinPos() {
        return DEFAULT_MIN;
    }

    /**
     * The maximum position of blocks to render
     */
    default Vector3i getMaxPos() {
        return DEFAULT_MAX;
    }

    /**
     * The (default) ambient light of this world (0-1)
     */
    float getAmbientLight();

    /**
     * The same as the maximum height, but blocks that are above this value are treated as AIR.<br>
     * This leads to the top-faces being rendered instead of them being culled.
     */
    default boolean isRenderEdges() {
        return true;
    }

    default boolean isIgnoreMissingLightData() {
        return false;
    }

    default boolean isInsideRenderBoundaries(int x, int z) {
        Vector3i min = getMinPos();
        Vector3i max = getMaxPos();

        return
                x >= min.getX() &&
                x <= max.getX() &&
                z >= min.getZ() &&
                z <= max.getZ();
    }

    default boolean isInsideRenderBoundaries(int x, int y, int z) {
        Vector3i min = getMinPos();
        Vector3i max = getMaxPos();

        return
                x >= min.getX() &&
                x <= max.getX() &&
                z >= min.getZ() &&
                z <= max.getZ() &&
                y >= min.getY() &&
                y <= max.getY();
    }

    boolean isSaveHiresLayer();

}
