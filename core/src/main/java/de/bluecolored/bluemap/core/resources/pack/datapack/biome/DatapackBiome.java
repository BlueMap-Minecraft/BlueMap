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
package de.bluecolored.bluemap.core.resources.pack.datapack.biome;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.biome.Biome;
import de.bluecolored.bluemap.core.world.biome.GrassColorModifier;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class DatapackBiome implements Biome {

    private final Key key;
    private final Data data;

    @Override
    public float getDownfall() {
        return data.downfall;
    }

    @Override
    public float getTemperature() {
        return data.temperature;
    }

    @Override
    public Color getWaterColor() {
        return data.effects.waterColor;
    }

    @Override
    public Color getOverlayFoliageColor() {
        return data.effects.foliageColor;
    }

    @Override
    public Color getOverlayGrassColor() {
        return data.effects.grassColor;
    }

    @Override
    public GrassColorModifier getGrassColorModifier() {
        return data.effects.grassColorModifier;
    }

    @SuppressWarnings("FieldMayBeFinal")
    @Getter
    public static class Data {

        private Effects effects = new Effects();
        private float temperature = Biome.DEFAULT.getTemperature();
        private float downfall = Biome.DEFAULT.getDownfall();

    }

    @SuppressWarnings("FieldMayBeFinal")
    @Getter
    public static class Effects {

        private Color waterColor = Biome.DEFAULT.getWaterColor();
        private Color foliageColor = Biome.DEFAULT.getOverlayFoliageColor();
        private Color grassColor = Biome.DEFAULT.getOverlayGrassColor();
        private GrassColorModifier grassColorModifier = Biome.DEFAULT.getGrassColorModifier();

    }

}
