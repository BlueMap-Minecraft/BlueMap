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
package de.bluecolored.bluemap.common.plugin;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;

import com.flowpowered.math.vector.Vector2i;

import de.bluecolored.bluemap.common.BlueMapWebServer;
import de.bluecolored.bluemap.common.MapType;
import de.bluecolored.bluemap.common.RenderManager;
import de.bluecolored.bluemap.common.api.BlueMapAPIImpl;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.config.ConfigManager;
import de.bluecolored.bluemap.core.config.MainConfig;
import de.bluecolored.bluemap.core.config.MainConfig.MapConfig;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.metrics.Metrics;
import de.bluecolored.bluemap.core.render.RenderSettings;
import de.bluecolored.bluemap.core.render.TileRenderer;
import de.bluecolored.bluemap.core.render.hires.HiresModelManager;
import de.bluecolored.bluemap.core.render.lowres.LowresModelManager;
import de.bluecolored.bluemap.core.resourcepack.ParseResourceException;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.web.WebFilesManager;
import de.bluecolored.bluemap.core.web.WebSettings;
import de.bluecolored.bluemap.core.world.SlicedWorld;
import de.bluecolored.bluemap.core.world.World;

public class Plugin {

	public static final String PLUGIN_ID = "bluemap";
	public static final String PLUGIN_NAME = "BlueMap";

	private static Plugin instance;
	
	private BlueMapAPIImpl api;
	
	private String implementationType;
	
	private ServerInterface serverInterface;
	
	private MainConfig config;
	private ResourcePack resourcePack;

	private Map<UUID, World> worlds;
	private Map<String, MapType> maps;
	
	private MapUpdateHandler updateHandler;

	private RenderManager renderManager;
	private BlueMapWebServer webServer;
	
	private Thread periodicalSaveThread;
	private Thread metricsThread;
	
	private boolean loaded = false;

	public Plugin(String implementationType, ServerInterface serverInterface) {
		this.implementationType = implementationType.toLowerCase();
		
		this.serverInterface = serverInterface;
		
		this.maps = new HashMap<>();
		this.worlds = new HashMap<>();
		
		instance = this;
	}
	
	public synchronized void load() throws IOException, ParseResourceException {
		if (loaded) return;
		unload(); //ensure nothing is left running (from a failed load or something)
		
		//load configs
		URL defaultSpongeConfig = Plugin.class.getResource("/bluemap-" + implementationType + ".conf");
		URL spongeConfigDefaults = Plugin.class.getResource("/bluemap-" + implementationType + "-defaults.conf");
		ConfigManager configManager = new ConfigManager(serverInterface.getConfigFolder(), defaultSpongeConfig, spongeConfigDefaults);
		configManager.loadMainConfig();
		config = configManager.getMainConfig();
		
		//load resources
		File defaultResourceFile = config.getDataPath().resolve("minecraft-client-" + ResourcePack.MINECRAFT_CLIENT_VERSION + ".jar").toFile();
		File resourceExtensionsFile = config.getDataPath().resolve("resourceExtensions.zip").toFile();
		File textureExportFile = config.getWebDataPath().resolve("textures.json").toFile();
		
		if (!defaultResourceFile.exists()) {
			if (config.isDownloadAccepted()) {
				
				//download file
				try {
					Logger.global.logInfo("Downloading " + ResourcePack.MINECRAFT_CLIENT_URL + " to " + defaultResourceFile + " ...");
					ResourcePack.downloadDefaultResource(defaultResourceFile);
				} catch (IOException e) {
					Logger.global.logError("Failed to download resources!", e);
					return;
				}
				
			} else {
				Logger.global.logWarning("BlueMap is missing important resources!");
				Logger.global.logWarning("You need to accept the download of the required files in order of BlueMap to work!");
				try { Logger.global.logWarning("Please check: " + configManager.getMainConfigFile().getCanonicalPath()); } catch (IOException ignored) {}
				Logger.global.logInfo("If you have changed the config you can simply reload the plugin using: /bluemap reload");
				
				return;
			}
		}

		resourceExtensionsFile.delete();
		FileUtils.copyURLToFile(Plugin.class.getResource("/resourceExtensions.zip"), resourceExtensionsFile, 10000, 10000);
		
		//find more resource packs
		File resourcePackFolder = new File(serverInterface.getConfigFolder(), "resourcepacks");
		resourcePackFolder.mkdirs();
		File[] resourcePacks = resourcePackFolder.listFiles();
		Arrays.sort(resourcePacks); //load resource packs in alphabetical order so you can reorder them by renaming
		
		List<File> resources = new ArrayList<>(resourcePacks.length + 1);
		resources.add(defaultResourceFile);
		for (File file : resourcePacks) resources.add(file);
		resources.add(resourceExtensionsFile);
		
		resourcePack = new ResourcePack();
		if (textureExportFile.exists()) resourcePack.loadTextureFile(textureExportFile);
		resourcePack.load(resources);
		resourcePack.saveTextureFile(textureExportFile);
		
		configManager.loadResourceConfigs(resourcePack);
		
		//load maps
		for (MapConfig mapConfig : config.getMapConfigs()) {
			String id = mapConfig.getId();
			String name = mapConfig.getName();
			
			File worldFolder = new File(mapConfig.getWorldPath());
			if (!worldFolder.exists() || !worldFolder.isDirectory()) {
				Logger.global.logError("Failed to load map '" + id + "': '" + worldFolder.getCanonicalPath() + "' does not exist or is no directory!", new IOException());
				continue;
			}
			
			UUID worldUUID;
			try {
				worldUUID = serverInterface.getUUIDForWorld(worldFolder);
			} catch (IOException e) {
				Logger.global.logError("Failed to load map '" + id + "': Failed to get UUID for the world!", e);
				continue;
			}
			
			World world = worlds.get(worldUUID);
			if (world == null) {
				try {
					world = MCAWorld.load(worldFolder.toPath(), worldUUID, configManager.getBlockIdConfig(), configManager.getBlockPropertiesConfig(), configManager.getBiomeConfig(), serverInterface.getWorldName(worldUUID), true);
					worlds.put(worldUUID, world);
				} catch (IOException e) {
					Logger.global.logError("Failed to load map '" + id + "': Failed to read level.dat", e);
					continue;
				}
			}
			
			//slice world if configured
			if (!mapConfig.getMin().equals(RenderSettings.DEFAULT_MIN) || !mapConfig.getMax().equals(RenderSettings.DEFAULT_MAX)) {
				if (mapConfig.isRenderEdges()) { 
					world = new SlicedWorld(world, mapConfig.getMin(), mapConfig.getMax());
				} else {
					world = new SlicedWorld(
							world, 
							mapConfig.getMin().min(mapConfig.getMin().sub(2, 2, 2)), // protect from int-overflow
							mapConfig.getMax().max(mapConfig.getMax().add(2, 2, 2))  // protect from int-overflow
							);
				}
			}
			
			HiresModelManager hiresModelManager = new HiresModelManager(
					config.getWebDataPath().resolve(id).resolve("hires"),
					resourcePack,
					mapConfig,
					new Vector2i(mapConfig.getHiresTileSize(), mapConfig.getHiresTileSize())
					);
			
			LowresModelManager lowresModelManager = new LowresModelManager(
					config.getWebDataPath().resolve(id).resolve("lowres"), 
					new Vector2i(mapConfig.getLowresPointsPerLowresTile(), mapConfig.getLowresPointsPerLowresTile()),
					new Vector2i(mapConfig.getLowresPointsPerHiresTile(), mapConfig.getLowresPointsPerHiresTile()),
					mapConfig.useGzipCompression()
					);
			
			TileRenderer tileRenderer = new TileRenderer(hiresModelManager, lowresModelManager);
			
			MapType mapType = new MapType(id, name, world, tileRenderer);
			maps.put(id, mapType);
		}
		if (maps.isEmpty()) {
			Logger.global.logWarning("There are no valid maps configured, please check your config! Disabling BlueMap...");
			unload();
			return;
		}
		
		//initialize render manager
		renderManager = new RenderManager(config.getRenderThreadCount());
		renderManager.start();
		
		//load render-manager state
		try {
			File saveFile = config.getDataPath().resolve("rmstate").toFile();
			saveFile.getParentFile().mkdirs();
			if (saveFile.exists()) {
				try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(saveFile)))) {
					renderManager.readState(in, getMapTypes());
				}
			}
		} catch (IOException ex) {
			Logger.global.logError("Failed to load render-manager state!", ex);
		}
		
		//create periodical-save thread
		periodicalSaveThread = new Thread(() -> {
			try {
				while (true) {
					Thread.sleep(TimeUnit.MINUTES.toMillis(5));
					try {
						saveRenderManagerState();
					} catch (IOException ex) {
						Logger.global.logError("Failed to save render-manager state!", ex);
					}
				}
			} catch (InterruptedException ex){
				return;
			}
		});
		periodicalSaveThread.start();
		
		//start map updater
		this.updateHandler = new MapUpdateHandler();
		serverInterface.registerListener(updateHandler);
		
		//create/update webfiles
		WebFilesManager webFilesManager = new WebFilesManager(config.getWebRoot());
		if (webFilesManager.needsUpdate()) {
			webFilesManager.updateFiles();
		}

		WebSettings webSettings = new WebSettings(config.getWebDataPath().resolve("settings.json").toFile());
		webSettings.set(config.isUseCookies(), "useCookies");
		webSettings.setAllMapsEnabled(false);
		for (MapType map : maps.values()) {
			webSettings.setMapEnabled(true, map.getId());
			webSettings.setFrom(map.getTileRenderer(), map.getId());
			webSettings.setFrom(map.getWorld(), map.getId());
		}
		int ordinal = 0;
		for (MapConfig map : config.getMapConfigs()) {
			if (!maps.containsKey(map.getId())) continue; //don't add not loaded maps
			webSettings.setOrdinal(ordinal++, map.getId());
			webSettings.setFrom(map, map.getId());
		}
		webSettings.save();
		
		//start webserver
		if (config.isWebserverEnabled()) {
			webServer = new BlueMapWebServer(config);
			webServer.updateWebfiles();
			webServer.start();
		}
		
		//metrics
		metricsThread = new Thread(() -> {
			try {
				Thread.sleep(TimeUnit.MINUTES.toMillis(1));
				
				while (true) {
					if (serverInterface.isMetricsEnabled(config.isMetricsEnabled())) Metrics.sendReport(this.implementationType);
					Thread.sleep(TimeUnit.MINUTES.toMillis(30));
				}
			} catch (InterruptedException ex){
				return;
			}
		});
		metricsThread.start();

		loaded = true;
		
		//enable api
		this.api = new BlueMapAPIImpl(this);
		this.api.register();
	}
	
	public synchronized void unload() {
		
		//disable api
		if (api != null) api.unregister();
		api = null;
		
		//unregister listeners
		serverInterface.unregisterAllListeners();

		//stop scheduled threads
		if (metricsThread != null) metricsThread.interrupt();
		metricsThread = null;
		
		if (periodicalSaveThread != null) periodicalSaveThread.interrupt();
		periodicalSaveThread = null;
		
		//stop services
		if (renderManager != null) renderManager.stop();
		if (webServer != null) webServer.close();
		
		//save render-manager state
		if (updateHandler != null) updateHandler.flushTileBuffer(); //first write all buffered tiles to the render manager to save them too
		if (renderManager != null) {
			try {
				saveRenderManagerState();
			} catch (IOException ex) {
				Logger.global.logError("Failed to save render-manager state!", ex);
			}
		}
		
		//save renders
		for (MapType map : maps.values()) {
			map.getTileRenderer().save();
		}
		
		//clear resources and configs
		renderManager = null;
		webServer = null;
		updateHandler = null;
		resourcePack = null;
		config = null;
		maps.clear();
		worlds.clear();
		
		loaded = false;
	}
	
	public void saveRenderManagerState() throws IOException {
		File saveFile = config.getDataPath().resolve("rmstate").toFile();
		saveFile.getParentFile().mkdirs();
		if (saveFile.exists()) saveFile.delete();
		saveFile.createNewFile();
		
		try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(saveFile)))) {
			renderManager.writeState(out);
		}
	}

	public synchronized void reload() throws IOException, ParseResourceException {
		unload();
		load();
	}
	
	public ServerInterface getServerInterface() {
		return serverInterface;
	}
	
	public MainConfig getMainConfig() {
		return config;
	}
	
	public ResourcePack getResourcePack() {
		return resourcePack;
	}
	
	public World getWorld(UUID uuid){
		return worlds.get(uuid);
	}
	
	public Collection<World> getWorlds(){
		return worlds.values();
	}
	
	public Collection<MapType> getMapTypes(){
		return maps.values();
	}
	
	public RenderManager getRenderManager() {
		return renderManager;
	}
	
	public MapUpdateHandler getUpdateHandler() {
		return updateHandler;
	}
	
	public BlueMapWebServer getWebServer() {
		return webServer;
	}
	
	public boolean isLoaded() {
		return loaded;
	}
	
	public static Plugin getInstance() {
		return instance;
	}
	
}
