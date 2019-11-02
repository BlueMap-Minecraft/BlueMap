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
package de.bluecolored.bluemap.core.mca.mapping;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import de.bluecolored.bluemap.core.world.BlockState;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;

public class BlockPropertyMapper {
	
	private static final BlockProperties DEFAULT_PROPERTIES = new BlockProperties(false, false, false);
	
	private Multimap<String, BlockStateMapping<BlockProperties>> mappings;
	private LoadingCache<BlockState, BlockProperties> mappingCache;
	
	private BlockPropertyMapper() throws IOException {
		mappings = HashMultimap.create();
		
		GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
				.setURL(getClass().getResource("/blockProperties.json"))
				.build();
		
		ConfigurationNode node = loader.load();
		
		for (Entry<Object, ? extends ConfigurationNode> e : node.getChildrenMap().entrySet()){
			String key = e.getKey().toString();
			BlockState bsKey = BlockState.fromString(key);
			BlockProperties bsValue = new BlockProperties(
					e.getValue().getNode("culling").getBoolean(false),
					e.getValue().getNode("occluding").getBoolean(false),
					e.getValue().getNode("flammable").getBoolean(false)
				);
			BlockStateMapping<BlockProperties> mapping = new BlockStateMapping<>(bsKey, bsValue);
				mappings.put(bsKey.getId(), mapping);
		}
		
		mappings = Multimaps.unmodifiableMultimap(mappings);
		
		mappingCache = CacheBuilder.newBuilder()
				.concurrencyLevel(8)
				.maximumSize(10000)
				.build(new CacheLoader<BlockState, BlockProperties>(){
					@Override public BlockProperties load(BlockState key) { return mapNoCache(key); }
				});
	}
	
	public BlockProperties map(BlockState from){
		try {
			return mappingCache.get(from);
		} catch (ExecutionException e) {
			//should never happen, since the CacheLoader does not throw any exceptions
			throw new RuntimeException("Unexpected error while trying to map a BlockState's properties", e.getCause());
		}
	}

	private BlockProperties mapNoCache(BlockState bs){
		for (BlockStateMapping<BlockProperties> bm : mappings.get(bs.getId())){
			if (bm.fitsTo(bs)){
				return bm.getMapping();
			}
		}
		
		return DEFAULT_PROPERTIES;
	}
	
	public static BlockPropertyMapper create() throws IOException {
		return new BlockPropertyMapper();
	}
	
}
