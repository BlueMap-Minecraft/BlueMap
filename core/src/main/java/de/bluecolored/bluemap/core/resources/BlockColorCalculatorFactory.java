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
package de.bluecolored.bluemap.core.resources;

import com.flowpowered.math.GenericMath;
import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.biome.Biome;
import de.bluecolored.bluemap.core.world.block.BlockAccess;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class BlockColorCalculatorFactory {

    private static final int BLEND_RADIUS_H = 2;
    private static final int BLEND_RADIUS_V = 1;
    private static final int
            BLEND_MIN_X = - BLEND_RADIUS_H,
            BLEND_MAX_X =   BLEND_RADIUS_H,
            BLEND_MIN_Y = - BLEND_RADIUS_V,
            BLEND_MAX_Y =   BLEND_RADIUS_V,
            BLEND_MIN_Z = - BLEND_RADIUS_H,
            BLEND_MAX_Z =   BLEND_RADIUS_H;

    private int[] foliageMap = new int[0];
    private int[] dryFoliageMap = new int[0];
    private int[] grassMap = new int[0];

    private final Map<String, ColorFunction> blockColorMap;

    public BlockColorCalculatorFactory() {
        this.blockColorMap = new HashMap<>();
    }

    public void load(Path configFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(configFile)) {
            JsonReader json = new JsonReader(reader);
            json.setLenient(true);

            json.beginObject();
            while (json.hasNext()) {

                String key = json.nextName();
                String value = json.nextString();

                ColorFunction colorFunction;
                switch (value) {
                    case "@foliage":
                        colorFunction = BlockColorCalculator::getBlendedFoliageColor;
                        break;
                    case "@dry_foliage":
                        colorFunction = BlockColorCalculator::getBlendedDryFoliageColor;
                        break;
                    case "@grass":
                        colorFunction = BlockColorCalculator::getBlendedGrassColor;
                        break;
                    case "@water":
                        colorFunction = BlockColorCalculator::getBlendedWaterColor;
                        break;
                    case "@redstone":
                        colorFunction = BlockColorCalculator::getRedstoneColor;
                        break;
                    default:
                        final Color color = new Color();
                        color.parse(value).premultiplied();
                        colorFunction = (calculator, block, target) -> target.set(color);
                        break;
                }

                // don't overwrite already present values, higher priority resources are loaded first
                blockColorMap.putIfAbsent(key, colorFunction);
            }

            json.endObject();
        }
    }

    public void setFoliageMap(BufferedImage map) {
        this.foliageMap = new int[65536];
        map.getRGB(0, 0, 256, 256, this.foliageMap, 0, 256);
    }

    public void setDryFoliageMap(BufferedImage map) {
        this.dryFoliageMap = new int[65536];
        map.getRGB(0, 0, 256, 256, this.dryFoliageMap, 0, 256);
    }

    public void setGrassMap(BufferedImage map) {
        this.grassMap = new int[65536];
        map.getRGB(0, 0, 256, 256, this.grassMap, 0, 256);
    }

    public BlockColorCalculator createCalculator() {
        return new BlockColorCalculator();
    }

    @FunctionalInterface
    private interface ColorFunction {
        Color invoke(BlockColorCalculator calculator, BlockNeighborhood block, Color target);
    }

    public class BlockColorCalculator {

        private final Color tempColor = new Color();

        @SuppressWarnings("UnusedReturnValue")
        public Color getBlockColor(BlockNeighborhood block, Color target) {
            String blockId = block.getBlockState().getFormatted();

            ColorFunction colorFunction = blockColorMap.get(blockId);
            if (colorFunction == null) colorFunction = blockColorMap.get("default");
            if (colorFunction == null) colorFunction = BlockColorCalculator::getBlendedFoliageColor;

            return colorFunction.invoke(this, block, target);
        }

        public Color getRedstoneColor(BlockAccess block, Color target) {
            int power = block.getBlockState().getRedstonePower();
            return target.set(
                    (power + 5f) / 20f, 0f, 0f,
                    1f, true
            );
        }

        public Color getBlendedWaterColor(BlockNeighborhood block, Color target) {
            target.set(0, 0, 0, 0, true);

            int x, y, z;

            Biome biome;
            for (y = BLEND_MIN_Y; y <= BLEND_MAX_Y; y++) {
                for (x = BLEND_MIN_X; x <= BLEND_MAX_X; x++) {
                    for (z = BLEND_MIN_Z; z <= BLEND_MAX_Z; z++) {
                        biome = block.getNeighborBlock(x, y, z).getBiome();
                        target.add(biome.getWaterColor());
                    }
                }
            }

            return target.flatten();
        }

        public Color getBlendedFoliageColor(BlockNeighborhood block, Color target) {
            target.set(0, 0, 0, 0, true);

            int x, y, z;

            Biome biome;
            for (y = BLEND_MIN_Y; y <= BLEND_MAX_Y; y++) {
                for (x = BLEND_MIN_X; x <= BLEND_MAX_X; x++) {
                    for (z = BLEND_MIN_Z; z <= BLEND_MAX_Z; z++) {
                        biome = block.getNeighborBlock(x, y, z).getBiome();
                        target.add(getFoliageColor(biome, tempColor));
                    }
                }
            }

            return target.flatten();
        }

        public Color getFoliageColor(Biome biome, Color target) {
            getColorFromMap(biome, foliageMap, 4764952, target);
            return target.overlay(biome.getOverlayFoliageColor());
        }

        public Color getBlendedDryFoliageColor(BlockNeighborhood block, Color target) {
            target.set(0, 0, 0, 0, true);

            int x, y, z;

            Biome biome;
            for (y = BLEND_MIN_Y; y <= BLEND_MAX_Y; y++) {
                for (x = BLEND_MIN_X; x <= BLEND_MAX_X; x++) {
                    for (z = BLEND_MIN_Z; z <= BLEND_MAX_Z; z++) {
                        biome = block.getNeighborBlock(x, y, z).getBiome();
                        target.add(getDryFoliageColor(biome, tempColor));
                    }
                }
            }

            return target.flatten();
        }

        public Color getDryFoliageColor(Biome biome, Color target) {
            getColorFromMap(biome, dryFoliageMap, 0xff8f5f33, target);
            return target.overlay(biome.getOverlayFoliageColor());
        }

        public Color getBlendedGrassColor(BlockNeighborhood block, Color target) {
            target.set(0, 0, 0, 0, true);

            int x, y, z;

            for (y = BLEND_MIN_Y; y <= BLEND_MAX_Y; y++) {
                for (x = BLEND_MIN_X; x <= BLEND_MAX_X; x++) {
                    for (z = BLEND_MIN_Z; z <= BLEND_MAX_Z; z++) {
                        target.add(getGrassColor(block.getNeighborBlock(x, y, z), tempColor));
                    }
                }
            }

            return target.flatten();
        }

        public Color getGrassColor(BlockAccess block, Color target) {
            Biome biome = block.getBiome();
            getColorFromMap(biome, grassMap, 0xff52952f, target);
            target.overlay(biome.getOverlayGrassColor());
            biome.getGrassColorModifier().apply(block, target);
            return target;
        }

        private void getColorFromMap(Biome biome, int[] colorMap, int defaultColor, Color target) {
            double temperature = GenericMath.clamp(biome.getTemperature(), 0.0, 1.0);
            double downfall = GenericMath.clamp(biome.getDownfall(), 0.0, 1.0);

            downfall *= temperature;

            int x = (int) ((1.0 - temperature) * 255.0);
            int y = (int) ((1.0 - downfall) * 255.0);

            int index = y << 8 | x;
            int color = (index >= colorMap.length ? defaultColor : colorMap[index]) | 0xFF000000;

            target.set(color);
        }

    }

}
