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
package de.bluecolored.bluemap.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.mapping.BlockPropertiesMapper;
import de.bluecolored.bluemap.core.resourcepack.NoSuchResourceException;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resourcepack.TransformedBlockModelResource;
import de.bluecolored.bluemap.core.world.BlockProperties;
import de.bluecolored.bluemap.core.world.BlockState;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class BlockPropertiesConfig implements BlockPropertiesMapper {
	
	private final ConfigurationLoader<? extends ConfigurationNode> autopoulationConfigLoader;
	
	private final Map<String, List<BlockStateMapping<BlockProperties>>> mappings;
	private final LoadingCache<BlockState, BlockProperties> mappingCache;
	
	private final ResourcePack resourcePack;
	
	public BlockPropertiesConfig(ConfigurationNode node, ResourcePack resourcePack) {
		this(node, resourcePack, null);
	}
	
	public BlockPropertiesConfig(ConfigurationNode node, ResourcePack resourcePack, ConfigurationLoader<? extends ConfigurationNode> autopoulationConfigLoader) {
		this.resourcePack = resourcePack;
		this.autopoulationConfigLoader = autopoulationConfigLoader;

		mappings = new ConcurrentHashMap<>();
		
		for (Entry<Object, ? extends ConfigurationNode> e : node.childrenMap().entrySet()){
			String key = e.getKey().toString();
			try {
				BlockState bsKey = BlockState.fromString(key);
				BlockProperties bsValue = new BlockProperties(
						e.getValue().node("culling").getBoolean(true),
						e.getValue().node("occluding").getBoolean(true),
						e.getValue().node("flammable").getBoolean(false)
					);
				BlockStateMapping<BlockProperties> mapping = new BlockStateMapping<>(bsKey, bsValue);
					mappings.computeIfAbsent(bsKey.getFullId(), k -> new ArrayList<>()).add(mapping);
			} catch (IllegalArgumentException ex) {
				Logger.global.logWarning("Loading BlockPropertiesConfig: Failed to parse BlockState from key '" + key + "'");
			}
		}
		
		mappingCache = Caffeine.newBuilder()
				.executor(BlueMap.THREAD_POOL)
				.maximumSize(10000)
				.build(this::mapNoCache);
	}
	
	@Override
	public BlockProperties get(BlockState from){
		return mappingCache.get(from);
	}

	private BlockProperties mapNoCache(BlockState bs){
		for (BlockStateMapping<BlockProperties> bm : mappings.getOrDefault(bs.getFullId(), Collections.emptyList())){
			if (bm.fitsTo(bs)){
				return bm.getMapping();
			}
		}
		
		BlockProperties generated = BlockProperties.SOLID;
		
		if (resourcePack != null) {
			try {
				boolean culling = false;
				boolean occluding = false;
	
				for(TransformedBlockModelResource model : resourcePack.getBlockStateResource(bs).getModels(bs)) {
					culling = culling || model.getModel().isCulling();
					occluding = occluding || model.getModel().isOccluding();
					if (culling && occluding) break;
				}
				
				generated = new BlockProperties(culling, occluding, generated.isFlammable());
			} catch (NoSuchResourceException ignore) {} //ignoring this because it will be logged later again if we try to render that block
		}
		
		mappings.computeIfAbsent(bs.getFullId(), k -> new ArrayList<>()).add(new BlockStateMapping<>(new BlockState(bs.getFullId()), generated));
		if (autopoulationConfigLoader != null) {
			synchronized (autopoulationConfigLoader) {
				try {
					ConfigurationNode node = autopoulationConfigLoader.load();
					ConfigurationNode bpNode = node.node(bs.getFullId());
					bpNode.node("culling").set(generated.isCulling());
					bpNode.node("occluding").set(generated.isOccluding());
					bpNode.node("flammable").set(generated.isFlammable());
					autopoulationConfigLoader.save(node);
				} catch (IOException ex) {
					Logger.global.noFloodError("blockpropsconf-autopopulate-ioex", "Failed to auto-populate BlockPropertiesConfig!", ex);
				}
			}
		}
		
		return generated;
	}
	
}
