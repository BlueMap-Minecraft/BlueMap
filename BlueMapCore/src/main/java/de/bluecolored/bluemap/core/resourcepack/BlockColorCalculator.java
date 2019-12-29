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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;

import de.bluecolored.bluemap.core.render.context.BlockContext;
import de.bluecolored.bluemap.core.util.ConfigUtils;
import de.bluecolored.bluemap.core.util.MathUtils;
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.Block;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;

public class BlockColorCalculator {

	private BufferedImage foliageMap;
	private BufferedImage grassMap;
	
	private Map<String, Function<BlockContext, Vector3f>> blockColorMap;
	
	public BlockColorCalculator(BufferedImage foliageMap, BufferedImage grassMap) {
		this.foliageMap = foliageMap;
		this.grassMap = grassMap;
		
		this.blockColorMap = new HashMap<>();
	}
	
	public void loadColorConfig(File configFile) throws IOException {
		blockColorMap.clear();
		
		ConfigurationNode colorConfig = GsonConfigurationLoader.builder()
				.setFile(configFile)
				.build()
				.load();
		
		for (Entry<Object, ? extends ConfigurationNode> entry : colorConfig.getChildrenMap().entrySet()){
			String key = entry.getKey().toString();
			String value = entry.getValue().getString();
			
			Function<BlockContext, Vector3f> colorFunction;
			switch (value) {
			case "@foliage":
				colorFunction = this::getFoliageAverageColor;
				break;
			case "@grass":
				colorFunction = this::getGrassAverageColor;
				break;
			case "@water":
				colorFunction = this::getWaterAverageColor;
				break;
			default:
				final Vector3f color = MathUtils.color3FromInt(ConfigUtils.readInt(entry.getValue()));
				colorFunction = context -> color;
				break;
			}
			
			blockColorMap.put(key, colorFunction);
		}
	}
	
	public Vector3f getBlockColor(BlockContext context){
		Block block = context.getRelativeBlock(0, 0, 0);
		String blockId = block.getBlock().getFullId();
		
		Function<BlockContext, Vector3f> colorFunction = blockColorMap.get(blockId);
		if (colorFunction == null) colorFunction = blockColorMap.get("default");
		if (colorFunction == null) colorFunction = this::getFoliageAverageColor;
		
		return colorFunction.apply(context);
	}
	
	public Vector3f getWaterAverageColor(BlockContext context){
		Vector3f color = Vector3f.ZERO;
		
		for (int x = -1; x <= 1; x++){
			for (int z = -1; z <= 1; z++){
				color = color.add(context.getRelativeBlock(x, 0, z).getBiome().getWaterColor());
			}
		}
		
		return color.div(9f);
	}

	public Vector3f getFoliageAverageColor(BlockContext context){
		Vector3f color = Vector3f.ZERO;
		
		for (int x = -1; x <= 1; x++){
			for (int z = -1; z <= 1; z++){
				color = color.add(getFoliageColor(context.getRelativeBlock(x, 0, z)));
			}
		}
		
		return color.div(9f);
	}

	public Vector3f getFoliageColor(Block block){
		int blocksAboveSeaLevel = Math.max(block.getPosition().getY() - block.getWorld().getSeaLevel(), 0);
		return getFoliageColor(block.getBiome(), blocksAboveSeaLevel);
	}
	
	public Vector3f getFoliageColor(Biome biome, int blocksAboveSeaLevel){
		Vector3f mapColor = getColorFromMap(biome, blocksAboveSeaLevel, foliageMap);
		Vector3f overlayColor = biome.getOverlayFoliageColor().toVector3();
		float overlayAlpha = biome.getOverlayFoliageColor().getW();
		return mapColor.mul(1f - overlayAlpha).add(overlayColor.mul(overlayAlpha));
	}

	public Vector3f getGrassAverageColor(BlockContext context){
		Vector3f color = Vector3f.ZERO;
		
		for (int x = -1; x <= 1; x++){
			for (int z = -1; z <= 1; z++){
				color = color.add(getGrassColor(context.getRelativeBlock(x, 0, z)));
			}
		}
		
		return color.div(9f);
	}
	
	public Vector3f getGrassColor(Block block){
		int blocksAboveSeaLevel = Math.max(block.getPosition().getY() - block.getWorld().getSeaLevel(), 0);
		return getGrassColor(block.getBiome(), blocksAboveSeaLevel);
	}
	
	public Vector3f getGrassColor(Biome biome, int blocksAboveSeaLevel){
		Vector3f mapColor = getColorFromMap(biome, blocksAboveSeaLevel, grassMap);
		Vector3f overlayColor = biome.getOverlayGrassColor().toVector3();
		float overlayAlpha = biome.getOverlayGrassColor().getW();
		return mapColor.mul(1f - overlayAlpha).add(overlayColor.mul(overlayAlpha));
	}
	
	private Vector3f getColorFromMap(Biome biome, int blocksAboveSeaLevel, BufferedImage map){
		Vector2i pixel = getColorMapPosition(biome, blocksAboveSeaLevel).mul(map.getWidth(), map.getHeight()).floor().toInt();
		int cValue = map.getRGB(GenericMath.clamp(pixel.getX(), 0, map.getWidth() - 1), GenericMath.clamp(pixel.getY(), 0, map.getHeight() - 1));
		Color color = new Color(cValue, false);
		return new Vector3f(color.getRed(), color.getGreen(), color.getBlue()).div(0xff);
		
	}
	
	private Vector2f getColorMapPosition(Biome biome, int blocksAboveSeaLevel){
		float adjTemp = (float) GenericMath.clamp(biome.getTemp() - (0.00166667 * blocksAboveSeaLevel), 0d, 1d);
		float adjHumidity = (float) GenericMath.clamp(biome.getHumidity(), 0d, 1d) * adjTemp;
		return new Vector2f(1 - adjTemp, 1 - adjHumidity);
	}

	public BufferedImage getFoliageMap() {
		return foliageMap;
	}

	public void setFoliageMap(BufferedImage foliageMap) {
		this.foliageMap = foliageMap;
	}

	public BufferedImage getGrassMap() {
		return grassMap;
	}

	public void setGrassMap(BufferedImage grassMap) {
		this.grassMap = grassMap;
	}
	
}
