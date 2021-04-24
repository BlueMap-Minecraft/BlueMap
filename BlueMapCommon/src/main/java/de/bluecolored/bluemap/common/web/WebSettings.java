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
package de.bluecolored.bluemap.common.web;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;
import de.bluecolored.bluemap.core.config.MapConfig;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.util.FileUtils;
import de.bluecolored.bluemap.core.util.MathUtils;
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
		return rootNode.getNode("maps").getChildrenMap().keySet().stream().map(Object::toString).collect(Collectors.toSet());
	}
	
	public void setAllMapsEnabled(boolean enabled) {
		for (ConfigurationNode mapNode : rootNode.getNode("maps").getChildrenMap().values()) {
			mapNode.getNode("enabled").setValue(enabled);
		}
	}
	
	public void setMapEnabled(boolean enabled, String mapId) {
		set(enabled, "maps", mapId, "enabled");
	}
	
	public void setFrom(BmMap map) {
		Vector2i hiresTileSize = map.getHiresModelManager().getTileGrid().getGridSize();
		Vector2i gridOrigin = map.getHiresModelManager().getTileGrid().getOffset();
		Vector2i lowresTileSize = map.getLowresModelManager().getTileSize();
		Vector2i lowresPointsPerHiresTile = map.getLowresModelManager().getPointsPerHiresTile();
		
		set(hiresTileSize.getX(), "maps", map.getId(), "hires", "tileSize", "x");
		set(hiresTileSize.getY(), "maps", map.getId(), "hires", "tileSize", "z");
		set(1, "maps", map.getId(), "hires", "scale", "x");
		set(1, "maps", map.getId(), "hires", "scale", "z");
		set(gridOrigin.getX(), "maps", map.getId(), "hires", "translate", "x");
		set(gridOrigin.getY(), "maps", map.getId(), "hires", "translate", "z");
		
		Vector2i pointSize = hiresTileSize.div(lowresPointsPerHiresTile);
		Vector2i tileSize = pointSize.mul(lowresTileSize);
		
		set(tileSize.getX(), "maps", map.getId(), "lowres", "tileSize", "x");
		set(tileSize.getY(), "maps", map.getId(), "lowres", "tileSize", "z");
		set(pointSize.getX(), "maps", map.getId(), "lowres", "scale", "x");
		set(pointSize.getY(), "maps", map.getId(), "lowres", "scale", "z");
		set(pointSize.getX() / 2, "maps", map.getId(), "lowres", "translate", "x");
		set(pointSize.getY() / 2, "maps", map.getId(), "lowres", "translate", "z");

		set(map.getWorld().getSpawnPoint().getX(), "maps", map.getId(), "startPos", "x");
		set(map.getWorld().getSpawnPoint().getZ(), "maps", map.getId(), "startPos", "z");
		set(map.getWorld().getUUID().toString(), "maps", map.getId(), "world");
	}
	
	public void setFrom(MapConfig mapConfig) {
		Vector2i startPos = mapConfig.getStartPos();
		if (startPos != null) {
			set(startPos.getX(), "maps", mapConfig.getId(), "startPos", "x");
			set(startPos.getY(), "maps", mapConfig.getId(), "startPos", "z");
		}

		Vector3f skyColor = MathUtils.color3FromInt(mapConfig.getSkyColor());
		set(skyColor.getX(), "maps", mapConfig.getId(), "skyColor", "r");
		set(skyColor.getY(), "maps", mapConfig.getId(), "skyColor", "g");
		set(skyColor.getZ(), "maps", mapConfig.getId(), "skyColor", "b");
		
		set(mapConfig.getAmbientLight(), "maps", mapConfig.getId(), "ambientLight");
		
		setName(mapConfig.getName(), mapConfig.getId());
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
