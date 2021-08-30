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
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.io.IOException;

public class MapSettings {

	private final ConfigurationLoader<? extends ConfigurationNode> configLoader;
	private ConfigurationNode rootNode;
	
	public MapSettings(File settingsFile) throws IOException {
		FileUtils.createFile(settingsFile);
		
		configLoader = GsonConfigurationLoader.builder()
				.file(settingsFile)
				.build();
		
		load();
	}
	
	public void load() throws IOException {
		rootNode = configLoader.load();
	}
	
	public void save() throws IOException {
		configLoader.save(rootNode);
	}
	
	public void set(Object value, Object... path) throws SerializationException {
		rootNode.node(path).set(value);
	}

	public Object get(Object... path) {
		return rootNode.node(path).raw();
	}
	
	public String getString(Object... path) {
		return rootNode.node(path).getString();
	}
	
	public int getInt(Object... path) {
		return rootNode.node(path).getInt();
	}

	public long getLong(Object... path) {
		return rootNode.node(path).getLong();
	}

	public float getFloat(Object... path) {
		return rootNode.node(path).getFloat();
	}

	public double getDouble(Object... path) {
		return rootNode.node(path).getDouble();
	}
	
	public String getMapId() {
		return getString("world");
	}
	
	public void setFrom(BmMap map) throws SerializationException {
		Vector2i hiresTileSize = map.getHiresModelManager().getTileGrid().getGridSize();
		Vector2i gridOrigin = map.getHiresModelManager().getTileGrid().getOffset();
		Vector2i lowresTileSize = map.getLowresModelManager().getTileSize();
		Vector2i lowresPointsPerHiresTile = map.getLowresModelManager().getPointsPerHiresTile();
		
		set(hiresTileSize.getX(), "hires", "tileSize", "x");
		set(hiresTileSize.getY(), "hires", "tileSize", "z");
		set(1, "hires", "scale", "x");
		set(1, "hires", "scale", "z");
		set(gridOrigin.getX(), "hires", "translate", "x");
		set(gridOrigin.getY(), "hires", "translate", "z");
		
		Vector2i pointSize = hiresTileSize.div(lowresPointsPerHiresTile);
		Vector2i tileSize = pointSize.mul(lowresTileSize);
		
		set(tileSize.getX(), "lowres", "tileSize", "x");
		set(tileSize.getY(), "lowres", "tileSize", "z");
		set(pointSize.getX(),"lowres", "scale", "x");
		set(pointSize.getY(),"lowres", "scale", "z");
		set(pointSize.getX() / 2, "lowres", "translate", "x");
		set(pointSize.getY() / 2, "lowres", "translate", "z");

		set(map.getWorld().getSpawnPoint().getX(), "startPos", "x");
		set(map.getWorld().getSpawnPoint().getZ(), "startPos", "z");
		set(map.getWorld().getUUID().toString(), "world");
	}
	
	public void setFrom(MapConfig mapConfig) throws SerializationException {
		Vector2i startPos = mapConfig.getStartPos();
		if (startPos != null) {
			set(startPos.getX(), "startPos", "x");
			set(startPos.getY(), "startPos", "z");
		}

		Vector3f skyColor = MathUtils.color3FromInt(mapConfig.getSkyColor());
		set(skyColor.getX(), "skyColor", "r");
		set(skyColor.getY(), "skyColor", "g");
		set(skyColor.getZ(), "skyColor", "b");
		
		set(mapConfig.getAmbientLight(), "ambientLight");
		
		setName(mapConfig.getName(), mapConfig.getId());
	}
	
	public void setOrdinal(int ordinal) throws SerializationException {
		set(ordinal, "ordinal");
	}
	
	public int getOrdinal(String mapId) {
		return getInt("ordinal");
	}
	
	public void setName(String name, String mapId) throws SerializationException {
		set(name, "name");
	}
	
	public String getName(String mapId) {
		return getString("name");
	}
	
}
