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
package de.bluecolored.bluemap.core.map.hires.block;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.map.hires.TileModelView;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.block.BlockNeighborhood;

import java.util.ArrayList;
import java.util.List;

public class BlockStateModelRenderer {

    private final ResourcePack resourcePack;
    private final LoadingCache<BlockRendererType, BlockRenderer> blockRenderers;

    private final List<Variant> variants = new ArrayList<>();

    public BlockStateModelRenderer(ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings) {
        this.resourcePack = resourcePack;
        this.blockRenderers = Caffeine.newBuilder()
                .build(type -> type.create(resourcePack, textureGallery, renderSettings));
    }

    public void render(BlockNeighborhood block, TileModelView blockModel, Color blockColor) {
        render(block, block.getBlockState(), blockModel, blockColor);
    }

    private final Color waterloggedColor = new Color();
    public void render(BlockNeighborhood block, BlockState blockState, TileModelView tileModel, Color blockColor) {
        blockColor.set(0, 0, 0, 0, true);

        //shortcut for air
        if (blockState.isAir()) return;

        int modelStart = tileModel.getStart();

        // render block
        renderModel(block, blockState, tileModel.initialize(), blockColor);

        // add water if block is waterlogged
        if (blockState.isWaterlogged() || block.getProperties().isAlwaysWaterlogged()) {
            waterloggedColor.set(0f, 0f, 0f, 0f, true);
            renderModel(block, BlockState.WATER, tileModel.initialize(), waterloggedColor);
            blockColor.set(waterloggedColor.overlay(blockColor.premultiplied()));
        }

        tileModel.initialize(modelStart);
    }

    private final Color variantColor = new Color();
    private void renderModel(BlockNeighborhood block, BlockState blockState, TileModelView tileModel, Color blockColor) {
        int modelStart = tileModel.getStart();

        var stateResource = resourcePack.getBlockState(blockState);
        if (stateResource == null) return;

        float blockColorOpacity = 0;
        variants.clear();
        stateResource.forEach(blockState, block.getX(), block.getY(), block.getZ(), variants::add);

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < variants.size(); i++) {
            variantColor.set(0f, 0f, 0f, 0f, true);

            Variant variant = variants.get(i);
            blockRenderers.get(variant.getRenderer())
                    .render(block, variant, tileModel.initialize(), variantColor);

            if (variantColor.a > blockColorOpacity)
                blockColorOpacity = variantColor.a;
            blockColor.add(variantColor.premultiplied());
        }

        if (blockColor.a > 0) {
            blockColor.flatten().straight();
            blockColor.a = blockColorOpacity;
        }

        tileModel.initialize(modelStart);
    }

}
