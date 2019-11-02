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
package de.bluecolored.bluemap.core.render.hires.blockmodel;

import de.bluecolored.bluemap.core.render.RenderSettings;
import de.bluecolored.bluemap.core.render.context.EmptyBlockContext;
import de.bluecolored.bluemap.core.render.context.ExtendedBlockContext;
import de.bluecolored.bluemap.core.resourcepack.BlockStateResource;
import de.bluecolored.bluemap.core.resourcepack.InvalidResourceDeclarationException;
import de.bluecolored.bluemap.core.resourcepack.NoSuchResourceException;
import de.bluecolored.bluemap.core.resourcepack.NoSuchTextureException;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.world.BlockState;

public class BlockStateModelFactory {

	private ResourcePack resourcePack;
	
	public BlockStateModelFactory(ResourcePack resources) {
		this.resourcePack = resources;
	}

	public BlockStateModel createFrom(BlockState blockState) throws NoSuchResourceException, InvalidResourceDeclarationException, NoSuchTextureException {
		return createFrom(blockState, EmptyBlockContext.instance(), new RenderSettings() {
			@Override
			public float getLightShadeMultiplier() {
				return 0;
			}
			
			@Override
			public boolean isExcludeFacesWithoutSunlight() {
				return false;
			}

			@Override
			public float getAmbientOcclusionStrenght() {
				return 0;
			}
		});
	}
	
	public BlockStateModel createFrom(BlockState blockState, ExtendedBlockContext context, RenderSettings renderSettings) throws NoSuchResourceException, InvalidResourceDeclarationException, NoSuchTextureException {

		//air won't be rendered
		if (
				blockState.getId().equals("air") ||
				blockState.getId().equals("cave_air") ||
				blockState.getId().equals("void_air")
		) {
			return new BlockStateModel();
		}
		
		// if it is a liquid, use the LiquidModelBuilder
		if (
				blockState.getId().equals("water") ||
				blockState.getId().equals("lava")
		){
			return new LiquidModelBuilder(blockState, context, resourcePack, renderSettings).build();
		}
		
		// if no other model builder matched try to find a model definition from the resource-packs and use the default ResourceModelBuilder
		BlockStateResource resource = resourcePack.getBlockStateResource(blockState);
		BlockStateModel model = new ResourceModelBuilder(resource, context, resourcePack, renderSettings).build();
		
		// if block is waterlogged
		if (LiquidModelBuilder.isWaterlogged(blockState)) {
			BlockStateModel watermodel = new LiquidModelBuilder(WATERLOGGED_BLOCKSTATE, context, resourcePack, renderSettings).build();
			model.merge(watermodel);
		}
		
		return model;
	}
	
	private BlockState WATERLOGGED_BLOCKSTATE = new BlockState("minecraft:water");
	
}
