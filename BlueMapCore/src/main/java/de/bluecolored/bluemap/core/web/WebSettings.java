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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import com.flowpowered.math.vector.Vector2i;

import de.bluecolored.bluemap.core.render.TileRenderer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

public class WebSettings {

	private ConfigurationLoader<? extends ConfigurationNode> configLoader;
	private ConfigurationNode rootNode;
	
	public WebSettings(File settingsFile) throws IOException {
		
		if (!settingsFile.exists()) {
			settingsFile.getParentFile().mkdirs();
			settingsFile.createNewFile();
		}
		
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
		return rootNode.getChildrenMap().keySet().stream().map(o -> o.toString()).collect(Collectors.toSet());
	}
	
	public void setAllEnabled(boolean enabled) {
		for (ConfigurationNode mapNode : rootNode.getChildrenMap().values()) {
			mapNode.getNode("enabled").setValue(enabled);
		}
	}
	
	public void setEnabled(boolean enabled, String mapId) {
		set(enabled, mapId, "enabled");
	}
	
	public void setFrom(TileRenderer tileRenderer, String mapId) {
		Vector2i hiresTileSize = tileRenderer.getHiresModelManager().getTileSize();
		Vector2i gridOrigin = tileRenderer.getHiresModelManager().getGridOrigin();
		Vector2i lowresTileSize = tileRenderer.getLowresModelManager().getTileSize();
		Vector2i lowresPointsPerHiresTile = tileRenderer.getLowresModelManager().getPointsPerHiresTile();
		
		set(hiresTileSize.getX(), mapId, "hires", "tileSize", "x");
		set(hiresTileSize.getY(), mapId, "hires", "tileSize", "z");
		set(1, mapId, "hires", "scale", "x");
		set(1, mapId, "hires", "scale", "z");
		set(gridOrigin.getX(), mapId, "hires", "translate", "x");
		set(gridOrigin.getY(), mapId, "hires", "translate", "z");
		
		Vector2i pointSize = hiresTileSize.div(lowresPointsPerHiresTile);
		Vector2i tileSize = pointSize.mul(lowresTileSize);
		
		set(tileSize.getX(), mapId, "lowres", "tileSize", "x");
		set(tileSize.getY(), mapId, "lowres", "tileSize", "z");
		set(pointSize.getX(), mapId, "lowres", "scale", "x");
		set(pointSize.getY(), mapId, "lowres", "scale", "z");
		set(pointSize.getX() / 2, mapId, "lowres", "translate", "x");
		set(pointSize.getY() / 2, mapId, "lowres", "translate", "z");
	}
	
	public void setHiresViewDistance(float hiresViewDistance, String mapId) {
		set(hiresViewDistance, mapId, "hires", "viewDistance");
	}
	
	public void setLowresViewDistance(float lowresViewDistance, String mapId) {
		set(lowresViewDistance, mapId, "lowres", "viewDistance");
	}
	
	public void setOrdinal(int ordinal, String mapId) {
		set(ordinal, mapId, "ordinal");
	}
	
	public int getOrdinal(String mapId) {
		return getInt(mapId, "ordinal");
	}
	
	public void setName(String name, String mapId) {
		set(name, mapId, "name");
	}
	
	public String getName(String mapId) {
		return getString(mapId, "name");
	}
	
}
