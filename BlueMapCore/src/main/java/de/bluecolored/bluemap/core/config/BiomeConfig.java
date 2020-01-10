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
import java.util.HashMap;
import java.util.Map.Entry;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.mapping.BiomeMapper;
import de.bluecolored.bluemap.core.world.Biome;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

public class BiomeConfig implements BiomeMapper {

	private ConfigurationLoader<? extends ConfigurationNode> autopoulationConfigLoader;
	private HashMap<Integer, Biome> biomes;
	
	public BiomeConfig(ConfigurationNode node) {
		this(node, null);
	}

	public BiomeConfig(ConfigurationNode node, ConfigurationLoader<? extends ConfigurationNode> autopoulationConfigLoader) {
		this.autopoulationConfigLoader = autopoulationConfigLoader;
		
		biomes = new HashMap<>();

		for (Entry<Object, ? extends ConfigurationNode> e : node.getChildrenMap().entrySet()){
			String id = e.getKey().toString();
			Biome biome = Biome.create(id, e.getValue());
			biomes.put(biome.getNumeralId(), biome);
		}
		
	}
	
	@Override
	public Biome get(int id) {
		Biome biome = biomes.get(id);
		
		if (biome == null) {
			if (autopoulationConfigLoader != null) {
				biomes.put(id, Biome.DEFAULT);
				
				synchronized (autopoulationConfigLoader) {
					try {
						ConfigurationNode node = autopoulationConfigLoader.load();
						node.getNode("unknown:" + id).getNode("id").setValue(id);
						autopoulationConfigLoader.save(node);
					} catch (IOException ex) {
						Logger.global.noFloodError("biomeconf-autopopulate-ioex", "Failed to auto-populate BiomeConfig!", ex);
					}
				}
			}
			
			return Biome.DEFAULT;
		}
		
		return biome;
	}
	
}
