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

import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.core.map.mask.BoxMask;
import de.bluecolored.bluemap.core.map.mask.Mask;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@SuppressWarnings("FieldMayBeFinal")
@ConfigSerializable
@Getter
public class BoxMaskConfig extends MaskConfig {

    private int
            minX = Integer.MIN_VALUE,
            minY = Integer.MIN_VALUE,
            minZ = Integer.MIN_VALUE,
            maxX = Integer.MAX_VALUE,
            maxY = Integer.MAX_VALUE,
            maxZ = Integer.MAX_VALUE;

    @Override
    public Mask createMask() throws ConfigurationException {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new ConfigurationException("""
                    The box-mask configuration results in a degenerate mask.
                    Make sure that all "min-" values are actually SMALLER than their "max-" counterparts.
                    """.trim());
        }

        return new BoxMask(
                new Vector3i(minX, minY, minZ),
                new Vector3i(maxX, maxY, maxZ)
        );
    }

}
