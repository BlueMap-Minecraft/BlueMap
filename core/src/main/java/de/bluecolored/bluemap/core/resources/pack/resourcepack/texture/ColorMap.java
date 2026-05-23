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
package de.bluecolored.bluemap.core.resources.pack.resourcepack.texture;

import com.flowpowered.math.GenericMath;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.biome.Biome;

import java.awt.image.BufferedImage;

public class ColorMap {

    private final int[] colorMap;

    public ColorMap(BufferedImage map) {
        int[] colorMap = new int[65536];
        map.getRGB(0, 0, 256, 256, colorMap, 0, 256);
        this(colorMap);
    }

    public ColorMap(int[] colorMap) {
        this.colorMap = colorMap;
    }

    public Color getColor(Biome biome, Color defaultColor, Color target) {
        return getColor(biome.getTemperature(), biome.getDownfall(), defaultColor, target);
    }

    public Color getColor(double temperature, double downfall, Color defaultColor, Color target) {
        temperature = GenericMath.clamp(temperature, 0.0, 1.0);
        downfall = GenericMath.clamp(downfall, 0.0, 1.0);

        downfall *= temperature;

        int x = (int) ((1.0 - temperature) * 255.0);
        int y = (int) ((1.0 - downfall) * 255.0);

        int index = y << 8 | x;

        if (index >= colorMap.length) return target.set(defaultColor);

        int color = colorMap[index] | 0xFF000000;
        return target.set(color, true);
    }

}
