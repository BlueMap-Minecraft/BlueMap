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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.PropertyCondition.All;
import de.bluecolored.bluemap.core.resourcepack.fileaccess.FileAccess;
import de.bluecolored.bluemap.core.util.MathUtils;
import de.bluecolored.bluemap.core.world.BlockState;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;

public class BlockStateResource {
	
	private List<Variant> variants = new ArrayList<>();
	private Collection<Variant> multipart = new ArrayList<>();

	private BlockStateResource() {
	}

	public Collection<TransformedBlockModelResource> getModels(BlockState blockState) {
		return getModels(blockState, Vector3i.ZERO);
	}

	public Collection<TransformedBlockModelResource> getModels(BlockState blockState, Vector3i pos) {
		Collection<TransformedBlockModelResource> models = new ArrayList<>();
		
		Variant allMatch = null;
		for (Variant variant : variants) {
			if (variant.condition.matches(blockState)) {
				if (variant.condition instanceof All) { //only use "all" condition if nothing else matched
					if (allMatch == null) allMatch = variant;
					continue;
				}
				
				models.add(variant.getModel(pos));
				return models;
			}
		}
		
		if (allMatch != null) {
			models.add(allMatch.getModel(pos));
			return models;
		}

		for (Variant variant : multipart) {
			if (variant.condition.matches(blockState)) {
				models.add(variant.getModel(pos));
			}
		}
		
		//fallback to first variant
		if (models.isEmpty() && !variants.isEmpty()) {
			models.add(variants.get(0).getModel(pos));
		}

		return models;
	}

	private class Variant {

		private PropertyCondition condition = PropertyCondition.all();
		private Collection<Weighted<TransformedBlockModelResource>> models = new ArrayList<>();

		private double totalWeight;

		private Variant() {
		}

		public TransformedBlockModelResource getModel(Vector3i pos) {
			if (models.isEmpty()) throw new IllegalStateException("A variant must have at least one model!");
			
			double selection = MathUtils.hashToFloat(pos, 827364) * totalWeight; // random based on position
			for (Weighted<TransformedBlockModelResource> w : models) {
				selection -= w.weight;
				if (selection <= 0) return w.value;
			}

			throw new RuntimeException("This line should never be reached!");
		}

		public void checkValid() throws ParseResourceException {
			if (models.isEmpty()) throw new ParseResourceException("A variant must have at least one model!");
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

		private static final String JSON_COMMENT = "__comment";
		
		private final FileAccess sourcesAccess;
		private final ResourcePack resourcePack;

		private Builder(FileAccess sourcesAccess, ResourcePack resourcePack) {
			this.sourcesAccess = sourcesAccess;
			this.resourcePack = resourcePack;
		}

		public BlockStateResource build(String blockstateFile) throws IOException {
			ConfigurationNode config = GsonConfigurationLoader.builder()
					.setSource(() -> new BufferedReader(
							new InputStreamReader(sourcesAccess.readFile(blockstateFile), StandardCharsets.UTF_8)))
					.build().load();

			if (!config.getNode("forge_marker").isVirtual()) {
				return buildForge(config, blockstateFile);
			}

			BlockStateResource blockState = new BlockStateResource();

			// create variants
			for (Entry<Object, ? extends ConfigurationNode> entry : config.getNode("variants").getChildrenMap().entrySet()) {
				if (entry.getKey().equals(JSON_COMMENT)) continue;
				
				try {
					String conditionString = entry.getKey().toString();
					ConfigurationNode transformedModelNode = entry.getValue();

					Variant variant = blockState.new Variant();
					variant.condition = parseConditionString(conditionString);
					variant.models = loadModels(transformedModelNode, blockstateFile, null);

					variant.updateTotalWeight();
					variant.checkValid();

					blockState.variants.add(variant);
				} catch (Throwable t) {
					Logger.global.logWarning("Failed to parse a variant of " + blockstateFile + ": " + t);
				}
			}

			// create multipart
			for (ConfigurationNode partNode : config.getNode("multipart").getChildrenList()) {
				try {
					Variant variant = blockState.new Variant();
					ConfigurationNode whenNode = partNode.getNode("when");
					if (!whenNode.isVirtual()) {
						variant.condition = parseCondition(whenNode);
					}
					variant.models = loadModels(partNode.getNode("apply"), blockstateFile, null);

					variant.updateTotalWeight();
					variant.checkValid();

					blockState.multipart.add(variant);
				} catch (Throwable t) {
					Logger.global.logWarning("Failed to parse a multipart-part of " + blockstateFile + ": " + t);
				}
			}

			return blockState;
		}

		private Collection<Weighted<TransformedBlockModelResource>> loadModels(ConfigurationNode node, String blockstateFile, Map<String, String> overrideTextures) {
			Collection<Weighted<TransformedBlockModelResource>> models = new ArrayList<>();

			if (node.hasListChildren()) {
				for (ConfigurationNode modelNode : node.getChildrenList()) {
					try {
						models.add(loadModel(modelNode, overrideTextures));
					} catch (ParseResourceException ex) {
						Logger.global.logWarning("Failed to load a model trying to parse " + blockstateFile + ": " + ex);
					}
				}
			} else if (node.hasMapChildren()) {
				try {
					models.add(loadModel(node, overrideTextures));
				} catch (ParseResourceException ex) {
					Logger.global.logWarning("Failed to load a model trying to parse " + blockstateFile + ": " + ex);
				}
			}

			return models;
		}

		private Weighted<TransformedBlockModelResource> loadModel(ConfigurationNode node, Map<String, String> overrideTextures) throws ParseResourceException {
			String namespacedModelPath = node.getNode("model").getString();
			if (namespacedModelPath == null)
				throw new ParseResourceException("No model defined!");

			String modelPath = ResourcePack.namespacedToAbsoluteResourcePath(namespacedModelPath, "models") + ".json";

			BlockModelResource model = resourcePack.blockModelResources.get(modelPath);
			if (model == null) {
				BlockModelResource.Builder builder = BlockModelResource.builder(sourcesAccess, resourcePack);
				try {
					if (overrideTextures != null) model = builder.build(modelPath, overrideTextures);
					else model = builder.build(modelPath);
				} catch (IOException e) {
					throw new ParseResourceException("Failed to load model " + modelPath, e);
				}

				resourcePack.blockModelResources.put(modelPath, model);
			}

			Vector2f rotation = new Vector2f(node.getNode("x").getFloat(0), node.getNode("y").getFloat(0));
			boolean uvLock = node.getNode("uvlock").getBoolean(false);

			TransformedBlockModelResource transformedModel = new TransformedBlockModelResource(rotation, uvLock, model);
			return new Weighted<TransformedBlockModelResource>(transformedModel, node.getNode("weight").getDouble(1d));
		}

		private PropertyCondition parseCondition(ConfigurationNode conditionNode) {
			List<PropertyCondition> andConditions = new ArrayList<>();
			for (Entry<Object, ? extends ConfigurationNode> entry : conditionNode.getChildrenMap().entrySet()) {
				String key = entry.getKey().toString();
				if (key.equals(JSON_COMMENT)) continue;
				
				if (key.equals("OR")) {
					List<PropertyCondition> orConditions = new ArrayList<>();
					for (ConfigurationNode orConditionNode : entry.getValue().getChildrenList()) {
						orConditions.add(parseCondition(orConditionNode));
					}
					andConditions.add(
							PropertyCondition.or(orConditions.toArray(new PropertyCondition[orConditions.size()])));
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
					if (keyval.length < 2)
						throw new IllegalArgumentException("Condition-String '" + conditionString + "' is invalid!");
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

		private BlockStateResource buildForge(ConfigurationNode config, String blockstateFile) {
			ConfigurationNode modelDefaults = config.getNode("defaults");

			List<ForgeVariant> variants = new ArrayList<>();
			for (Entry<Object, ? extends ConfigurationNode> entry : config.getNode("variants").getChildrenMap().entrySet()) {
				if (entry.getKey().equals(JSON_COMMENT)) continue;
				if (isForgeStraightVariant(entry.getValue())) continue;

				// create variants for single property
				List<ForgeVariant> propertyVariants = new ArrayList<>();
				String key = entry.getKey().toString();
				for (Entry<Object, ? extends ConfigurationNode> value : entry.getValue().getChildrenMap().entrySet()) {
					if (value.getKey().equals(JSON_COMMENT)) continue;
					
					ForgeVariant variant = new ForgeVariant();
					variant.properties.put(key, value.getKey().toString());
					variant.node = value.getValue();
					propertyVariants.add(variant);
				}

				// join variants
				List<ForgeVariant> oldVariants = variants;
				variants = new ArrayList<>(oldVariants.size() * propertyVariants.size());
				for (ForgeVariant oldVariant : oldVariants) {
					for (ForgeVariant addVariant : propertyVariants) {
						variants.add(oldVariant.createMerge(addVariant));
					}
				}
			}

			//create all possible property-variants
			BlockStateResource blockState = new BlockStateResource();
			for (ForgeVariant forgeVariant : variants) {
				Variant variant = blockState.new Variant();

				ConfigurationNode modelNode = forgeVariant.node.mergeValuesFrom(modelDefaults);

				Map<String, String> textures = new HashMap<>();
				for (Entry<Object, ? extends ConfigurationNode> entry : modelNode.getNode("textures").getChildrenMap().entrySet()) {
					if (entry.getKey().equals(JSON_COMMENT)) continue;
					
					textures.putIfAbsent(entry.getKey().toString(), entry.getValue().getString(null));
				}

				List<PropertyCondition> conditions = new ArrayList<>(forgeVariant.properties.size());
				for (Entry<String, String> property : forgeVariant.properties.entrySet()) {
					conditions.add(PropertyCondition.property(property.getKey(), property.getValue()));
				}
				variant.condition = PropertyCondition.and(conditions.toArray(new PropertyCondition[conditions.size()]));

				variant.models.addAll(loadModels(modelNode, blockstateFile, textures));
				
				for (Entry<Object, ? extends ConfigurationNode> entry : modelNode.getNode("submodel").getChildrenMap().entrySet()) {
					if (entry.getKey().equals(JSON_COMMENT)) continue;
					
					variant.models.addAll(loadModels(entry.getValue(), blockstateFile, textures));
				}
				
				variant.updateTotalWeight();

				try {
					variant.checkValid();
					blockState.variants.add(variant);
				} catch (ParseResourceException ex) {
					Logger.global.logWarning("Failed to parse a variant (forge/property) of " + blockstateFile + ": " + ex);
				}
				
			}
			
			//create default straight variant
			ConfigurationNode normalNode = config.getNode("variants", "normal");
			if (normalNode.isVirtual() || isForgeStraightVariant(normalNode)) {
				normalNode.mergeValuesFrom(modelDefaults);
				
				Map<String, String> textures = new HashMap<>();
				for (Entry<Object, ? extends ConfigurationNode> entry : normalNode.getNode("textures").getChildrenMap().entrySet()) {
					if (entry.getKey().equals(JSON_COMMENT)) continue;
					
					textures.putIfAbsent(entry.getKey().toString(), entry.getValue().getString(null));
				}
				
				Variant variant = blockState.new Variant();
				variant.condition = PropertyCondition.all();
				variant.models.addAll(loadModels(normalNode, blockstateFile, textures));
				
				for (Entry<Object, ? extends ConfigurationNode> entry : normalNode.getNode("submodel").getChildrenMap().entrySet()) {
					if (entry.getKey().equals(JSON_COMMENT)) continue;
					
					variant.models.addAll(loadModels(entry.getValue(), blockstateFile, textures));
				}
				
				variant.updateTotalWeight();

				try {
					variant.checkValid();
					blockState.variants.add(variant);
				} catch (ParseResourceException ex) {
					Logger.global.logWarning("Failed to parse a variant (forge/straight) of " + blockstateFile + ": " + ex);
				}
				
			}

			return blockState;
		}

		private boolean isForgeStraightVariant(ConfigurationNode node) {
			if (node.hasListChildren())
				return true;

			for (Entry<Object, ? extends ConfigurationNode> entry : node.getChildrenMap().entrySet()) {
				if (entry.getKey().equals(JSON_COMMENT)) continue;
				if (!entry.getValue().hasMapChildren()) return true;
			}

			return false;
		}

		private class ForgeVariant {
			public Map<String, String> properties = new HashMap<>();
			public ConfigurationNode node = GsonConfigurationLoader.builder().build().createEmptyNode();

			public ForgeVariant createMerge(ForgeVariant other) {
				ForgeVariant merge = new ForgeVariant();

				merge.properties.putAll(this.properties);
				merge.properties.putAll(other.properties);

				merge.node.mergeValuesFrom(this.node);
				merge.node.mergeValuesFrom(other.node);

				return merge;
			}
		}

	}
}
