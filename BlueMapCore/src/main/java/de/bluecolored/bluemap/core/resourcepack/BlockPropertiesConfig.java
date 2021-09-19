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
package de.bluecolored.bluemap.core.resourcepack;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.BlockProperties;
import de.bluecolored.bluemap.core.world.BlockState;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class BlockPropertiesConfig {

    private final Map<String, List<BlockStateMapping<BlockProperties>>> mappings;

    public BlockPropertiesConfig() {
        mappings = new ConcurrentHashMap<>();
    }

    public void load(ConfigurationNode node) {
        for (Entry<Object, ? extends ConfigurationNode> e : node.childrenMap().entrySet()){
            String key = e.getKey().toString();
            try {
                BlockState bsKey = BlockState.fromString(key);
                BlockProperties.Builder bsValueBuilder = BlockProperties.builder();

                readBool(e.getValue().node("culling"), bsValueBuilder::culling);
                readBool(e.getValue().node("occluding"), bsValueBuilder::occluding);
                readBool(e.getValue().node("alwaysWaterlogged"), bsValueBuilder::alwaysWaterlogged);
                readBool(e.getValue().node("randomOffset"), bsValueBuilder::randomOffset);

                BlockStateMapping<BlockProperties> mapping = new BlockStateMapping<>(bsKey, bsValueBuilder.build());
                mappings.computeIfAbsent(bsKey.getFullId(), k -> new LinkedList<>()).add(0, mapping);
            } catch (IllegalArgumentException ex) {
                Logger.global.logWarning("Loading BlockPropertiesConfig: Failed to parse BlockState from key '" + key + "'");
            }
        }
    }

    private void readBool(ConfigurationNode node, Consumer<Boolean> target) {
        if (!node.virtual()) target.accept(node.getBoolean());
    }

    public BlockProperties getBlockProperties(BlockState from){
        for (BlockStateMapping<BlockProperties> bm : mappings.getOrDefault(from.getFullId(), Collections.emptyList())){
            if (bm.fitsTo(from)){
                return bm.getMapping();
            }
        }

        return BlockProperties.DEFAULT;
    }

}
