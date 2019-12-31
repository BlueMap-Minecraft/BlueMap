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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.fileaccess.FileAccess;
import de.bluecolored.bluemap.core.util.MathUtils;
import de.bluecolored.bluemap.core.world.BlockState;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;

public class BlockStateResource {

	private List<Variant> variants = new ArrayList<>();
	private Collection<Variant> multipart = new ArrayList<>();
	
	private BlockStateResource() {}
	
	public Collection<TransformedBlockModelResource> getModels(BlockState blockState){
		return getModels(blockState, Vector3i.ZERO);
	}
	
	public Collection<TransformedBlockModelResource> getModels(BlockState blockState, Vector3i pos){
		Collection<TransformedBlockModelResource> models = new ArrayList<>();
		for (Variant variant : variants) {
			if (variant.condition.matches(blockState)) {
				models.add(variant.getModel(pos));
				return models;
			}
		}
		
		for (Variant variant : multipart) {
			if (variant.condition.matches(blockState)) {
				models.add(variant.getModel(pos));
			}
		}
		
		return models;
	}
	
	private class Variant {
		
		private PropertyCondition condition = PropertyCondition.all();
		private Collection<Weighted<TransformedBlockModelResource>> models = new ArrayList<>();
		
		private double totalWeight;
		
		private Variant() {}
		
		public TransformedBlockModelResource getModel(Vector3i pos) {
			double selection = MathUtils.hashToFloat(pos, 827364) * totalWeight; //random based on position
			for (Weighted<TransformedBlockModelResource> w : models) {
				selection -= w.weight;
				if (selection < 0) return w.value;
			}
			
			throw new RuntimeException("This line should never be reached!");
		}
		
		public void updateTotalWeight() {
			totalWeight = 0d;
			for (Weighted<?> w : models) {
				totalWeight += w.weight;
			}
		}
		
	}
	
	private static class Weighted<T> {
		
		private T value;
		private double weight;
		
		public Weighted(T value, double weight) {
			this.value = value;
			this.weight = weight;
		}
		
	}

	public static Builder builder(FileAccess sourcesAccess, ResourcePack resourcePack) {
		return new Builder(sourcesAccess, resourcePack);
	}
	
	public static class Builder {
		private final FileAccess sourcesAccess;
		private final ResourcePack resourcePack;
		
		private Builder(FileAccess sourcesAccess, ResourcePack resourcePack) {
			this.sourcesAccess = sourcesAccess;
			this.resourcePack = resourcePack;
		}
		
		public BlockStateResource build(String blockstateFile) throws IOException {
			BlockStateResource blockState = new BlockStateResource();
			ConfigurationNode config = GsonConfigurationLoader.builder()
					.setSource(() -> new BufferedReader(new InputStreamReader(sourcesAccess.readFile(blockstateFile), StandardCharsets.UTF_8)))
					.build()
					.load();
			
			//create variants
			for (Entry<Object, ? extends ConfigurationNode> entry : config.getNode("variants").getChildrenMap().entrySet()) {
				try {
					String conditionString = entry.getKey().toString();
					ConfigurationNode transformedModelNode = entry.getValue();
					
					Variant variant = blockState.new Variant();
					variant.condition = parseConditionString(conditionString);
					variant.models = loadModels(transformedModelNode, blockstateFile);
					
					variant.updateTotalWeight();
					
					blockState.variants.add(variant);
				} catch (Exception ex) {
					Logger.global.logWarning("Failed to parse a variant of " + blockstateFile + ": " + ex);
				}
			}
			
			//create multipart
			for (ConfigurationNode partNode : config.getNode("multipart").getChildrenList()) {
				try {
					Variant variant = blockState.new Variant();
					ConfigurationNode whenNode = partNode.getNode("when");
					if (!whenNode.isVirtual()) {
						variant.condition = parseCondition(whenNode);
					}
					variant.models = loadModels(partNode.getNode("apply"), blockstateFile);
					
					variant.updateTotalWeight();
					
					blockState.multipart.add(variant);
				} catch (Exception ex) {
					Logger.global.logWarning("Failed to parse a multipart-part of " + blockstateFile + ": " + ex);
				}
			}
			
			return blockState;
		}
		
		private Collection<Weighted<TransformedBlockModelResource>> loadModels(ConfigurationNode node, String blockstateFile) {
			Collection<Weighted<TransformedBlockModelResource>> models = new ArrayList<>();
			
			if (node.hasListChildren()) {
				for (ConfigurationNode modelNode : node.getChildrenList()) {
					try {
						models.add(loadModel(modelNode));
					} catch (ParseResourceException ex) {
						Logger.global.logWarning("Failed to load a model trying to parse " + blockstateFile + ": " + ex);
					}
				}
			} else if (node.hasMapChildren()) {
				try {
					models.add(loadModel(node));
				} catch (ParseResourceException ex) {
					Logger.global.logWarning("Failed to load a model trying to parse " + blockstateFile + ": " + ex);
				}
			}
			
			return models;
		}
		
		private Weighted<TransformedBlockModelResource> loadModel(ConfigurationNode node) throws ParseResourceException {
			String modelPath = node.getNode("model").getString();
			if (modelPath == null) throw new ParseResourceException("No model defined!");
			
			modelPath = ResourcePack.namespacedToAbsoluteResourcePath(modelPath, "models") + ".json";
			
			BlockModelResource model = resourcePack.blockModelResources.get(modelPath);
			if (model == null) {
				try {
					model = BlockModelResource.builder(sourcesAccess, resourcePack).build(modelPath);
				} catch (IOException e) {
					throw new ParseResourceException("Failed to load model " + modelPath, e);
				}
				
				resourcePack.blockModelResources.put(modelPath, model);
			}
			
			Vector2i rotation = new Vector2i(
					node.getNode("x").getInt(0), 
					node.getNode("y").getInt(0)
				);
			boolean uvLock = node.getNode("uvlock").getBoolean(false);
			
			TransformedBlockModelResource transformedModel = new TransformedBlockModelResource(rotation, uvLock, model);
			return new Weighted<TransformedBlockModelResource>(transformedModel, node.getNode("weight").getDouble(1d));
		}

		private PropertyCondition parseCondition(ConfigurationNode conditionNode) {
			List<PropertyCondition> andConditions = new ArrayList<>();
			for (Entry<Object, ? extends ConfigurationNode> entry : conditionNode.getChildrenMap().entrySet()) {
				String key = entry.getKey().toString();
				if (key.equals("OR")) {
					List<PropertyCondition> orConditions = new ArrayList<>();
					for (ConfigurationNode orConditionNode : entry.getValue().getChildrenList()) {
						orConditions.add(parseCondition(orConditionNode));
					}
					andConditions.add(PropertyCondition.or(orConditions.toArray(new PropertyCondition[orConditions.size()])));
				} else {
					String[] values = StringUtils.split(entry.getValue().getString(""), '|');
					andConditions.add(PropertyCondition.property(key, values));
				}
			}
			
			return PropertyCondition.and(andConditions.toArray(new PropertyCondition[andConditions.size()]));
		}
		
		private PropertyCondition parseConditionString(String conditionString) throws IllegalArgumentException {
			List<PropertyCondition> conditions = new ArrayList<>();
			if (!conditionString.isEmpty() && !conditionString.equals("default") && !conditionString.equals("normal")) {
				String[] conditionSplit = StringUtils.split(conditionString, ',');
				for (String element : conditionSplit) {
					String[] keyval = StringUtils.split(element, "=", 2);
					if (keyval.length < 2) throw new IllegalArgumentException("Condition-String '" + conditionString + "' is invalid!");
					conditions.add(PropertyCondition.property(keyval[0], keyval[1]));
				}
			}
			
			PropertyCondition condition;
			if (conditions.isEmpty()) {
				condition = PropertyCondition.all();
			} else if (conditions.size() == 1) {
				condition = conditions.get(0);
			} else {
				condition = PropertyCondition.and(conditions.toArray(new PropertyCondition[conditions.size()]));
			}
			
			return condition;
		}
	}
}
