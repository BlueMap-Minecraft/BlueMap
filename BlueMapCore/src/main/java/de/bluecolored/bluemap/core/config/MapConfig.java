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
package de.bluecolored.bluemap.core.config;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.map.MapSettings;
import de.bluecolored.bluemap.core.util.ConfigUtils;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.util.regex.Pattern;

@DebugDump
public class MapConfig implements MapSettings {
	private static final Pattern VALID_ID_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");
	
	private String id;
	private String name;
	private String world;
	
	private Vector2i startPos;
	private int skyColor;
	private float ambientLight;
	private int worldSkyLight;
	
	private boolean renderCaves;
	
	private Vector3i min, max;
	private boolean renderEdges;
	
	private boolean useGzip;
	private boolean ignoreMissingLightData;
	
	private int hiresTileSize;
	
	private int lowresPointsPerHiresTile;
	private int lowresPointsPerLowresTile;
	
	public MapConfig(ConfigurationNode node) throws IOException {
		
		//id
		this.id = node.node("id").getString("");
		if (id.isEmpty()) throw new IOException("Invalid configuration: Node maps[?].id is not defined");
		if (!VALID_ID_PATTERN.matcher(id).matches()) throw new IOException("Invalid configuration: Node maps[?].id '" + id + "' has invalid characters in it");
		
		//name
		this.name = node.node("name").getString(id);
		
		//world
		this.world = node.node("world").getString("");
		if (world.isEmpty()) throw new IOException("Invalid configuration: Node maps[?].world is not defined");
		
		//startPos
		if (!node.node("startPos").virtual()) this.startPos = ConfigUtils.readVector2i(node.node("startPos"));
		
		//skyColor
		if (!node.node("skyColor").virtual()) this.skyColor = ConfigUtils.readColorInt(node.node("skyColor"));
		else this.skyColor = 0x7dabff;
		
		//ambientLight
		this.ambientLight = node.node("ambientLight").getFloat(0f);

		//worldSkyLight
		this.worldSkyLight = node.node("worldSkyLight").getInt(15);
		
		//renderCaves
		this.renderCaves = node.node("renderCaves").getBoolean(false);

		//bounds
		int minX = node.node("minX").getInt(MapSettings.super.getMin().getX());
		int maxX = node.node("maxX").getInt(MapSettings.super.getMax().getX());
		int minZ = node.node("minZ").getInt(MapSettings.super.getMin().getZ());
		int maxZ = node.node("maxZ").getInt(MapSettings.super.getMax().getZ());
		int minY = node.node("minY").getInt(MapSettings.super.getMin().getY());
		int maxY = node.node("maxY").getInt(MapSettings.super.getMax().getY());
		this.min = new Vector3i(minX, minY, minZ);
		this.max = new Vector3i(maxX, maxY, maxZ);
		
		//renderEdges
		this.renderEdges = node.node("renderEdges").getBoolean(true);

		//useCompression
		this.useGzip = node.node("useCompression").getBoolean(true);

		//ignoreMissingLightData
		this.ignoreMissingLightData = node.node("ignoreMissingLightData").getBoolean(false);
		
		//tile-settings
		this.hiresTileSize = node.node("hires", "tileSize").getInt(32);
		this.lowresPointsPerHiresTile = node.node("lowres", "pointsPerHiresTile").getInt(4);
		this.lowresPointsPerLowresTile = node.node("lowres", "pointsPerLowresTile").getInt(50);
		
		//check valid tile configuration values
		double blocksPerPoint = (double) this.hiresTileSize / (double) this.lowresPointsPerHiresTile;
		if (blocksPerPoint != Math.floor(blocksPerPoint)) throw new IOException("Invalid configuration: Invalid map resolution settings of map " + id + ": hires.tileSize / lowres.pointsPerTile has to be an integer result");
		
	}
	
	public String getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public String getWorldPath() {
		return world;
	}
	
	public Vector2i getStartPos() {
		return startPos;
	}
	
	public int getSkyColor() {
		return skyColor;
	}

	@Override
	public float getAmbientLight() {
		return ambientLight;
	}

	@Override
	public int getWorldSkyLight() {
		return worldSkyLight;
	}

	public boolean isRenderCaves() {
		return renderCaves;
	}
	
	public boolean isIgnoreMissingLightData() {
		return ignoreMissingLightData;
	}

	@Override
	public int getHiresTileSize() {
		return hiresTileSize;
	}

	@Override
	public int getLowresPointsPerHiresTile() {
		return lowresPointsPerHiresTile;
	}

	@Override
	public int getLowresPointsPerLowresTile() {
		return lowresPointsPerLowresTile;
	}

	@Override
	public boolean isExcludeFacesWithoutSunlight() {
		return !isRenderCaves();
	}
	
	@Override
	public Vector3i getMin() {
		return min;
	}
	
	@Override
	public Vector3i getMax() {
		return max;
	}
	
	@Override
	public boolean isRenderEdges() {
		return renderEdges;
	}
	
	@Override
	public boolean useGzipCompression() {
		return useGzip;
	}
	
}
