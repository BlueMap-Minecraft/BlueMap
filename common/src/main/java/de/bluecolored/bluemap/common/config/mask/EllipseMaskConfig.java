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
package de.bluecolored.bluemap.common.config.mask;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.core.map.mask.EllipseMask;
import de.bluecolored.bluemap.core.map.mask.Mask;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@SuppressWarnings("FieldMayBeFinal")
@ConfigSerializable
@Getter
public class EllipseMaskConfig extends MaskConfig {

    private double
            centerX = 0,
            centerZ = 0;
    private double radiusX = Double.MAX_VALUE;
    private double radiusZ = Double.MAX_VALUE;
    private int
            minY = Integer.MIN_VALUE,
            maxY = Integer.MAX_VALUE;

    @Override
    public Mask createMask() throws ConfigurationException {
        if (minY > maxY) {
            throw new ConfigurationException("""
                    The circle-mask configuration results in a degenerate mask.
                    Make sure that the "min-y" value is actually SMALLER than the "max-y" counterpart.
                    """.trim());
        }

        if (radiusX <= 0 || radiusZ <= 0) {
            throw new ConfigurationException("""
                    The ellipse-mask configuration results in a degenerate mask.
                    Make sure that the radius values are greater than 0.
                    """.trim());
        }

        return new EllipseMask(
                new Vector2d(centerX, centerZ),
                radiusX, radiusZ,
                minY, maxY
        );
    }

}
