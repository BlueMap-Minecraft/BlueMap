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
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;

import de.bluecolored.bluemap.core.render.context.ExtendedBlockContext;
import de.bluecolored.bluemap.core.world.Block;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;

public class BlockColorProvider {
	
	private BufferedImage foliageMap;
	private BufferedImage grassMap;
	private Map<String, BiomeInfo> biomeInfos;
	private Map<String, String> blockColors;
	
	public BlockColorProvider(ResourcePack resourcePack) throws IOException, NoSuchResourceException {
		
		this.foliageMap = ImageIO.read(resourcePack.getResource(Paths.get("assets", "minecraft", "textures", "colormap", "foliage.png")));
		this.grassMap = ImageIO.read(resourcePack.getResource(Paths.get("assets", "minecraft", "textures", "colormap", "grass.png")));
		
		
		this.biomeInfos = new ConcurrentHashMap<>();
		GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
				.setURL(getClass().getResource("/biomes.json"))
				.build();
		ConfigurationNode biomesConfig = loader.load();
		
		for (Entry<Object, ? extends ConfigurationNode> n : biomesConfig.getChildrenMap().entrySet()){
			String key = n.getKey().toString();
			BiomeInfo value = new BiomeInfo();
			value.humidity = n.getValue().getNode("humidity").getFloat(0.4f);
			value.temp = n.getValue().getNode("temp").getFloat(0.8f);
			value.watercolor = n.getValue().getNode("watercolor").getInt(4159204);
			
			biomeInfos.put(key, value);
		}
		
		this.blockColors = new ConcurrentHashMap<>();
		loader = GsonConfigurationLoader.builder()
				.setURL(getClass().getResource("/blockColors.json"))
				.build();
		ConfigurationNode blockConfig = loader.load();
		
		for (Entry<Object, ? extends ConfigurationNode> n : blockConfig.getChildrenMap().entrySet()){
			String blockId = n.getKey().toString();
			String color = n.getValue().getString();
			blockColors.put(blockId, color);
		}
		
	}
	
	public Vector3f getBlockColor(ExtendedBlockContext context){
		Block block = context.getRelativeBlock(0, 0, 0);
		String blockId = block.getBlock().getId();
		
		// water color
		if (blockId.equals("water")) {
			return getBiomeWaterAverageColor(context);
		}
		
		String colorDef = blockColors.get(blockId);
		if (colorDef == null) colorDef = blockColors.get("default");
		if (colorDef == null) colorDef = "#foliage";
		
		// grass map
		if (colorDef.equals("#grass")){
			return getBiomeGrassAverageColor(context);
		}

		// foliage map
		if (colorDef.equals("#foliage")){
			return getBiomeFoliageAverageColor(context);
		}
		
		int cValue = Integer.parseInt(colorDef, 16);
		return colorFromInt(cValue);
	}
	
	public Vector3f getBiomeFoliageAverageColor(ExtendedBlockContext context){
		Vector3f color = Vector3f.ZERO;
		
		for (int x = -1; x <= 1; x++){
			for (int z = -1; z <= 1; z++){
				color = color.add(getBiomeFoliageColor(context.getRelativeBlock(x, 0, z)));
			}
		}
		
		return color.div(9f);
	}
	
	private Vector3f getBiomeFoliageColor(Block block){
		Vector3f color = Vector3f.ONE;

		if (block.getBiome().contains("mesa")){
			return colorFromInt(0x9e814d);
		}
		
		if (block.getBiome().contains("swamp")) {
			return colorFromInt(0x6A7039);
		}
		
		int blocksAboveSeaLevel = Math.max(block.getPosition().getY() - block.getWorld().getSeaLevel(), 0);
		color = getFoliageColor(block.getBiome(), blocksAboveSeaLevel);

		//improvised to match the original better
		if (block.getBiome().contains("roofed_forest")){
			color = color.mul(2f).add(colorFromInt(0x28340a)).div(3f);
		}
		
		return color;
	}
	
	public Vector3f getBiomeGrassAverageColor(ExtendedBlockContext context){
		Vector3f color = Vector3f.ZERO;
		
		for (int x = -1; x <= 1; x++){
			for (int z = -1; z <= 1; z++){
				color = color.add(getBiomeGrassColor(context.getRelativeBlock(x, 0, z)));
			}
		}
		
		return color.div(9f);
	}
	
	private Vector3f getBiomeGrassColor(Block block){
		Vector3f color = Vector3f.ONE;
		
		if (block.getBiome().contains("mesa")){
			return colorFromInt(0x90814d);
		}
		
		if (block.getBiome().contains("swamp")) {
			return colorFromInt(0x6A7039);
		}
		
		int blocksAboveSeaLevel = Math.max(block.getPosition().getY() - block.getWorld().getSeaLevel(), 0);
		color = getGrassColor(block.getBiome(), blocksAboveSeaLevel);

		if (block.getBiome().contains("roofed_forest")){
			color = color.add(colorFromInt(0x28340a)).div(2f);
		}
		
		return color;
	}
	
	public Vector3f getBiomeWaterAverageColor(ExtendedBlockContext context){
		Vector3f color = Vector3f.ZERO;
		
		for (int x = -1; x <= 1; x++){
			for (int z = -1; z <= 1; z++){
				color = color.add(getBiomeWaterColor(context.getRelativeBlock(x, 0, z)));
			}
		}
		
		return color.div(9f);
	}
	
	private Vector3f getBiomeWaterColor(Block block){
		return colorFromInt(biomeInfos.get(block.getBiome()).watercolor);
	}
	
	private Vector3f colorFromInt(int cValue){
		Color c = new Color(cValue, false);
		return new Vector3f(c.getRed(), c.getGreen(), c.getBlue()).div(0xff);
	}
	
	private Vector3f getFoliageColor(String biomeId, int blocksAboveSeaLevel){
		return getColorFromMap(biomeId, blocksAboveSeaLevel, foliageMap);
	}
	
	private Vector3f getGrassColor(String biomeId, int blocksAboveSeaLevel){
		return getColorFromMap(biomeId, blocksAboveSeaLevel, grassMap);
	}
	
	private Vector3f getColorFromMap(String biomeId, int blocksAboveSeaLevel, BufferedImage map){
		Vector2i pixel = getColorMapPosition(biomeId, blocksAboveSeaLevel).mul(map.getWidth(), map.getHeight()).floor().toInt();
		int cValue = map.getRGB(GenericMath.clamp(pixel.getX(), 0, map.getWidth() - 1), GenericMath.clamp(pixel.getY(), 0, map.getHeight() - 1));
		Color color = new Color(cValue, false);
		return new Vector3f(color.getRed(), color.getGreen(), color.getBlue()).div(0xff);
		
	}
	
	private Vector2f getColorMapPosition(String biomeId, int blocksAboveSeaLevel){
		BiomeInfo bi = biomeInfos.get(biomeId);
		
		if (bi == null){
			throw new NoSuchElementException("No biome found with id: " + biomeId);
		}
		
		float adjTemp = (float) GenericMath.clamp(bi.temp - (0.00166667 * (double) blocksAboveSeaLevel), 0d, 1d);
		float adjHumidity = (float) GenericMath.clamp(bi.humidity, 0d, 1d) * adjTemp;
		return new Vector2f(1 - adjTemp, 1 - adjHumidity);
	}
	
	class BiomeInfo {
		float humidity;
		float temp;
		int watercolor;
	}
}
