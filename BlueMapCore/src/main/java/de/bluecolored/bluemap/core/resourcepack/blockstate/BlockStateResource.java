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
package de.bluecolored.bluemap.core.resourcepack.blockstate;

import com.flowpowered.math.vector.Vector2f;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.ParseResourceException;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resourcepack.blockmodel.BlockModelResource;
import de.bluecolored.bluemap.core.resourcepack.blockmodel.TransformedBlockModelResource;
import de.bluecolored.bluemap.core.resourcepack.blockstate.PropertyCondition.All;
import de.bluecolored.bluemap.core.resourcepack.fileaccess.FileAccess;
import de.bluecolored.bluemap.core.world.BlockState;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

public class BlockStateResource {

    private final List<Variant> variants = new ArrayList<>(0);
    private final Collection<Variant> multipart = new ArrayList<>(0);

    private BlockStateResource() {}

    public Collection<TransformedBlockModelResource> getModels(BlockState blockState, Collection<TransformedBlockModelResource> targetCollection) {
        return getModels(blockState, 0, 0, 0, targetCollection);
    }

    public Collection<TransformedBlockModelResource> getModels(BlockState blockState, int x, int y, int z, Collection<TransformedBlockModelResource> targetCollection) {
        targetCollection.clear();

        Variant allMatch = null;
        for (Variant variant : variants) {
            if (variant.condition.matches(blockState)) {
                if (variant.condition instanceof All) { //only use "all" condition if nothing else matched
                    if (allMatch == null) allMatch = variant;
                    continue;
                }

                targetCollection.add(variant.getModel(x, y, z));
                return targetCollection;
            }
        }

        if (allMatch != null) {
            targetCollection.add(allMatch.getModel(x, y, z));
            return targetCollection;
        }

        for (Variant variant : multipart) {
            if (variant.condition.matches(blockState)) {
                targetCollection.add(variant.getModel(x, y, z));
            }
        }

        //fallback to first variant
        if (targetCollection.isEmpty() && !variants.isEmpty()) {
            targetCollection.add(variants.get(0).getModel(x, y, z));
        }

        return targetCollection;
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

            InputStream fileIn = sourcesAccess.readFile(blockstateFile);
            ConfigurationNode config = GsonConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new InputStreamReader(fileIn, StandardCharsets.UTF_8)))
                    .build()
                    .load();

            if (!config.node("forge_marker").virtual()) {
                return buildForge(config, blockstateFile);
            }

            BlockStateResource blockState = new BlockStateResource();

            // create variants
            for (Entry<Object, ? extends ConfigurationNode> entry : config.node("variants").childrenMap().entrySet()) {
                if (entry.getKey().equals(JSON_COMMENT)) continue;

                try {
                    String conditionString = entry.getKey().toString();
                    ConfigurationNode transformedModelNode = entry.getValue();

                    //some exceptions in 1.12 resource packs that we ignore
                    if (conditionString.equals("all") || conditionString.equals("map")) continue;

                    Variant variant = new Variant();
                    variant.condition = parseConditionString(conditionString);
                    variant.models = loadModels(transformedModelNode, blockstateFile, null);

                    variant.updateTotalWeight();
                    variant.checkValid();

                    blockState.variants.add(variant);
                } catch (ParseResourceException | RuntimeException e) {
                    logParseError("Failed to parse a variant of " + blockstateFile, e);
                }
            }

            // create multipart
            for (ConfigurationNode partNode : config.node("multipart").childrenList()) {
                try {
                    Variant variant = new Variant();
                    ConfigurationNode whenNode = partNode.node("when");
                    if (!whenNode.virtual()) {
                        variant.condition = parseCondition(whenNode);
                    }
                    variant.models = loadModels(partNode.node("apply"), blockstateFile, null);

                    variant.updateTotalWeight();
                    variant.checkValid();

                    blockState.multipart.add(variant);
                } catch (ParseResourceException | RuntimeException e) {
                    logParseError("Failed to parse a multipart-part of " + blockstateFile, e);
                }
            }

            return blockState;
        }

        private Collection<Weighted<TransformedBlockModelResource>> loadModels(ConfigurationNode node, String blockstateFile, Map<String, String> overrideTextures) {
            Collection<Weighted<TransformedBlockModelResource>> models = new ArrayList<>();

            if (node.isList()) {
                for (ConfigurationNode modelNode : node.childrenList()) {
                    try {
                        models.add(loadModel(modelNode, overrideTextures));
                    } catch (ParseResourceException ex) {
                        logParseError("Failed to load a model trying to parse " + blockstateFile, ex);
                    }
                }
            } else if (node.isMap()) {
                try {
                    models.add(loadModel(node, overrideTextures));
                } catch (ParseResourceException ex) {
                    logParseError("Failed to load a model trying to parse " + blockstateFile, ex);
                }
            }

            return models;
        }

        private Weighted<TransformedBlockModelResource> loadModel(ConfigurationNode node, Map<String, String> overrideTextures) throws ParseResourceException {
            String namespacedModelPath = node.node("model").getString();
            if (namespacedModelPath == null)
                throw new ParseResourceException("No model defined!");

            String modelPath = ResourcePack.namespacedToAbsoluteResourcePath(namespacedModelPath, "models") + ".json";
            BlockModelResource model = resourcePack.getBlockModelResources().get(modelPath);
            if (model == null) {
                BlockModelResource.Builder builder = BlockModelResource.builder(sourcesAccess, resourcePack);
                try {
                    if (overrideTextures != null) model = builder.build(modelPath, overrideTextures);
                    else model = builder.build(modelPath);
                } catch (IOException e) {
                    throw new ParseResourceException("Failed to load model " + modelPath, e);
                }

                resourcePack.getBlockModelResources().put(modelPath, model);
            }

            Vector2f rotation = new Vector2f(node.node("x").getFloat(0), node.node("y").getFloat(0));
            boolean uvLock = node.node("uvlock").getBoolean(false);

            TransformedBlockModelResource transformedModel = new TransformedBlockModelResource(rotation, uvLock, model);
            return new Weighted<>(transformedModel, node.node("weight").getDouble(1d));
        }

        private PropertyCondition parseCondition(ConfigurationNode conditionNode) {
            List<PropertyCondition> andConditions = new ArrayList<>();
            for (Entry<Object, ? extends ConfigurationNode> entry : conditionNode.childrenMap().entrySet()) {
                String key = entry.getKey().toString();
                if (key.equals(JSON_COMMENT)) continue;

                if (key.equals("OR")) {
                    List<PropertyCondition> orConditions = new ArrayList<>();
                    for (ConfigurationNode orConditionNode : entry.getValue().childrenList()) {
                        orConditions.add(parseCondition(orConditionNode));
                    }
                    andConditions.add(
                            PropertyCondition.or(orConditions.toArray(new PropertyCondition[0])));
                } else {
                    String[] values = StringUtils.split(entry.getValue().getString(""), '|');
                    andConditions.add(PropertyCondition.property(key, values));
                }
            }

            return PropertyCondition.and(andConditions.toArray(new PropertyCondition[0]));
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
                condition = PropertyCondition.and(conditions.toArray(new PropertyCondition[0]));
            }

            return condition;
        }

        private BlockStateResource buildForge(ConfigurationNode config, String blockstateFile) {
            ConfigurationNode modelDefaults = config.node("defaults");

            List<ForgeVariant> variants = new ArrayList<>();
            for (Entry<Object, ? extends ConfigurationNode> entry : config.node("variants").childrenMap().entrySet()) {
                if (entry.getKey().equals(JSON_COMMENT)) continue;
                if (isForgeStraightVariant(entry.getValue())) continue;

                // create variants for single property
                List<ForgeVariant> propertyVariants = new ArrayList<>();
                String key = entry.getKey().toString();
                for (Entry<Object, ? extends ConfigurationNode> value : entry.getValue().childrenMap().entrySet()) {
                    if (value.getKey().equals(JSON_COMMENT)) continue;

                    ForgeVariant variant = new ForgeVariant();
                    variant.properties.put(key, value.getKey().toString());
                    variant.node = value.getValue();
                    propertyVariants.add(variant);
                }

                // join variants
                if (variants.isEmpty()){
                    variants = propertyVariants;
                } else {
                    List<ForgeVariant> oldVariants = variants;
                    variants = new ArrayList<>(oldVariants.size() * propertyVariants.size());
                    for (ForgeVariant oldVariant : oldVariants) {
                        for (ForgeVariant addVariant : propertyVariants) {
                            variants.add(oldVariant.createMerge(addVariant));
                        }
                    }
                }

            }

            //create all possible property-variants
            BlockStateResource blockState = new BlockStateResource();
            for (ForgeVariant forgeVariant : variants) {
                Variant variant = new Variant();

                ConfigurationNode modelNode = forgeVariant.node.mergeFrom(modelDefaults);

                Map<String, String> textures = new HashMap<>();
                for (Entry<Object, ? extends ConfigurationNode> entry : modelNode.node("textures").childrenMap().entrySet()) {
                    if (entry.getKey().equals(JSON_COMMENT)) continue;

                    textures.putIfAbsent(entry.getKey().toString(), entry.getValue().getString());
                }

                List<PropertyCondition> conditions = new ArrayList<>(forgeVariant.properties.size());
                for (Entry<String, String> property : forgeVariant.properties.entrySet()) {
                    conditions.add(PropertyCondition.property(property.getKey(), property.getValue()));
                }
                variant.condition = PropertyCondition.and(conditions.toArray(new PropertyCondition[0]));

                variant.models.addAll(loadModels(modelNode, blockstateFile, textures));

                for (Entry<Object, ? extends ConfigurationNode> entry : modelNode.node("submodel").childrenMap().entrySet()) {
                    if (entry.getKey().equals(JSON_COMMENT)) continue;

                    variant.models.addAll(loadModels(entry.getValue(), blockstateFile, textures));
                }

                variant.updateTotalWeight();

                try {
                    variant.checkValid();
                    blockState.variants.add(variant);
                } catch (ParseResourceException ex) {
                    logParseError("Failed to parse a variant (forge/property) of " + blockstateFile, ex);
                }

            }

            //create default straight variant
            ConfigurationNode normalNode = config.node("variants", "normal");
            if (normalNode.virtual() || isForgeStraightVariant(normalNode)) {
                normalNode.mergeFrom(modelDefaults);

                Map<String, String> textures = new HashMap<>();
                for (Entry<Object, ? extends ConfigurationNode> entry : normalNode.node("textures").childrenMap().entrySet()) {
                    if (entry.getKey().equals(JSON_COMMENT)) continue;

                    textures.putIfAbsent(entry.getKey().toString(), entry.getValue().getString());
                }

                Variant variant = new Variant();
                variant.condition = PropertyCondition.all();
                variant.models.addAll(loadModels(normalNode, blockstateFile, textures));

                for (Entry<Object, ? extends ConfigurationNode> entry : normalNode.node("submodel").childrenMap().entrySet()) {
                    if (entry.getKey().equals(JSON_COMMENT)) continue;

                    variant.models.addAll(loadModels(entry.getValue(), blockstateFile, textures));
                }

                variant.updateTotalWeight();

                try {
                    variant.checkValid();
                    blockState.variants.add(variant);
                } catch (ParseResourceException ex) {
                    logParseError("Failed to parse a variant (forge/straight) of " + blockstateFile, ex);
                }

            }

            return blockState;
        }

        private boolean isForgeStraightVariant(ConfigurationNode node) {
            if (node.isList())
                return true;

            for (Entry<Object, ? extends ConfigurationNode> entry : node.childrenMap().entrySet()) {
                if (entry.getKey().equals(JSON_COMMENT)) continue;
                if (!entry.getValue().isMap()) return true;
            }

            return false;
        }

        private static class ForgeVariant {
            public Map<String, String> properties = new HashMap<>();
            public ConfigurationNode node = GsonConfigurationLoader.builder().build().createNode();

            public ForgeVariant createMerge(ForgeVariant other) {
                ForgeVariant merge = new ForgeVariant();

                merge.properties.putAll(this.properties);
                merge.properties.putAll(other.properties);

                merge.node.mergeFrom(this.node);
                merge.node.mergeFrom(other.node);

                return merge;
            }
        }

        private static void logParseError(String message, Throwable throwable) {
            Logger.global.logDebug(message);
            while (throwable != null){
                String errorMessage = throwable.getMessage();
                if (errorMessage == null) errorMessage = throwable.toString();
                Logger.global.logDebug(" > " + errorMessage);

                //Logger.global.logError("DETAIL: ", throwable);

                throwable = throwable.getCause();
            }
        }

    }

}
