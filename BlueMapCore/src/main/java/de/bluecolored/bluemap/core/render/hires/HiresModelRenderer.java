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
package de.bluecolored.bluemap.core.render.hires;

import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.flowpowered.math.vector.Vector4f;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.render.RenderSettings;
import de.bluecolored.bluemap.core.render.WorldTile;
import de.bluecolored.bluemap.core.render.hires.blockmodel.BlockStateModel;
import de.bluecolored.bluemap.core.render.hires.blockmodel.BlockStateModelFactory;
import de.bluecolored.bluemap.core.resourcepack.NoSuchResourceException;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.AABB;
import de.bluecolored.bluemap.core.util.MathUtils;
import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.BlockState;
import de.bluecolored.bluemap.core.world.World;

public class HiresModelRenderer {

	private RenderSettings renderSettings;
	private BlockStateModelFactory modelFactory;
	
	public HiresModelRenderer(ResourcePack resourcePack, RenderSettings renderSettings) {
		this.renderSettings = renderSettings;
		this.modelFactory = new BlockStateModelFactory(resourcePack, renderSettings);
	}
	
	public HiresModel render(WorldTile tile, AABB region) {
		Vector3i modelMin = region.getMin();
		Vector3i modelMax = region.getMax();
		
		Vector3i min = modelMin.max(renderSettings.getMin());
		Vector3i max = modelMax.min(renderSettings.getMax());
		
		World world = tile.getWorld();
		
		HiresModel model = new HiresModel(tile.getWorld().getUUID(), tile.getTile(), modelMin, modelMax);
		
		for (int x = min.getX(); x <= max.getX(); x++){
			for (int z = min.getZ(); z <= max.getZ(); z++){

				int maxHeight = 0;
				Vector4f color = Vector4f.ZERO;
				
				for (int y = min.getY(); y <= max.getY(); y++){
					Block block = world.getBlock(x, y, z);
					if (block.getBlockState().equals(BlockState.AIR)) continue;
					
					maxHeight = y;

					BlockStateModel blockModel;
					try {
						blockModel = modelFactory.createFrom(block);
					} catch (NoSuchResourceException e) {
						try {
							blockModel = modelFactory.createFrom(block, BlockState.MISSING);
						} catch (NoSuchResourceException e2) {
							e.addSuppressed(e2);
							blockModel = new BlockStateModel();
						}
						//Logger.global.noFloodDebug(block.getBlockState().getFullId() + "-hiresModelRenderer-blockmodelerr", "Failed to create BlockModel for BlockState: " + block.getBlockState() + " (" + e.toString() + ")");
					}
					
					blockModel.translate(new Vector3f(x, y, z).sub(modelMin.toFloat()));

					color = MathUtils.overlayColors(blockModel.getMapColor(), color);
					
					//TODO: quick hack to random offset grass
					if (block.getBlockState().getFullId().equals("minecraft:grass")){
						float dx = (MathUtils.hashToFloat(x, y, z, 123984) - 0.5f) * 0.75f;
						float dz = (MathUtils.hashToFloat(x, y, z, 345542) - 0.5f) * 0.75f;
						blockModel.translate(new Vector3f(dx, 0, dz));
					}
					
					model.merge(blockModel);
				}

				model.setHeight(x, z, maxHeight);
				model.setColor(x, z, color);
				
			}
		}
		
		return model;
	}
	
}
