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
package de.bluecolored.bluemap.core.web;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;
import de.bluecolored.bluemap.core.config.MapConfig;
import de.bluecolored.bluemap.core.render.TileRenderer;
import de.bluecolored.bluemap.core.util.FileUtils;
import de.bluecolored.bluemap.core.util.MathUtils;
import de.bluecolored.bluemap.core.world.World;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

public class WebSettings {

	private ConfigurationLoader<? extends ConfigurationNode> configLoader;
	private ConfigurationNode rootNode;
	
	public WebSettings(File settingsFile) throws IOException {
		FileUtils.createFile(settingsFile);
		
		configLoader = GsonConfigurationLoader.builder()
				.setFile(settingsFile)
				.build();
		
		load();
	}
	
	public void load() throws IOException {
		rootNode = configLoader.load();
	}
	
	public void save() throws IOException {
		configLoader.save(rootNode);
	}
	
	public void set(Object value, Object... path) {
		rootNode.getNode(path).setValue(value);
	}

	public Object get(Object... path) {
		return rootNode.getNode(path).getValue();
	}
	
	public String getString(Object... path) {
		return rootNode.getNode(path).getString();
	}
	
	public int getInt(Object... path) {
		return rootNode.getNode(path).getInt();
	}
	
	public long getLong(Object... path) {
		return rootNode.getNode(path).getLong();
	}
	
	public float getFloat(Object... path) {
		return rootNode.getNode(path).getFloat();
	}
	
	public double getDouble(Object... path) {
		return rootNode.getNode(path).getDouble();
	}
	
	public Collection<String> getMapIds() {
		return rootNode.getNode("maps").getChildrenMap().keySet().stream().map(o -> o.toString()).collect(Collectors.toSet());
	}
	
	public void setAllMapsEnabled(boolean enabled) {
		for (ConfigurationNode mapNode : rootNode.getNode("maps").getChildrenMap().values()) {
			mapNode.getNode("enabled").setValue(enabled);
		}
	}
	
	public void setMapEnabled(boolean enabled, String mapId) {
		set(enabled, "maps", mapId, "enabled");
	}
	
	public void setFrom(TileRenderer tileRenderer, String mapId) {
		Vector2i hiresTileSize = tileRenderer.getHiresModelManager().getTileSize();
		Vector2i gridOrigin = tileRenderer.getHiresModelManager().getGridOrigin();
		Vector2i lowresTileSize = tileRenderer.getLowresModelManager().getTileSize();
		Vector2i lowresPointsPerHiresTile = tileRenderer.getLowresModelManager().getPointsPerHiresTile();
		
		set(hiresTileSize.getX(), "maps", mapId, "hires", "tileSize", "x");
		set(hiresTileSize.getY(), "maps", mapId, "hires", "tileSize", "z");
		set(1, "maps", mapId, "hires", "scale", "x");
		set(1, "maps", mapId, "hires", "scale", "z");
		set(gridOrigin.getX(), "maps", mapId, "hires", "translate", "x");
		set(gridOrigin.getY(), "maps", mapId, "hires", "translate", "z");
		
		Vector2i pointSize = hiresTileSize.div(lowresPointsPerHiresTile);
		Vector2i tileSize = pointSize.mul(lowresTileSize);
		
		set(tileSize.getX(), "maps", mapId, "lowres", "tileSize", "x");
		set(tileSize.getY(), "maps", mapId, "lowres", "tileSize", "z");
		set(pointSize.getX(), "maps", mapId, "lowres", "scale", "x");
		set(pointSize.getY(), "maps", mapId, "lowres", "scale", "z");
		set(pointSize.getX() / 2, "maps", mapId, "lowres", "translate", "x");
		set(pointSize.getY() / 2, "maps", mapId, "lowres", "translate", "z");
	}

	public void setFrom(World world, String mapId) {
		set(world.getSpawnPoint().getX(), "maps", mapId, "startPos", "x");
		set(world.getSpawnPoint().getZ(), "maps", mapId, "startPos", "z");
		set(world.getUUID().toString(), "maps", mapId, "world");
	}
	
	public void setFrom(MapConfig mapConfig, String mapId) {
		Vector2i startPos = mapConfig.getStartPos();
		if (startPos != null) {
			set(startPos.getX(), "maps", mapId, "startPos", "x");
			set(startPos.getY(), "maps", mapId, "startPos", "z");
		}

		Vector3f skyColor = MathUtils.color3FromInt(mapConfig.getSkyColor());
		set(skyColor.getX(), "maps", mapId, "skyColor", "r");
		set(skyColor.getY(), "maps", mapId, "skyColor", "g");
		set(skyColor.getZ(), "maps", mapId, "skyColor", "b");
		
		set(mapConfig.getAmbientLight(), "maps", mapId, "ambientLight");
		
		setName(mapConfig.getName(), mapId);
	}
	
	public void setOrdinal(int ordinal, String mapId) {
		set(ordinal, "maps", mapId, "ordinal");
	}
	
	public int getOrdinal(String mapId) {
		return getInt("maps", mapId, "ordinal");
	}
	
	public void setName(String name, String mapId) {
		set(name, "maps", mapId, "name");
	}
	
	public String getName(String mapId) {
		return getString("maps", mapId, "name");
	}
	
}
