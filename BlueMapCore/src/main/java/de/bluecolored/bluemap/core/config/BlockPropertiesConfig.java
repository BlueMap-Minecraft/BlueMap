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

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.mapping.BlockPropertiesMapper;
import de.bluecolored.bluemap.core.resourcepack.NoSuchResourceException;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resourcepack.TransformedBlockModelResource;
import de.bluecolored.bluemap.core.world.BlockProperties;
import de.bluecolored.bluemap.core.world.BlockState;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

public class BlockPropertiesConfig implements BlockPropertiesMapper {
	
	private ConfigurationLoader<? extends ConfigurationNode> autopoulationConfigLoader;
	
	private Multimap<String, BlockStateMapping<BlockProperties>> mappings;
	private LoadingCache<BlockState, BlockProperties> mappingCache;
	
	private ResourcePack resourcePack = null;
	
	public BlockPropertiesConfig(ConfigurationNode node, ResourcePack resourcePack) throws IOException {
		this(node, resourcePack, null);
	}
	
	public BlockPropertiesConfig(ConfigurationNode node, ResourcePack resourcePack, ConfigurationLoader<? extends ConfigurationNode> autopoulationConfigLoader) throws IOException {
		this.resourcePack = resourcePack;
		this.autopoulationConfigLoader = autopoulationConfigLoader;
		
		mappings = MultimapBuilder.hashKeys().arrayListValues().build();
		
		for (Entry<Object, ? extends ConfigurationNode> e : node.getChildrenMap().entrySet()){
			String key = e.getKey().toString();
			try {
				BlockState bsKey = BlockState.fromString(key);
				BlockProperties bsValue = new BlockProperties(
						e.getValue().getNode("culling").getBoolean(true),
						e.getValue().getNode("occluding").getBoolean(true),
						e.getValue().getNode("flammable").getBoolean(false)
					);
				BlockStateMapping<BlockProperties> mapping = new BlockStateMapping<>(bsKey, bsValue);
					mappings.put(bsKey.getFullId(), mapping);
			} catch (IllegalArgumentException ex) {
				Logger.global.logWarning("Loading BlockPropertiesConfig: Failed to parse BlockState from key '" + key + "'");
			}
		}
		
		mappingCache = CacheBuilder.newBuilder()
				.concurrencyLevel(8)
				.maximumSize(10000)
				.build(new CacheLoader<BlockState, BlockProperties>(){
					@Override public BlockProperties load(BlockState key) { return mapNoCache(key); }
				});
	}
	
	@Override
	public BlockProperties get(BlockState from){
		try {
			return mappingCache.get(from);
		} catch (ExecutionException neverHappens) {
			//should never happen, since the CacheLoader does not throw any exceptions
			throw new RuntimeException("Unexpected error while trying to map a BlockState's properties", neverHappens.getCause());
		}
	}

	private BlockProperties mapNoCache(BlockState bs){
		for (BlockStateMapping<BlockProperties> bm : mappings.get(bs.getFullId())){
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
		
		mappings.put(bs.getFullId(), new BlockStateMapping<BlockProperties>(new BlockState(bs.getFullId()), generated));
		if (autopoulationConfigLoader != null) {
			synchronized (autopoulationConfigLoader) {
				try {
					ConfigurationNode node = autopoulationConfigLoader.load();
					ConfigurationNode bpNode = node.getNode(bs.getFullId());
					bpNode.getNode("culling").setValue(generated.isCulling());
					bpNode.getNode("occluding").setValue(generated.isOccluding());
					bpNode.getNode("flammable").setValue(generated.isFlammable());
					autopoulationConfigLoader.save(node);
				} catch (IOException ex) {
					Logger.global.noFloodError("blockpropsconf-autopopulate-ioex", "Failed to auto-populate BlockPropertiesConfig!", ex);
				}
			}
		}
		
		return generated;
	}
	
}
