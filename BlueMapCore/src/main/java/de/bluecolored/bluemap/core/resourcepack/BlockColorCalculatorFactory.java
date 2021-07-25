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
import de.bluecolored.bluemap.core.world.BlockNeighborhood;
import org.spongepowered.configurate.ConfigurationNode;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@DebugDump
public class BlockColorCalculatorFactory {

	private final int[] foliageMap = new int[65536];
	private final int[] grassMap = new int[65536];

	private final Map<String, ColorFunction> blockColorMap;
	
	public BlockColorCalculatorFactory() {
		this.blockColorMap = new HashMap<>();
	}
	
	public void load(ConfigurationNode colorConfig) {
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
		foliageMap.getRGB(0, 0, 256, 256, this.foliageMap, 0, 256);
	}

	public void setGrassMap(BufferedImage grassMap) {
		grassMap.getRGB(0, 0, 256, 256, this.grassMap, 0, 256);
	}

	public BlockColorCalculator createCalculator() {
		return new BlockColorCalculator();
	}

	@FunctionalInterface
	private interface ColorFunction {
		Color invoke(BlockColorCalculator calculator, BlockNeighborhood block, Color target);
	}

	public class BlockColorCalculator {

		private final Color tempColor = new Color();

		public Color getBlockColor(BlockNeighborhood block, Color target) {
			String blockId = block.getBlockState().getFullId();

			ColorFunction colorFunction = blockColorMap.get(blockId);
			if (colorFunction == null) colorFunction = blockColorMap.get("default");
			if (colorFunction == null) colorFunction = BlockColorCalculator::getFoliageAverageColor;

			return colorFunction.invoke(this, block, target);
		}

		public Color getRedstoneColor(BlockNeighborhood block, Color target) {
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

		public Color getWaterAverageColor(BlockNeighborhood block, Color target) {
			target.set(0, 0, 0, 0, true);

			int x, y, z,
					minX = - 2,
					maxX = + 2,
					minY = - 1,
					maxY = + 1,
					minZ = - 2,
					maxZ = + 2;

			Biome biome;
			for (x = minX; x <= maxX; x++) {
				for (y = minY; y <= maxY; y++) {
					for (z = minZ; z <= maxZ; z++) {
						biome = block.getNeighborBlock(x, y, z).getBiome();
						target.add(biome.getWaterColor());
					}
				}
			}

			return target.flatten();
		}

		public Color getFoliageAverageColor(BlockNeighborhood block, Color target) {
			target.set(0, 0, 0, 0, true);

			int x, y, z,
					minX = - 2,
					maxX = + 2,
					minY = - 1,
					maxY = + 1,
					minZ = - 2,
					maxZ = + 2;

			Biome biome;
			for (y = minY; y <= maxY; y++) {
				for (x = minX; x <= maxX; x++) {
					for (z = minZ; z <= maxZ; z++) {
						biome = block.getNeighborBlock(x, y, z).getBiome();
						target.add(getFoliageColor(biome, tempColor));
					}
				}
			}

			return target.flatten();
		}

		public Color getFoliageColor(Biome biome, Color target) {
			getColorFromMap(biome, foliageMap, 4764952, target);
			return target.overlay(biome.getOverlayFoliageColor());
		}

		public Color getGrassAverageColor(BlockNeighborhood block, Color target) {
			target.set(0, 0, 0, 0, true);

			int x, y, z,
					minX = - 2,
					maxX = + 2,
					minY = - 1,
					maxY = + 1,
					minZ = - 2,
					maxZ = + 2;

			Biome biome;
			for (y = minY; y <= maxY; y++) {
				for (x = minX; x <= maxX; x++) {
					for (z = minZ; z <= maxZ; z++) {
						biome = block.getNeighborBlock(x, y, z).getBiome();
						target.add(getGrassColor(biome, tempColor));
					}
				}
			}

			return target.flatten();
		}

		public Color getGrassColor(Biome biome, Color target) {
			getColorFromMap(biome, grassMap, 0xff52952f, target);
			return target.overlay(biome.getOverlayGrassColor());
		}

		private void getColorFromMap(Biome biome, int[] colorMap, int defaultColor, Color target) {
			double temperature = GenericMath.clamp(biome.getTemp(), 0, 1);
			double humidity = GenericMath.clamp(biome.getHumidity(), 0, 1) * temperature;

			int x = (int) ((1.0 - temperature) * 255.0);
			int y = (int) ((1.0 - humidity) * 255.0);

			int index = y << 8 | x;
			int color = index >= colorMap.length ? defaultColor : colorMap[index];

			target.set(color);
		}

	}
	
}
