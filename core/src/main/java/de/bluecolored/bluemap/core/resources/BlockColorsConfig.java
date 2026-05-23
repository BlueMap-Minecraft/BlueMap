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

import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.hires.block.color.BlockColorCalculator;
import de.bluecolored.bluemap.core.map.hires.block.color.BlockColorCalculatorFactory;
import de.bluecolored.bluemap.core.map.hires.block.color.BlockColorCalculatorType;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.block.BlockAccess;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

public class BlockColorsConfig {
    private final Map<BlockState, String> colorConfig = new HashMap<>();
    private final Color defaultColor = new Color().set(0xffffffff, true);
    private final BlockColorCalculatorFactory defaultColorCalculatorFactory = BlockColorCalculatorFactory.fixed(defaultColor);

    public synchronized void load(Path configFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(configFile)) {
            JsonReader json = new JsonReader(reader);
            json.setLenient(true);

            json.beginObject();
            while (json.hasNext()) {

                BlockState key = BlockState.fromString(json.nextName());
                String value = json.nextString();

                // don't overwrite already present values, higher priority resources are loaded first
                colorConfig.putIfAbsent(key, value);
            }

            json.endObject();
        }
    }

    /**
     * Creates a new instance of {@link BlockColorCalculator} based on this config.<br>
     * The returned instance is not thread-safe, create a new instance for each Thread you use it on.
     */
    public synchronized BlockColorCalculator createBlockColorCalculator(ResourcePack resourcePack) {

        Map<Key, List<BlockStateMapping<BlockColorCalculator>>> mappings = new HashMap<>();
        BlockColorCalculator defaultCalculator = defaultColorCalculatorFactory.create(resourcePack);

        Map<String, BlockColorCalculator> calculators = new HashMap<>();
        Function<String, BlockColorCalculator> valueDeserializer = value -> createBlockColorCalculator(value, resourcePack);

        for (Map.Entry<BlockState, String> configEntry : colorConfig.entrySet()) {
            BlockState blockState = configEntry.getKey();
            String value = configEntry.getValue();

            BlockColorCalculator calculator = calculators.computeIfAbsent(value, valueDeserializer);
            BlockStateMapping<BlockColorCalculator> mapping = new BlockStateMapping<>(blockState, calculator);
            mappings.computeIfAbsent(blockState.getId(), _ -> new ArrayList<>(1)).add(mapping);
        }

        return new BlockColorCalculator() {

            @Override
            public Color getBlockColor(BlockAccess block, BlockState blockState, Color target) {
                return getCalculator(blockState).getBlockColor(block, blockState, target);
            }

            private BlockColorCalculator getCalculator(BlockState from){
                for (BlockStateMapping<BlockColorCalculator> bm : mappings.getOrDefault(from.getId(), Collections.emptyList())){
                    if (bm.fitsTo(from)){
                        return bm.getMapping();
                    }
                }

                return defaultCalculator;
            }

        };
    }

    private BlockColorCalculator createBlockColorCalculator(String configValue, ResourcePack resourcePack) {
        return createBlockColorCalculatorFactory(configValue).create(resourcePack);
    }

    private BlockColorCalculatorFactory createBlockColorCalculatorFactory(String configValue) {
        if (configValue == null || configValue.isBlank()) {
            Logger.global.noFloodDebug("Found empty color-config value, using default");
            return defaultColorCalculatorFactory;
        }

        if (configValue.charAt(0) == '@') {
            Key calculatorTypeKey = Key.parse(configValue.substring(1), Key.MINECRAFT_NAMESPACE);
            BlockColorCalculatorFactory factory = BlockColorCalculatorType.REGISTRY.get(calculatorTypeKey);
            if (factory == null) {
                Logger.global.noFloodDebug("Color-config value '%s' references an unknown calculator-type '%s', using default".formatted(configValue, calculatorTypeKey));
                return defaultColorCalculatorFactory;
            }
            return factory;
        }

        if (configValue.charAt(0) == '#') {
            try {
                Color color = new Color();
                color.parse(configValue);
                return BlockColorCalculatorFactory.fixed(color);
            } catch (NumberFormatException ex) {
                Logger.global.noFloodDebug("Color-config value '%s' starts with # but has an invalid format, using default".formatted(configValue));
                return defaultColorCalculatorFactory;
            }
        }

        try {
            Color color = new Color();
            color.parse(configValue);
            return BlockColorCalculatorFactory.fixed(color);
        } catch (NumberFormatException ignore) {}

        Key colorMapKey = Key.parse(configValue, Key.MINECRAFT_NAMESPACE);
        return BlockColorCalculatorFactory.colorMap(colorMapKey, defaultColor);
    }

}
