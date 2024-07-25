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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

public interface DimensionType {

    DimensionType OVERWORLD = new Builtin(
            true,
            true,
            false,
            0f,
            -64,
            384,
            null,
            1.0
    );
    DimensionType OVERWORLD_CAVES = new Builtin(
            true,
            true,
            true,
            0,
            -64,
            384,
            null,
            1.0
    );
    DimensionType NETHER = new Builtin(
            false,
            false,
            true,
            0.1f,
            0,
            256,
            6000L,
            8.0
    );
    DimensionType END = new Builtin(
            false,
            false,
            false,
            0,
            0,
            256,
            18000L,
            1.0
    );

    boolean isNatural();

    boolean hasSkylight();

    boolean hasCeiling();

    float getAmbientLight();

    int getMinY();

    int getHeight();

    Long getFixedTime();

    double getCoordinateScale();

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Builtin implements DimensionType {

        private final boolean natural;
        @Accessors(fluent = true) private final boolean hasSkylight;
        @Accessors(fluent = true) private final boolean hasCeiling;
        private final float ambientLight;
        private final int minY;
        private final int height;
        private final Long fixedTime;
        private final double coordinateScale;

    }

}
