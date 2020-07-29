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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.util.ConfigUtils;
import de.bluecolored.bluemap.core.util.MathUtils;
import de.bluecolored.bluemap.core.world.Biome;
import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.World;
import ninja.leaping.configurate.ConfigurationNode;

public class BlockColorCalculator {

	private BufferedImage foliageMap;
	private BufferedImage grassMap;
	
	private Map<String, Function<Block, Vector3f>> blockColorMap;
	
	public BlockColorCalculator(BufferedImage foliageMap, BufferedImage grassMap) {
		this.foliageMap = foliageMap;
		this.grassMap = grassMap;
		
		this.blockColorMap = new HashMap<>();
	}
	
	public void loadColorConfig(ConfigurationNode colorConfig) throws IOException {
		blockColorMap.clear();
		
		for (Entry<Object, ? extends ConfigurationNode> entry : colorConfig.getChildrenMap().entrySet()){
			String key = entry.getKey().toString();
			String value = entry.getValue().getString();
			
			Function<Block, Vector3f> colorFunction;
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
				final Vector3f color = MathUtils.color3FromInt(ConfigUtils.readColorInt(entry.getValue()));
				colorFunction = context -> color;
				break;
			}
			
			blockColorMap.put(key, colorFunction);
		}
	}
	
	public Vector3f getBlockColor(Block block){
		String blockId = block.getBlockState().getFullId();
		
		Function<Block, Vector3f> colorFunction = blockColorMap.get(blockId);
		if (colorFunction == null) colorFunction = blockColorMap.get("default");
		if (colorFunction == null) colorFunction = this::getFoliageAverageColor;
		
		return colorFunction.apply(block);
	}
	
	public Vector3f getWaterAverageColor(Block block){
		Vector3f color = Vector3f.ZERO;

		int count = 0;
		for (Biome biome : iterateAverageBiomes(block)) {
			color = color.add(biome.getWaterColor());
			count++;
		}
		
		return color.div(count);
	}

	public Vector3f getFoliageAverageColor(Block block){
		Vector3f color = Vector3f.ZERO;
		
		int blocksAboveSeaLevel = Math.max(block.getPosition().getY() - block.getWorld().getSeaLevel(), 0);

		int count = 0;
		for (Biome biome : iterateAverageBiomes(block)) {
			color = color.add(getFoliageColor(biome, blocksAboveSeaLevel));
			count++;
		}
		
		return color.div(count);
	}
	
	public Vector3f getFoliageColor(Biome biome, int blocksAboveSeaLevel){
		Vector3f mapColor = getColorFromMap(biome, blocksAboveSeaLevel, foliageMap);
		Vector3f overlayColor = biome.getOverlayFoliageColor().toVector3();
		float overlayAlpha = biome.getOverlayFoliageColor().getW();
		return mapColor.mul(1f - overlayAlpha).add(overlayColor.mul(overlayAlpha));
	}

	public Vector3f getGrassAverageColor(Block block){
		Vector3f color = Vector3f.ZERO;
		
		int blocksAboveSeaLevel = Math.max(block.getPosition().getY() - block.getWorld().getSeaLevel(), 0);
		
		int count = 0;
		for (Biome biome : iterateAverageBiomes(block)) {
			color = color.add(getGrassColor(biome, blocksAboveSeaLevel));
			count++;
		}
		
		return color.div(count);
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
	
	private Iterable<Biome> iterateAverageBiomes(Block block){
		Vector3i pos = block.getPosition();
		Vector3i radius = new Vector3i(2, 1, 2);
		
		final World world = block.getWorld(); 
		final int sx = pos.getX() - radius.getX(), 
				  sy = Math.max(0,  pos.getY() - radius.getY()), 
				  sz = pos.getZ() - radius.getZ();
		final int mx = pos.getX() + radius.getX(), 
				  my = Math.min(255, pos.getY() + radius.getY()), 
				  mz = pos.getZ() + radius.getZ();
		
		return () -> new Iterator<Biome>() {
			private int x = sx, 
						y = sy, 
						z = sz;

			@Override
			public boolean hasNext() {
				return z < mz || y < my || x < mx;
			}

			@Override
			public Biome next() {
				x++;
				if (x > mx) {
					x = sx;
					y++;
				}
				if (y > my) {
					y = sy;
					z++;
				}
				
				return world.getBiome(new Vector3i(x, y, z));
			}
		};
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
