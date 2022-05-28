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
import de.bluecolored.bluemap.core.world.BlockNeighborhood;
import de.bluecolored.bluemap.core.world.BlockState;

import java.util.ArrayList;
import java.util.Collection;

public class BlockStateModelFactory {

    private final ResourcePack resourcePack;
    private final ResourceModelBuilder resourceModelBuilder;
    private final LiquidModelBuilder liquidModelBuilder;

    private final Collection<Variant> variants = new ArrayList<>();

    public BlockStateModelFactory(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        this.resourcePack = resourcePack;

        this.resourceModelBuilder = new ResourceModelBuilder(resourcePack, textureGallery, renderSettings);
        this.liquidModelBuilder = new LiquidModelBuilder(resourcePack, textureGallery, renderSettings);
    }

    public void render(BlockNeighborhood<?> block, BlockModelView blockModel, Color blockColor) {
        render(block, block.getBlockState(), blockModel, blockColor);
    }

    public void render(BlockNeighborhood<?> block, BlockState blockState, BlockModelView blockModel, Color blockColor) {

        //shortcut for air
        if (blockState.isAir()) return;

        int modelStart = blockModel.getStart();

        // render block
        renderModel(block, blockState, blockModel.initialize(), blockColor);

        // add water if block is waterlogged
        if (blockState.isWaterlogged() || block.getProperties().isAlwaysWaterlogged()) {
            renderModel(block, WATERLOGGED_BLOCKSTATE, blockModel.initialize(), blockColor);
        }

        blockModel.initialize(modelStart);
    }

    private void renderModel(BlockNeighborhood<?> block, BlockState blockState, BlockModelView blockModel, Color blockColor) {
        int modelStart = blockModel.getStart();

        var stateResource = resourcePack.getBlockState(blockState);
        if (stateResource == null) return;

        variants.clear();
        stateResource.forEach(blockState, block.getX(), block.getY(), block.getZ(), variants::add);
        for (Variant variant : variants) {
            BlockModel modelResource = variant.getModel().getResource(resourcePack::getBlockModel);
            if (modelResource == null) continue;

            if (modelResource.isLiquid()) {
                liquidModelBuilder.build(block, blockState, variant, blockModel.initialize(), blockColor);
            } else {
                resourceModelBuilder.build(block, variant, blockModel.initialize(), blockColor);
            }
        }

        blockModel.initialize(modelStart);
    }

    private final static BlockState WATERLOGGED_BLOCKSTATE = new BlockState("minecraft:water");

}
