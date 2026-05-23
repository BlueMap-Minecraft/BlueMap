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
package de.bluecolored.bluemap.core.map.hires.block.color;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.ColorMap;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.biome.Biome;
import de.bluecolored.bluemap.core.world.biome.ColorModifier;

import java.util.function.Function;
import java.util.function.UnaryOperator;

public interface BlockColorCalculatorFactory {

    BlockColorCalculator create(ResourcePack resourcePack);

    default BlockColorCalculatorFactory withBiomeOverlay(Function<Biome, Color> biomeOverlayFunction) {
        return resourcePack -> new BiomeOverlayBlockColorCalculator(this.create(resourcePack), biomeOverlayFunction);
    }

    default BlockColorCalculatorFactory withBiomeColorModifier(Function<Biome, ColorModifier> biomeColorModifierFunction) {
        return resourcePack -> new BiomeColorModifierBlockColorCalculator(this.create(resourcePack), biomeColorModifierFunction);
    }

    default BlockColorCalculatorFactory blended() {
        return resourcePack -> new BlendedBlockColorCalculator(this.create(resourcePack));
    }

    default BlockColorCalculatorFactory blended(int horizontalBlend, int verticalBlend) {
        return resourcePack -> new BlendedBlockColorCalculator(this.create(resourcePack), horizontalBlend, verticalBlend);
    }

    default BlockColorCalculatorFactory with(UnaryOperator<BlockColorCalculatorFactory> blockColorCalculatorFactory) {
        return blockColorCalculatorFactory.apply(this);
    }

    static BlockColorCalculatorFactory fixed(Color color) {
        BlockColorCalculator calculator = new FixedBlockColorCalculator(color);
        return _ -> calculator;
    }

    static BlockColorCalculatorFactory biome(Function<Biome, Color>  biomeColorFunction) {
        BlockColorCalculator calculator = new BiomeBlockColorCalculator(biomeColorFunction);
        return _ -> calculator;
    }

    static BlockColorCalculatorFactory colorMap(Key colorMapKey, Color defaultColor) {
        return resourcePack -> {
            ColorMap colorMap = resourcePack.getColormaps().get(colorMapKey);
            if (colorMap == null) {
                Logger.global.noFloodDebug("No color map found for resource-key '%s', using default color".formatted(colorMapKey));
                return new FixedBlockColorCalculator(defaultColor);
            }
            return new ColorMapBlockColorCalculator(colorMap, defaultColor);
        };
    }

}
