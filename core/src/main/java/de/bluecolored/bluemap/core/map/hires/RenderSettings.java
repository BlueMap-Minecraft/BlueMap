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

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.map.mask.Mask;
import de.bluecolored.bluemap.core.util.Grid;

import java.util.function.Predicate;

public interface RenderSettings {

    /**
     * The y-level below which "caves" will not be rendered
     */
    int getRemoveCavesBelowY();

    /**
     * The y-level relative to the ocean-floor heightmap below which caves will not be rendered
     */
    int getCaveDetectionOceanFloor();

    /**
     * If blocklight should be used instead of skylight to detect "caves"
     */
    boolean isCaveDetectionUsesBlockLight();

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

    Mask getRenderMask();

    default boolean isInsideRenderBoundaries(int x, int z) {
        return getRenderMask().test(x, Integer.MIN_VALUE, z, x, Integer.MAX_VALUE, z).getOr(true);
    }

    default boolean isInsideRenderBoundaries(int x, int y, int z) {
        return getRenderMask().test(x, y, z);
    }

    default boolean isInsideRenderBoundaries(Vector2i cell, Grid grid, boolean allowPartiallyIncludedCells) {
        return getRenderMask().test(
                grid.getCellMinX(cell.getX()), Integer.MIN_VALUE, grid.getCellMinY(cell.getY()),
                grid.getCellMaxX(cell.getX()), Integer.MAX_VALUE, grid.getCellMaxY(cell.getY())
        ).getOr(allowPartiallyIncludedCells);
    }

    /**
     * Returns a predicate which is filtering out all cells of a {@link Grid}
     * that are outside the render boundaries.
     */
    default Predicate<Vector2i> getCellRenderBoundariesFilter(Grid grid, boolean allowPartiallyIncludedCells) {
        return cell -> isInsideRenderBoundaries(cell, grid, allowPartiallyIncludedCells);
    }

    boolean isSaveHiresLayer();

    boolean isRenderTopOnly();

}
