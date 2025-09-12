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
package de.bluecolored.bluemap.core.map.hires.entity;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.entitystate.Part;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.Entity;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import de.bluecolored.bluemap.core.world.mca.entity.Llama;

public class LlamaRenderer extends ResourceModelRenderer {

    private final ResourcePath<Model>
            LLAMA_CREAMY = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/color/llama_creamy"),
            LLAMA_WHITE = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/color/llama_white"),
            LLAMA_BROWN = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/color/llama_brown"),
            LLAMA_GRAY = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/color/llama_gray"),
            LLAMA_CHEST = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/llama_chest"),
            LLAMA_CARPET_BLACK = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_black"),
            LLAMA_CARPET_BLUE = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_blue"),
            LLAMA_CARPET_BROWN = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_brown"),
            LLAMA_CARPET_CYAN = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_cyan"),
            LLAMA_CARPET_GRAY = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_gray"),
            LLAMA_CARPET_GREEN = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_green"),
            LLAMA_CARPET_LIGHT_BLUE = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_light_blue"),
            LLAMA_CARPET_LIGHT_GRAY = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_light_gray"),
            LLAMA_CARPET_LIME = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_lime"),
            LLAMA_CARPET_MAGENTA = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_magenta"),
            LLAMA_CARPET_ORANGE = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_orange"),
            LLAMA_CARPET_PINK = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_pink"),
            LLAMA_CARPET_PURPLE = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_purple"),
            LLAMA_CARPET_RED = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_red"),
            LLAMA_CARPET_WHITE = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_white"),
            LLAMA_CARPET_YELLOW = new ResourcePath<>(Key.MINECRAFT_NAMESPACE, "entity/llama/carpet/llama_yellow");

    public LlamaRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        super(resourcePack, textureGallery, renderSettings);
    }

    @Override
    public void render(Entity entity, BlockNeighborhood block, Part part, TileModelView tileModel) {
        if (!(entity instanceof Llama llama)) return;

        // base model
        ResourcePath<Model> baseModel = switch (llama.getVariant()) {
            case CREAMY -> LLAMA_CREAMY;
            case WHITE -> LLAMA_WHITE;
            case BROWN -> LLAMA_BROWN;
            case GRAY -> LLAMA_GRAY;
        };
        super.render(entity, block, baseModel.getResource(getModelProvider()), TintColorProvider.NO_TINT, tileModel);

        // chest model
        if (llama.isWithChest()) {
            super.render(entity, block, LLAMA_CHEST.getResource(getModelProvider()), TintColorProvider.NO_TINT, tileModel);
        }

        // decoration model
        ResourcePath<Model> decorationModel = switch (llama.getSaddle().id().getFormatted()) {
            case "minecraft:black_carpet" -> LLAMA_CARPET_BLACK;
            case "minecraft:blue_carpet" -> LLAMA_CARPET_BLUE;
            case "minecraft:brown_carpet" -> LLAMA_CARPET_BROWN;
            case "minecraft:cyan_carpet" -> LLAMA_CARPET_CYAN;
            case "minecraft:gray_carpet" -> LLAMA_CARPET_GRAY;
            case "minecraft:green_carpet" -> LLAMA_CARPET_GREEN;
            case "minecraft:light_blue_carpet" -> LLAMA_CARPET_LIGHT_BLUE;
            case "minecraft:light_gray_carpet" -> LLAMA_CARPET_LIGHT_GRAY;
            case "minecraft:lime_carpet" -> LLAMA_CARPET_LIME;
            case "minecraft:magenta_carpet" -> LLAMA_CARPET_MAGENTA;
            case "minecraft:orange_carpet" -> LLAMA_CARPET_ORANGE;
            case "minecraft:pink_carpet" -> LLAMA_CARPET_PINK;
            case "minecraft:purple_carpet" -> LLAMA_CARPET_PURPLE;
            case "minecraft:red_carpet" -> LLAMA_CARPET_RED;
            case "minecraft:white_carpet" -> LLAMA_CARPET_WHITE;
            case "minecraft:yellow_carpet" -> LLAMA_CARPET_YELLOW;

            default -> null;
        };
        if (decorationModel != null) {
            super.render(entity, block, decorationModel.getResource(getModelProvider()), TintColorProvider.NO_TINT, tileModel);
        }

        // apply part transform
        if (part.isTransformed())
            tileModel.transform(part.getTransformMatrix());
    }

}
