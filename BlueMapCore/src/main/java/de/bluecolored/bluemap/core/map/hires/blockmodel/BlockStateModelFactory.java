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

import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.map.hires.BlockModelView;
import de.bluecolored.bluemap.core.map.hires.RenderSettings;
import de.bluecolored.bluemap.core.resourcepack.*;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.BlockState;

import java.util.ArrayList;
import java.util.Collection;

public class BlockStateModelFactory {

	private final ResourcePack resourcePack;
	private final ResourceModelBuilder resourceModelBuilder;
	private final LiquidModelBuilder liquidModelBuilder;

	private final Collection<TransformedBlockModelResource> bmrs;
	
	public BlockStateModelFactory(ResourcePack resourcePack, RenderSettings renderSettings) {
		this.resourcePack = resourcePack;

		Block[] neighborCache = new Block[3 * 3 * 3];
		for (int i = 0; i < neighborCache.length; i++) {
			neighborCache[i] = new Block(null, 0, 0, 0);
		}

		this.resourceModelBuilder = new ResourceModelBuilder(resourcePack, renderSettings, neighborCache);
		this.liquidModelBuilder = new LiquidModelBuilder(resourcePack, renderSettings, neighborCache);

		this.bmrs = new ArrayList<>();
	}

	public void render(Block block, BlockModelView blockModel, Color blockColor) throws NoSuchResourceException {
		render(block, block.getBlockState(), blockModel, blockColor);
	}
	
	public void render(Block block, BlockState blockState, BlockModelView blockModel, Color blockColor) throws NoSuchResourceException {
		
		//shortcut for air
		if (blockState.isAir) return;

		int modelStart = blockModel.getStart();

		// render block
		renderModel(block, blockState, blockModel.initialize(), blockColor);
		
		// add water if block is waterlogged
		if (blockState.isWaterlogged) {
			renderModel(block, WATERLOGGED_BLOCKSTATE, blockModel.initialize(), blockColor);
		}

		blockModel.initialize(modelStart);

	}

	private void renderModel(Block block, BlockState blockState, BlockModelView blockModel, Color blockColor) throws NoSuchResourceException {
		int modelStart = blockModel.getStart();

		BlockStateResource resource = resourcePack.getBlockStateResource(blockState);
		for (TransformedBlockModelResource bmr : resource.getModels(blockState, block.getX(), block.getY(), block.getZ(), bmrs)){
			switch (bmr.getModel().getType()){
			case LIQUID:
				liquidModelBuilder.build(block, blockState, bmr, blockModel.initialize(), blockColor);
				break;
			default:
				resourceModelBuilder.build(block, bmr, blockModel.initialize(), blockColor);
				break;
			}
		}

		blockModel.initialize(modelStart);
	}
	
	private final static BlockState WATERLOGGED_BLOCKSTATE = new BlockState(MinecraftVersion.LATEST_SUPPORTED, "minecraft:water");
	
}
