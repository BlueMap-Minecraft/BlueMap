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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import com.google.common.base.Preconditions;

import de.bluecolored.bluemap.core.util.WeighedArrayList;
import de.bluecolored.bluemap.core.world.BlockState;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;

public class BlockStateResource {
	private BlockState block;
	private Collection<WeighedArrayList<BlockModelResource>> modelResources; 
	
	protected BlockStateResource(BlockState block, ResourcePack resources) throws NoSuchResourceException, InvalidResourceDeclarationException {
		this.block = Preconditions.checkNotNull(block);
		this.modelResources = new Vector<>();
		
		try {
			ConfigurationNode data = GsonConfigurationLoader.builder()
					.setSource(() -> new BufferedReader(new InputStreamReader(resources.getResource(getResourcePath()), StandardCharsets.UTF_8)))
					.build()
					.load();
			
			load(data, resources);
		} catch (IOException e) {
			throw new NoSuchResourceException("There is no definition for resource-id: " + block.getId(), e);
		} catch (NullPointerException e){
			throw new InvalidResourceDeclarationException(e);
		}
		
		this.modelResources = Collections.unmodifiableCollection(this.modelResources);
	}
	
	private void load(ConfigurationNode data, ResourcePack resources) throws InvalidResourceDeclarationException {
		
		//load variants
		ConfigurationNode variants = data.getNode("variants");
		for (Entry<Object, ? extends ConfigurationNode> e : variants.getChildrenMap().entrySet()){
			if (getBlock().checkVariantCondition(e.getKey().toString())){
				addModelResource(e.getValue(), resources);
				break;
			}
		}
		
		//load multipart
		ConfigurationNode multipart = data.getNode("multipart");
		for (ConfigurationNode part : multipart.getChildrenList()){
			
			ConfigurationNode when = part.getNode("when");
			if (when.isVirtual() || checkMultipartCondition(when)){
				addModelResource(part.getNode("apply"), resources);
			}
		}
		
	}
	
	private void addModelResource(ConfigurationNode n, ResourcePack resources) throws InvalidResourceDeclarationException {
		WeighedArrayList<BlockModelResource> models = new WeighedArrayList<>();
		
		if (n.hasListChildren()){
			
			//if it is a weighted list of alternative models, select one by random and weight
			List<? extends ConfigurationNode> cList = n.getChildrenList();
			for (ConfigurationNode c : cList){
				int weight = c.getNode("weight").getInt(1);
				models.add(new BlockModelResource(this, c, resources), weight);
			}
			
		} else {
			models.add(new BlockModelResource(this, n, resources));
		}
		
		modelResources.add(models);
	}
	
	private boolean checkMultipartCondition(ConfigurationNode when){
		ConfigurationNode or = when.getNode("OR");
		if (!or.isVirtual()){
			for (ConfigurationNode condition : or.getChildrenList()){
				if (checkMultipartCondition(condition)) return true;
			}
			
			return false;
		}
		
		Map<String, String> blockProperties = getBlock().getProperties();
		for (Entry<Object, ? extends ConfigurationNode> e : when.getChildrenMap().entrySet()){
			String key = e.getKey().toString();
			String[] values = e.getValue().getString().split("\\|");
			
			boolean found = false;
			for (String value : values){
				if (value.equals(blockProperties.get(key))){
					found = true;
					break;
				}
			}
			
			if (!found) return false;
		}
		
		return true;
	}

	public BlockState getBlock() {
		return block;
	}
	
	public Collection<WeighedArrayList<BlockModelResource>> getModelResources(){
		return modelResources;
	}
	
	private Path getResourcePath(){
		return Paths.get("assets", block.getNamespace(), "blockstates", block.getId() + ".json");
	}
	
}
