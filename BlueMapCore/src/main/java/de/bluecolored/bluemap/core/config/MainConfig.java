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
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.Preconditions;

import de.bluecolored.bluemap.core.render.RenderSettings;
import de.bluecolored.bluemap.core.util.ConfigUtils;
import de.bluecolored.bluemap.core.web.WebServerConfig;
import ninja.leaping.configurate.ConfigurationNode;

public class MainConfig implements WebServerConfig {
	private static final Pattern VALID_ID_PATTERN = Pattern.compile("[a-zA-Z0-9_]+");
	
	private boolean downloadAccepted = false;
	private boolean metricsEnabled = false;
	
	private boolean webserverEnabled = true;
	private int webserverPort = 8100;
	private int webserverMaxConnections = 100;
	private InetAddress webserverBindAdress = null;
	private boolean useCookies;

	private Path dataPath = Paths.get("data");
	
	private Path webRoot = Paths.get("web");
	private Path webDataPath = webRoot.resolve("data");
	
	private int renderThreadCount = 0;
	
	private List<MapConfig> mapConfigs = new ArrayList<>();
	
	public MainConfig(ConfigurationNode node) throws OutdatedConfigException, IOException {
		checkOutdated(node);
		
		//acceppt-download
		downloadAccepted = node.getNode("accept-download").getBoolean(false);

		//renderThreadCount
		int processors = Runtime.getRuntime().availableProcessors();
		renderThreadCount = node.getNode("renderThreadCount").getInt(0);
		if (renderThreadCount <= 0) renderThreadCount = processors + renderThreadCount;
		if (renderThreadCount <= 0) renderThreadCount = 1;
		
		//metrics
		metricsEnabled = node.getNode("metrics").getBoolean(false);
		
		//data
		dataPath = toFolder(node.getNode("data").getString("data"));

		//webroot
		String webRootString = node.getNode("webroot").getString();
		if (webserverEnabled && webRootString == null) throw new IOException("Invalid configuration: Node webroot is not defined");
		webRoot = toFolder(webRootString);
		
		//webdata
		String webDataString = node.getNode("webdata").getString();
		if (webDataString != null) 
			webDataPath = toFolder(webDataString);
		else
			webDataPath = webRoot.resolve("data");
		
		useCookies = node.getNode("useCookies").getBoolean(true);
		
		//webserver
		loadWebConfig(node.getNode("webserver"));
		
		//maps
		loadMapConfigs(node.getNode("maps"));
	}

	private void loadWebConfig(ConfigurationNode node) throws IOException {
		//enabled
		webserverEnabled = node.getNode("enabled").getBoolean(false);

		if (webserverEnabled) {
			//ip
			String webserverBindAdressString = node.getNode("ip").getString("");
			if (webserverBindAdressString.isEmpty() || webserverBindAdressString.equals("0.0.0.0") || webserverBindAdressString.equals("::0")) {
				webserverBindAdress = new InetSocketAddress(0).getAddress(); // 0.0.0.0
			} else if (webserverBindAdressString.equals("#getLocalHost")) {
				webserverBindAdress = InetAddress.getLocalHost();
			} else {
				webserverBindAdress = InetAddress.getByName(webserverBindAdressString);
			}
			
			//port
			webserverPort = node.getNode("port").getInt(8100);
			
			//maxConnectionCount
			webserverMaxConnections = node.getNode("maxConnectionCount").getInt(100);
		}
		
	}
	
	private void loadMapConfigs(ConfigurationNode node) throws IOException {
		mapConfigs = new ArrayList<>();
		for (ConfigurationNode mapConfigNode : node.getChildrenList()) {
			mapConfigs.add(new MapConfig(mapConfigNode));
		}
	}
	
	private Path toFolder(String pathString) throws IOException {
		Preconditions.checkNotNull(pathString);
		
		File file = new File(pathString);
		if (file.exists() && !file.isDirectory()) throw new IOException("Invalid configuration: Path '" + file.getAbsolutePath() + "' is a file (should be a directory)");
		if (!file.exists() && !file.mkdirs()) throw new IOException("Invalid configuration: Folders to path '" + file.getAbsolutePath() + "' could not be created");
		return file.toPath();
	}

	@Override
	public Path getWebRoot() {
		return webRoot;
	}

	public Path getDataPath() {
		return dataPath;
	}
	
	public boolean isUseCookies() {
		return useCookies;
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
	
	public boolean isDownloadAccepted() {
		return downloadAccepted;
	}
	
	public boolean isMetricsEnabled() {
		return metricsEnabled;
	}
	
	public int getRenderThreadCount() {
		return renderThreadCount;
	}
	
	public List<MapConfig> getMapConfigs(){
		return mapConfigs;
	}
	
	public class MapConfig implements RenderSettings {
		
		private String id;
		private String name;
		private String world;
		
		private Vector2i startPos;
		private int skyColor;
		private float ambientLight;
		
		private boolean renderCaves;
		
		private Vector3i min, max;
		private boolean renderEdges;
		
		private boolean useGzip;
		private boolean ignoreMissingLightData;
		
		private int hiresTileSize;
		
		private int lowresPointsPerHiresTile;
		private int lowresPointsPerLowresTile;
		
		private MapConfig(ConfigurationNode node) throws IOException {
			this.id = node.getNode("id").getString("");
			if (id.isEmpty()) throw new IOException("Invalid configuration: Node maps[?].id is not defined");
			if (!VALID_ID_PATTERN.matcher(id).matches()) throw new IOException("Invalid configuration: Node maps[?].id '" + id + "' has invalid characters in it");
			
			this.name = node.getNode("name").getString(id);
			
			this.world = node.getNode("world").getString("");
			if (world.isEmpty()) throw new IOException("Invalid configuration: Node maps[?].world is not defined");
			
			if (!node.getNode("startPos").isVirtual()) this.startPos = ConfigUtils.readVector2i(node.getNode("startPos"));
			
			if (!node.getNode("skyColor").isVirtual()) this.skyColor = ConfigUtils.readColorInt(node.getNode("skyColor"));
			else this.skyColor = 0x7dabff;
			
			this.ambientLight = node.getNode("ambientLight").getFloat(0f);
			
			this.renderCaves = node.getNode("renderCaves").getBoolean(false);

			int minX = node.getNode("minX").getInt(RenderSettings.super.getMin().getX());
			int maxX = node.getNode("maxX").getInt(RenderSettings.super.getMax().getX());
			int minZ = node.getNode("minZ").getInt(RenderSettings.super.getMin().getZ());
			int maxZ = node.getNode("maxZ").getInt(RenderSettings.super.getMax().getZ());
			int minY = node.getNode("minY").getInt(RenderSettings.super.getMin().getY());
			int maxY = node.getNode("maxY").getInt(RenderSettings.super.getMax().getY());
			this.min = new Vector3i(minX, minY, minZ);
			this.max = new Vector3i(maxX, maxY, maxZ);
			
			this.renderEdges = node.getNode("renderEdges").getBoolean(true);

			this.useGzip = node.getNode("useCompression").getBoolean(true);
			this.ignoreMissingLightData = node.getNode("ignoreMissingLightData").getBoolean(false);
			
			this.hiresTileSize = node.getNode("hires", "tileSize").getInt(32);
			
			this.lowresPointsPerHiresTile = node.getNode("lowres", "pointsPerHiresTile").getInt(4);
			this.lowresPointsPerLowresTile = node.getNode("lowres", "pointsPerLowresTile").getInt(50);
			
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
		
		public String getWorldPath() {
			return world;
		}
		
		public Vector2i getStartPos() {
			return startPos;
		}
		
		public int getSkyColor() {
			return skyColor;
		}
		
		public float getAmbientLight() {
			return ambientLight;
		}

		public boolean isRenderCaves() {
			return renderCaves;
		}
		
		public int getHiresTileSize() {
			return hiresTileSize;
		}

		public int getLowresPointsPerHiresTile() {
			return lowresPointsPerHiresTile;
		}

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
		
		public boolean isIgnoreMissingLightData() {
			return ignoreMissingLightData;
		}
		
	}

	private void checkOutdated(ConfigurationNode node) throws OutdatedConfigException {
		// check for config-nodes from version 0.1.x
		assertNotPresent(node.getNode("version"));
		assertNotPresent(node.getNode("web"));
		for (ConfigurationNode map : node.getNode("maps").getChildrenList()) {
			assertNotPresent(map.getNode("sliceY"));
		}
	}
	
	private void assertNotPresent(ConfigurationNode node) throws OutdatedConfigException {
		if (!node.isVirtual()) throw new OutdatedConfigException("Configurtion-node '" + ConfigUtils.nodePathToString(node) + "' is no longer used, please update your configuration!");
	}
	
}
