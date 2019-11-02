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

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;

public class BiomeIdMapper {
	private static final String DEFAULT_BIOME = "ocean";

	private String[] biomes;
	
	public BiomeIdMapper() throws IOException {
		biomes = new String[256];
		for (int i = 0; i < biomes.length; i++) {
			biomes[i] = DEFAULT_BIOME;
		}
		
		GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
				.setURL(getClass().getResource("/biomes.json"))
				.build();
		
		ConfigurationNode node = loader.load();

		for (Entry<Object, ? extends ConfigurationNode> e : node.getChildrenMap().entrySet()){
			String biome = e.getKey().toString();
			int id = e.getValue().getNode("id").getInt(-1);
			if (id >= 0 && id < biomes.length) {
				biomes[id] = biome;
			}
		}	
		
	}
	
	public String get(int id) {
		if (id < 0 || id >= biomes.length) return DEFAULT_BIOME;
		return biomes[id];
	}
	
	public static BiomeIdMapper create() throws IOException {
		return new BiomeIdMapper();
	}
	
}
