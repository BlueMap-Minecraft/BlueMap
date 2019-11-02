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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FileUtils;

import com.google.common.base.Preconditions;

import de.bluecolored.bluemap.core.render.RenderSettings;
import de.bluecolored.bluemap.core.web.WebServerConfig;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

public class ConfigurationFile implements WebServerConfig {
	
	private String configVersion;
	
	private boolean webserverEnabled;
	private int webserverPort;
	private int webserverMaxConnections;
	private InetAddress webserverBindAdress;

	private Path webRoot;
	private Path webDataPath;
	
	private int renderThreadCount;
	
	private Collection<MapConfig> mapConfigs;
	
	private ConfigurationFile(File configFile) throws IOException {
		ConfigurationLoader<CommentedConfigurationNode> configLoader = HoconConfigurationLoader.builder()
				.setFile(configFile)
				.build();
		
		CommentedConfigurationNode rootNode = configLoader.load();
		
		configVersion = rootNode.getNode("version").getString("-");
		
		loadWebConfig(rootNode.getNode("web"));

		int defaultCount = (int) Math.max(Math.min((double) Runtime.getRuntime().availableProcessors() * 0.75, 16), 1);
		renderThreadCount = rootNode.getNode("renderThreadCount").getInt(defaultCount);
		if (renderThreadCount <= 0) renderThreadCount = defaultCount;
		
		loadMapConfigs(rootNode.getNode("maps"));
	}
	
	private void loadWebConfig(ConfigurationNode node) throws IOException {
		webserverEnabled = node.getNode("enabled").getBoolean(false);
		
		String webRootString = node.getNode("webroot").getString();
		if (webserverEnabled && webRootString == null) throw new IOException("Invalid configuration: Node web.webroot is not defined");
		webRoot = toFolder(webRootString);

		if (webserverEnabled) {
			webserverPort = node.getNode("port").getInt(8100);
			webserverMaxConnections = node.getNode("maxConnectionCount").getInt(100);
			
			String webserverBindAdressString = node.getNode("ip").getString("");
			if (webserverBindAdressString.isEmpty()) {
				webserverBindAdress = InetAddress.getLocalHost();
			} else {
				webserverBindAdress = InetAddress.getByName(webserverBindAdressString);
			}
		}
		
		String webDataString = node.getNode("data").getString();
		if (webDataString != null) 
			webDataPath = toFolder(webDataString);
		else if (webRoot != null)
			webDataPath = webRoot.resolve("data");
		else
			throw new IOException("Invalid configuration: Node web.data is not defined in config");
	}
	
	private void loadMapConfigs(ConfigurationNode node) throws IOException {
		mapConfigs = new ArrayList<>();
		for (ConfigurationNode mapConfigNode : node.getChildrenList()) {
			mapConfigs.add(new MapConfig(mapConfigNode));
		}
	}

	public boolean isWebserverEnabled() {
		return webserverEnabled;
	}
	
	public Path getWebDataPath() {
		return webDataPath;
	}

	@Override
	public int getWebserverPort() {
		return webserverPort;
	}

	@Override
	public int getWebserverMaxConnections() {
		return webserverMaxConnections;
	}

	@Override
	public InetAddress getWebserverBindAdress() {
		return webserverBindAdress;
	}

	@Override
	public Path getWebRoot() {
		return webRoot;
	}
	
	public String getConfigVersion() {
		return configVersion;
	}
	
	public int getRenderThreadCount() {
		return renderThreadCount;
	}
	
	public Collection<MapConfig> getMapConfigs(){
		return mapConfigs;
	}

	private Path toFolder(String pathString) throws IOException {
		Preconditions.checkNotNull(pathString);
		
		File file = new File(pathString);
		if (file.exists() && !file.isDirectory()) throw new IOException("Invalid configuration: Path '" + file.getAbsolutePath() + "' is a file (should be a directory)");
		if (!file.exists() && !file.mkdirs()) throw new IOException("Invalid configuration: Folders to path '" + file.getAbsolutePath() + "' could not be created");
		return file.toPath();
	}
	
	public static ConfigurationFile loadOrCreate(File configFile) throws IOException {
		if (!configFile.exists()) {
			configFile.getParentFile().mkdirs();
			
			FileUtils.copyURLToFile(ConfigurationFile.class.getResource("/bluemap.conf"), configFile, 10000, 10000);
		}
		
		return new ConfigurationFile(configFile);
	}
	
	public class MapConfig implements RenderSettings {
		
		private String id;
		private String name;
		private String world;
		
		private boolean renderCaves;
		private float ambientOcclusion;
		private float lighting;
		
		private int maxY, minY, sliceY;
		
		private int hiresTileSize;
		private float hiresViewDistance;
		
		private int lowresPointsPerHiresTile;
		private int lowresPointsPerLowresTile;
		private float lowresViewDistance;
		
		private MapConfig(ConfigurationNode node) throws IOException {
			this.id = node.getNode("id").getString("");
			if (id.isEmpty()) throw new IOException("Invalid configuration: Node maps[?].id is not defined");
			
			this.name = node.getNode("name").getString(id);
			
			this.world = node.getNode("world").getString("");
			if (world.isEmpty()) throw new IOException("Invalid configuration: Node maps[?].world is not defined");
			
			this.renderCaves = node.getNode("renderCaves").getBoolean(false);
			this.ambientOcclusion = node.getNode("ambientOcclusion").getFloat(0.25f);
			this.lighting = node.getNode("lighting").getFloat(0.8f);
			
			this.maxY = node.getNode("maxY").getInt(RenderSettings.super.getMaxY());
			this.minY = node.getNode("minY").getInt(RenderSettings.super.getMinY());
			this.sliceY = node.getNode("sliceY").getInt(RenderSettings.super.getSliceY());
			
			this.hiresTileSize = node.getNode("hires", "tileSize").getInt(32);
			this.hiresViewDistance = node.getNode("hires", "viewDistance").getFloat(3.5f);
			
			this.lowresPointsPerHiresTile = node.getNode("lowres", "pointsPerHiresTile").getInt(4);
			this.lowresPointsPerLowresTile = node.getNode("lowres", "pointsPerLowresTile").getInt(50);
			this.lowresViewDistance = node.getNode("lowres", "viewDistance").getFloat(4f);
			
			//check valid configuration values
			double blocksPerPoint = (double) this.hiresTileSize / (double) this.lowresPointsPerHiresTile;
			if (blocksPerPoint != Math.floor(blocksPerPoint)) throw new IOException("Invalid configuration: Invalid map resolution settings of map " + id + ": hires.tileSize / lowres.pointsPerTile has to be an integer result");
		}
		
		public String getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}
		
		public String getWorldId() {
			return world;
		}

		public boolean isRenderCaves() {
			return renderCaves;
		}

		@Override
		public float getAmbientOcclusionStrenght() {
			return ambientOcclusion;
		}

		@Override
		public float getLightShadeMultiplier() {
			return lighting;
		}
		
		public int getHiresTileSize() {
			return hiresTileSize;
		}

		public float getHiresViewDistance() {
			return hiresViewDistance;
		}

		public int getLowresPointsPerHiresTile() {
			return lowresPointsPerHiresTile;
		}

		public int getLowresPointsPerLowresTile() {
			return lowresPointsPerLowresTile;
		}

		public float getLowresViewDistance() {
			return lowresViewDistance;
		}

		@Override
		public boolean isExcludeFacesWithoutSunlight() {
			return !isRenderCaves();
		}
		
		@Override
		public int getMaxY() {
			return maxY;
		}
		
		@Override
		public int getMinY() {
			return minY;
		}
		
		@Override
		public int getSliceY() {
			return sliceY;
		}
		
	}
	
}
