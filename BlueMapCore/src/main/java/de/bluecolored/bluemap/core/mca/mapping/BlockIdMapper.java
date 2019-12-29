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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.BlockState;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;

public class BlockIdMapper {

	private Map<BlockIDMeta, BlockState> mappings;
	
	public BlockIdMapper() throws IOException {
		mappings = new HashMap<>();
		
		GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
				.setURL(getClass().getResource("/blockIds.json"))
				.build();
		
		ConfigurationNode node = loader.load();

		for (Entry<Object, ? extends ConfigurationNode> e : node.getChildrenMap().entrySet()){
			String key = e.getKey().toString();
			String value = e.getValue().getString();
			
			int splitIndex = key.indexOf(':');
			int blockId = Integer.parseInt(key.substring(0, splitIndex));
			int blockMeta = Integer.parseInt(key.substring(splitIndex + 1));
			
			BlockIDMeta idmeta = new BlockIDMeta(blockId, blockMeta);
			BlockState state = BlockState.fromString(value);
			
			mappings.put(idmeta, state);
		}	
	}
	
	public BlockState get(int id, int meta) {
		if (id == 0) return BlockState.AIR;
		
		BlockState state = mappings.get(new BlockIDMeta(id, meta));
		
		if (state == null) {
			state = mappings.get(new BlockIDMeta(id, 0)); //fallback
			
			if (state == null) {
				Logger.global.noFloodDebug(id + ":" + meta + "-blockidmapper-mappingerr", "Block ID can not be mapped: " + id + ":" + meta);
				return BlockState.AIR;
			}
		}
		
		return state;
	}
	
	class BlockIDMeta {
		private final int id;
		private final int meta;
		
		public BlockIDMeta(int id, int meta) {
			this.id = id;
			this.meta = meta;
		}
		
		public int getId() {
			return id;
		}
		
		public int getMeta() {
			return meta;
		}
		
		@Override
		public int hashCode() {
			return id * 0xFFFF + meta;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof BlockIDMeta) {
				BlockIDMeta other = (BlockIDMeta) obj;
				return other.id == id && other.meta == meta;
			}
			
			return false;
		}
	}
	
	public static BlockIdMapper create() throws IOException {
		return new BlockIdMapper();
	}
	
}
