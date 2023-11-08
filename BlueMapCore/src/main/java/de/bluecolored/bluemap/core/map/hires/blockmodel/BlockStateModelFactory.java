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
package de.bluecolored.bluemap.core.map.hires.blockmodel;

import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.BlockModelView;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.resources.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.resourcepack.blockmodel.BlockModel;
import de.bluecolored.bluemap.core.resources.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;
import de.bluecolored.bluemap.core.world.BlockState;

import java.util.ArrayList;
import java.util.List;

public class BlockStateModelFactory {

    private final ResourcePack resourcePack;
    private final ResourceModelBuilder resourceModelBuilder;
    private final LiquidModelBuilder liquidModelBuilder;

    private final List<Variant> variants = new ArrayList<>();

    public BlockStateModelFactory(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        this.resourcePack = resourcePack;

        this.resourceModelBuilder = new ResourceModelBuilder(resourcePack, textureGallery, renderSettings);
        this.liquidModelBuilder = new LiquidModelBuilder(resourcePack, textureGallery, renderSettings);
    }

    public void render(BlockNeighborhood<?> block, BlockModelView blockModel, Color blockColor) {
        render(block, block.getBlockState(), blockModel, blockColor);
    }

    private final Color waterloggedColor = new Color();
    public void render(BlockNeighborhood<?> block, BlockState blockState, BlockModelView blockModel, Color blockColor) {
        blockColor.set(0, 0, 0, 0, true);

        //shortcut for air
        if (blockState.isAir()) return;

        int modelStart = blockModel.getStart();

        // render block
        renderModel(block, blockState, blockModel.initialize(), blockColor);

        // add water if block is waterlogged
        if (blockState.isWaterlogged() || block.getProperties().isAlwaysWaterlogged()) {
            waterloggedColor.set(0f, 0f, 0f, 0f, true);
            renderModel(block, WATERLOGGED_BLOCKSTATE, blockModel.initialize(), waterloggedColor);
            blockColor.set(waterloggedColor.overlay(blockColor.premultiplied()));
        }

        blockModel.initialize(modelStart);
    }

    private final Color variantColor = new Color();
    private void renderModel(BlockNeighborhood<?> block, BlockState blockState, BlockModelView blockModel, Color blockColor) {
        int modelStart = blockModel.getStart();

        var stateResource = resourcePack.getBlockState(blockState);
        if (stateResource == null) return;

        float blockColorOpacity = 0;
        variants.clear();
        stateResource.forEach(blockState, block.getX(), block.getY(), block.getZ(), variants::add);

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < variants.size(); i++) {
            Variant variant = variants.get(i);

            BlockModel modelResource = variant.getModel().getResource(resourcePack::getBlockModel);
            if (modelResource == null) continue;

            variantColor.set(0f, 0f, 0f, 0f, true);

            if (modelResource.isLiquid()) {
                liquidModelBuilder.build(block, blockState, variant, blockModel.initialize(), variantColor);
            } else {
                resourceModelBuilder.build(block, variant, blockModel.initialize(), variantColor);
            }

            if (variantColor.a > blockColorOpacity)
                blockColorOpacity = variantColor.a;

            blockColor.add(variantColor.premultiplied());
        }

        if (blockColor.a > 0) {
            blockColor.flatten().straight();
            blockColor.a = blockColorOpacity;
        }

        blockModel.initialize(modelStart);
    }

    private final static BlockState WATERLOGGED_BLOCKSTATE = new BlockState("minecraft:water");

}
