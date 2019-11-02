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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;

public class BlockModelResource {

	private BlockStateResource blockState;
	
	private int xRot, yRot;
	private boolean uvLock;
	private boolean ambientOcclusion;
	private Collection<BlockModelElementResource> elements;
	private Map<String, String> textures;
	
	protected BlockModelResource(BlockStateResource blockState, ConfigurationNode declaration, ResourcePack resources) throws InvalidResourceDeclarationException {
		this.blockState = blockState;
		
		this.xRot = declaration.getNode("x").getInt(0);
		this.yRot = declaration.getNode("y").getInt(0);
		this.uvLock = declaration.getNode("uvlock").getBoolean(false);
		this.ambientOcclusion = true;
		this.elements = new Vector<>();
		this.textures = new ConcurrentHashMap<>();
		
		try {
			loadModelResource(declaration.getNode("model").getString(), resources);
		} catch (IOException e) {
			throw new InvalidResourceDeclarationException("Model not found: " + declaration.getNode("model").getString(), e);
		}
	}
	
	private void loadModelResource(String modelId, ResourcePack resources) throws IOException, InvalidResourceDeclarationException {
		Path resourcePath = Paths.get("assets", "minecraft", "models", modelId + ".json");
		
		ConfigurationNode data = GsonConfigurationLoader.builder()
			.setSource(() -> new BufferedReader(new InputStreamReader(resources.getResource(resourcePath), StandardCharsets.UTF_8)))
		 	.build()
		 	.load();
		
		//load parent first
		ConfigurationNode parent = data.getNode("parent");
		if (!parent.isVirtual()){
			loadModelResource(parent.getString(), resources);
		}
		
		for (Entry<Object, ? extends ConfigurationNode> texture : data.getNode("textures").getChildrenMap().entrySet()){
			String key = texture.getKey().toString();
			String value = texture.getValue().getString();
			textures.put(key, value);
		}
		
		ambientOcclusion = data.getNode("ambientocclusion").getBoolean(ambientOcclusion);
		
		if (!data.getNode("elements").isVirtual()){
			elements.clear();
			for (ConfigurationNode e : data.getNode("elements").getChildrenList()){
				elements.add(new BlockModelElementResource(this, e));
			}
		}
	}
	
	public BlockStateResource getBlockState(){
		return blockState;
	}

	public int getXRot() {
		return xRot;
	}

	public int getYRot() {
		return yRot;
	}

	public boolean isUvLock() {
		return uvLock;
	}

	public boolean isAmbientOcclusion() {
		return ambientOcclusion;
	}

	public Collection<BlockModelElementResource> getElements() {
		return Collections.unmodifiableCollection(elements);
	}
	
	public String resolveTexture(String key){
		if (key == null) return null;
		if (!key.startsWith("#")) return key;
		String texture = textures.get(key.substring(1));
		if (texture == null) return key;
		return resolveTexture(texture);
	}
	
	public Collection<String> getAllTextureIds(){
		List<String> list = new ArrayList<>();
		for (String tex : textures.values()){
			if (!tex.startsWith("#")) list.add(tex);
		}
		return list;
	}
	
}
