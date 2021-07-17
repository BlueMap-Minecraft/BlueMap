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

import com.flowpowered.math.GenericMath;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.util.ConfigUtils;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.Block;
import org.spongepowered.configurate.ConfigurationNode;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@DebugDump
public class BlockColorCalculatorFactory {

	private BufferedImage foliageMap;
	private BufferedImage grassMap;

	private final Map<String, ColorFunction> blockColorMap;
	
	public BlockColorCalculatorFactory(BufferedImage foliageMap, BufferedImage grassMap) {
		this.foliageMap = foliageMap;
		this.grassMap = grassMap;
		
		this.blockColorMap = new HashMap<>();
	}
	
	public void loadColorConfig(ConfigurationNode colorConfig) {
		blockColorMap.clear();
		
		for (Entry<Object, ? extends ConfigurationNode> entry : colorConfig.childrenMap().entrySet()){
			String key = entry.getKey().toString();
			String value = entry.getValue().getString("");

			ColorFunction colorFunction;
			switch (value) {
			case "@foliage":
				colorFunction = BlockColorCalculator::getFoliageAverageColor;
				break;
			case "@grass":
				colorFunction = BlockColorCalculator::getGrassAverageColor;
				break;
			case "@water":
				colorFunction = BlockColorCalculator::getWaterAverageColor;
				break;
			case "@redstone":
				colorFunction = BlockColorCalculator::getRedstoneColor;
				break;
			default:
				final Color color = new Color();
				color.set(ConfigUtils.readColorInt(entry.getValue())).premultiplied();
				colorFunction = (calculator, block, target) -> target.set(color);
				break;
			}
			
			blockColorMap.put(key, colorFunction);
		}
	}

	public void setFoliageMap(BufferedImage foliageMap) {
		this.foliageMap = foliageMap;
	}

	public BufferedImage getFoliageMap() {
		return foliageMap;
	}

	public void setGrassMap(BufferedImage grassMap) {
		this.grassMap = grassMap;
	}

	public BufferedImage getGrassMap() {
		return grassMap;
	}

	public BlockColorCalculator createCalculator() {
		return new BlockColorCalculator();
	}

	@FunctionalInterface
	private interface ColorFunction {
		Color invoke(BlockColorCalculator calculator, Block block, Color target);
	}

	public class BlockColorCalculator {

		private final Color tempColor = new Color();

		public Color getBlockColor(Block block, Color target) {
			String blockId = block.getBlockState().getFullId();

			ColorFunction colorFunction = blockColorMap.get(blockId);
			if (colorFunction == null) colorFunction = blockColorMap.get("default");
			if (colorFunction == null) colorFunction = BlockColorCalculator::getFoliageAverageColor;

			return colorFunction.invoke(this, block, target);
		}

		public Color getRedstoneColor(Block block, Color target) {
			String powerString = block.getBlockState().getProperties().get("power");

			int power = 15;
			if (powerString != null) {
				power = Integer.parseInt(powerString);
			}

			return target.set(
					(power + 5f) / 20f, 0f, 0f,
					1f, true
			);
		}

		public Color getWaterAverageColor(Block block, Color target) {
			target.set(0, 0, 0, 0, true);

			int x, y, z,
					minX = block.getX() - 2,
					maxX = block.getX() + 2,
					minY = block.getY() - 1,
					maxY = block.getY() + 1,
					minZ = block.getZ() - 2,
					maxZ = block.getZ() + 2;

			for (x = minX; x <= maxX; x++) {
				for (y = minY; y <= maxY; y++) {
					for (z = minZ; z <= maxZ; z++) {
						target.add(block
								.getWorld()
								.getBiome(x, y, z)
								.getWaterColor()
						);
					}
				}
			}

			return target.flatten();
		}

		public Color getFoliageAverageColor(Block block, Color target) {
			target.set(0, 0, 0, 0, true);

			int x, y, z,
					minX = block.getX() - 2,
					maxX = block.getX() + 2,
					minY = block.getY() - 1,
					maxY = block.getY() + 1,
					minZ = block.getZ() - 2,
					maxZ = block.getZ() + 2;

			int seaLevel = block.getWorld().getSeaLevel();
			int blocksAboveSeaLevel;
			Biome biome;

			for (y = minY; y <= maxY; y++) {
				blocksAboveSeaLevel = Math.max(block.getY() - seaLevel, 0);

				for (x = minX; x <= maxX; x++) {
					for (z = minZ; z <= maxZ; z++) {
						biome = block.getWorld().getBiome(x, y, z);
						target.add(getFoliageColor(biome, blocksAboveSeaLevel, tempColor));
					}
				}
			}

			return target.flatten();
		}

		public Color getFoliageColor(Biome biome, int blocksAboveSeaLevel, Color target) {
			getColorFromMap(biome, blocksAboveSeaLevel, foliageMap, target);
			return target.overlay(biome.getOverlayFoliageColor());
		}

		public Color getGrassAverageColor(Block block, Color target) {
			target.set(0, 0, 0, 0, true);

			int x, y, z,
					minX = block.getX() - 2,
					maxX = block.getX() + 2,
					minY = block.getY() - 1,
					maxY = block.getY() + 1,
					minZ = block.getZ() - 2,
					maxZ = block.getZ() + 2;

			int seaLevel = block.getWorld().getSeaLevel();
			int blocksAboveSeaLevel;
			Biome biome;

			for (y = minY; y <= maxY; y++) {
				blocksAboveSeaLevel = Math.max(block.getY() - seaLevel, 0);

				for (x = minX; x <= maxX; x++) {
					for (z = minZ; z <= maxZ; z++) {
						biome = block.getWorld().getBiome(x, y, z);
						target.add(getGrassColor(biome, blocksAboveSeaLevel, tempColor));
					}
				}
			}

			return target.flatten();
		}

		public Color getGrassColor(Biome biome, int blocksAboveSeaLevel, Color target) {
			getColorFromMap(biome, blocksAboveSeaLevel, grassMap, target);
			return target.overlay(biome.getOverlayGrassColor());
		}

		private void getColorFromMap(Biome biome, int blocksAboveSeaLevel, BufferedImage map, Color target) {
			float adjTemp = (float) GenericMath.clamp(biome.getTemp() - (0.00166667 * blocksAboveSeaLevel), 0, 1);
			float adjHumidity = (float) GenericMath.clamp(biome.getHumidity(), 0, 1) * adjTemp;

			int x = (int) ((1 - adjTemp) * map.getWidth());
			int y = (int) ((1 - adjHumidity) * map.getHeight());

			int cValue = map.getRGB(
					GenericMath.clamp(x, 0, map.getWidth() - 1),
					GenericMath.clamp(y, 0, map.getHeight() - 1)
			);

			target.set(cValue);
		}

	}
	
}
