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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.mapping.BlockIdMapper;
import de.bluecolored.bluemap.core.world.BlockState;
import ninja.leaping.configurate.ConfigurationNode;

public class BlockIdConfig implements BlockIdMapper {

	private Map<BlockIDMeta, BlockState> mappings;
	
	public BlockIdConfig(ConfigurationNode node) {
		mappings = new HashMap<>(); 
		
		for (Entry<Object, ? extends ConfigurationNode> e : node.getChildrenMap().entrySet()){
			String key = e.getKey().toString();
			String value = e.getValue().getString();

			try {
				int splitIndex = key.indexOf(':');
				int blockId, blockMeta;
				if (splitIndex > 0 && splitIndex < key.length() - 1) {
					blockId = Integer.parseInt(key.substring(0, splitIndex));
					blockMeta = Integer.parseInt(key.substring(splitIndex + 1));
				} else {
					blockId = Integer.parseInt(key);
					blockMeta = 0;
				}
				
				BlockIDMeta idmeta = new BlockIDMeta(blockId, blockMeta);
				BlockState state = BlockState.fromString(value);
				
				mappings.put(idmeta, state);
			} catch (NumberFormatException ex) {
				Logger.global.logWarning("Loading BlockIdConfig: Failed to parse blockid:meta from key '" + key + "'");
			} catch (IllegalArgumentException ex) {
				Logger.global.logWarning("Loading BlockIdConfig: Failed to parse BlockState from value '" + value + "'");
			}
		}	
	}
	
	@Override
	public BlockState get(int id, int meta) {
		BlockState state = mappings.get(new BlockIDMeta(id, meta));
		
		if (state == null) {
			state = mappings.getOrDefault(new BlockIDMeta(id, 0), BlockState.AIR); //meta-fallback
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
	
}
