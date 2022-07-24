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
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.world.BlockProperties;
import de.bluecolored.bluemap.core.world.BlockState;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@DebugDump
public class BlockPropertiesConfig {

    private final Map<String, List<BlockStateMapping<BlockProperties>>> mappings;

    public BlockPropertiesConfig() {
        mappings = new ConcurrentHashMap<>();
    }

    public void load(Path configFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(configFile)) {
            JsonReader json = new JsonReader(reader);
            json.setLenient(true);

            json.beginObject();
            while (json.hasNext()) {
                String formatted = json.nextName();
                BlockState bsKey = BlockState.fromString(formatted);
                BlockProperties.Builder bsValueBuilder = BlockProperties.builder();

                json.beginObject();
                while (json.hasNext()) {
                    switch (json.nextName()) {
                        case "culling": bsValueBuilder.culling(json.nextBoolean()); break;
                        case "occluding": bsValueBuilder.occluding(json.nextBoolean()); break;
                        case "alwaysWaterlogged": bsValueBuilder.alwaysWaterlogged(json.nextBoolean()); break;
                        case "randomOffset": bsValueBuilder.randomOffset(json.nextBoolean()); break;
                        default: break;
                    }
                }
                json.endObject();

                BlockStateMapping<BlockProperties> mapping = new BlockStateMapping<>(bsKey, bsValueBuilder.build());

                // don't overwrite already present values, higher priority resources are loaded first
                mappings.computeIfAbsent(bsKey.getFormatted(), k -> new LinkedList<>()).add(0, mapping);
            }
            json.endObject();
        }
    }

    public BlockProperties getBlockProperties(BlockState from){
        for (BlockStateMapping<BlockProperties> bm : mappings.getOrDefault(from.getFormatted(), Collections.emptyList())){
            if (bm.fitsTo(from)){
                return bm.getMapping();
            }
        }

        return BlockProperties.DEFAULT;
    }

}
