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
package de.bluecolored.bluemap.core.map.mask;

import de.bluecolored.bluemap.core.map.renderstate.TileState;
import de.bluecolored.bluemap.core.util.Tristate;

public interface Mask {

    Mask NONE = new Mask() {
        @Override
        public boolean test(int x, int y, int z) {
            return false;
        }

        @Override
        public Tristate test(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            return Tristate.FALSE;
        }

        @Override
        public boolean isEdge(int minX, int minZ, int maxX, int maxZ) {
            return false;
        }
    };

    Mask ALL = NONE.inverted();

    /**
     * Returns {@code true} if the mask applies at the given point and {@code false} if not.
     */
    boolean test(int x, int y, int z);

    /**
     * Returns {@link Tristate#TRUE} if the entire region tests to {@code true}, {@link Tristate#FALSE} if the entire region tests to
     * {@code false} and {@link Tristate#UNDEFINED} or if unknown or part of it tests to true and other parts test to false.<br>
     * <br>
     * This is used to improve performance so {@link #test(int, int, int)} does not always have to be repeatedly called for every single block
     * while rendering.<br>
     * <br>
     * It is valid to approximate this. E.g.: if a precise collision-check is too complex or expensive, a simple bounding-box check
     * - where the collisions return UNDEFINED and non-collisions return FALSE - could be enough.
     */
    Tristate test(
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ
    );

    /**
     *  This is used to determine the resulting {@link TileState} after rendering.
     *  If this returns {@code true} the tile will be marked as a "map-edge".
     *  The tile will be updated if this function evaluates differently on the next test.<br>
     *  <br>
     *  Implementations should in most cases ignore the masks y and only return {@code true} for
     *  the edge of the mask if it were "projected" onto the xz-plane.
     */
    boolean isEdge(
            int minX, int minZ,
            int maxX, int maxZ
    );

    /**
     * Returns a mask-instance for the given area. The returned mask is only guaranteed to be equal to this mask in the
     * defined area. That way it might be optimized and do checks more performantly.
     */
    default Mask submask(
            int minX, int minY, int minZ,
            int maxX, int maxY, int maxZ
    ) {
        Tristate test = test(minX, minY, minZ, maxX, maxY, maxZ);
        if (test == Tristate.TRUE) return ALL;
        if (test == Tristate.FALSE) return NONE;
        return this;
    }

    /**
     * Returns an inverted view of this mask
     */
    default Mask inverted() {
        return new Mask() {
            @Override
            public boolean test(int x, int y, int z) {
                return !Mask.this.test(x, y, z);
            }

            @Override
            public Tristate test(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
                return Mask.this.test(minX, minY, minZ, maxX, maxY, maxZ).negated();
            }

            @Override
            public boolean isEdge(int minX, int minZ, int maxX, int maxZ) {
                return Mask.this.isEdge(minX, minZ, maxX, maxZ);
            }

            @Override
            public Mask inverted() {
                return Mask.this;
            }
        };
    }

}
