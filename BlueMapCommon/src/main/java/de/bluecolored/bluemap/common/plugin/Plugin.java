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
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.MapType;
import de.bluecolored.bluemap.common.MissingResourcesException;
import de.bluecolored.bluemap.common.RenderManager;
import de.bluecolored.bluemap.common.api.BlueMapAPIImpl;
import de.bluecolored.bluemap.common.live.LiveAPIRequestHandler;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.common.plugin.skins.PlayerSkinUpdater;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.config.CoreConfig;
import de.bluecolored.bluemap.core.config.RenderConfig;
import de.bluecolored.bluemap.core.config.WebServerConfig;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.metrics.Metrics;
import de.bluecolored.bluemap.core.resourcepack.ParseResourceException;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.web.FileRequestHandler;
import de.bluecolored.bluemap.core.webserver.HttpRequestHandler;
import de.bluecolored.bluemap.core.webserver.WebServer;
import de.bluecolored.bluemap.core.world.World;

public class Plugin {

	public static final String PLUGIN_ID = "bluemap";
	public static final String PLUGIN_NAME = "BlueMap";

	private MinecraftVersion minecraftVersion;
	private String implementationType;
	private ServerInterface serverInterface;

	private BlueMapService blueMap;
	private BlueMapAPIImpl api;

	private Map<UUID, World> worlds;
	private Map<String, MapType> maps;

	private RenderManager renderManager;
	private WebServer webServer;
	private Thread periodicalSaveThread;
	private Thread metricsThread;

	private PluginConfig pluginConfig;
	private MapUpdateHandler updateHandler;
	private PlayerSkinUpdater skinUpdater;

	private boolean loaded = false;

	public Plugin(MinecraftVersion minecraftVersion, String implementationType, ServerInterface serverInterface) {
		this.minecraftVersion = minecraftVersion;
		this.implementationType = implementationType.toLowerCase();
		this.serverInterface = serverInterface;
	}
	
	public synchronized void load() throws IOException, ParseResourceException {
		if (loaded) return;
		unload(); //ensure nothing is left running (from a failed load or something)

		blueMap = new BlueMapService(minecraftVersion, serverInterface);
	
		//load configs
		CoreConfig coreConfig = blueMap.getCoreConfig();
		RenderConfig renderConfig = blueMap.getRenderConfig();
		WebServerConfig webServerConfig = blueMap.getWebServerConfig();
		
		//load plugin config
		pluginConfig = new PluginConfig(blueMap.getConfigManager().loadOrCreate(
				new File(serverInterface.getConfigFolder(), "plugin.conf"), 
				Plugin.class.getResource("/plugin.conf"), 
				Plugin.class.getResource("/plugin-defaults.conf"), 
				true,
				true
		));

		//try load resources
		try {
			getResourcePack();
		} catch (MissingResourcesException ex) {
			Logger.global.logWarning("BlueMap is missing important resources!");
			Logger.global.logWarning("You need to accept the download of the required files in order of BlueMap to work!");
			try { Logger.global.logWarning("Please check: " + blueMap.getCoreConfigFile().getCanonicalPath()); } catch (IOException ignored) {}
			Logger.global.logInfo("If you have changed the config you can simply reload the plugin using: /bluemap reload");
			
			unload();
			return;
		}
		
		//load worlds and maps
		worlds = blueMap.getWorlds();
		maps = blueMap.getMaps();
		
		//warn if no maps are configured
		if (maps.isEmpty()) {
			Logger.global.logWarning("There are no valid maps configured, please check your render-config! Disabling BlueMap...");
		}
		
		//initialize render manager
		renderManager = new RenderManager(coreConfig.getRenderThreadCount());
		renderManager.start();
		
		//load render-manager state
		try {
			File saveFile = getRenderManagerSaveFile();
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
				Thread.currentThread().interrupt();
				return;
			}
		});
		periodicalSaveThread.start();
		
		//start map updater
		this.updateHandler = new MapUpdateHandler(this);
		serverInterface.registerListener(updateHandler);
		
		//update webapp and settings
		blueMap.createOrUpdateWebApp(false);
		blueMap.updateWebAppSettings();
		
		//start skin updater
		if (pluginConfig.isLiveUpdatesEnabled()) {
			this.skinUpdater = new PlayerSkinUpdater(new File(renderConfig.getWebRoot(), "assets" + File.separator + "playerheads"));
			serverInterface.registerListener(skinUpdater);
		}
		
		//create and start webserver
		if (webServerConfig.isWebserverEnabled()) {
			HttpRequestHandler requestHandler = new FileRequestHandler(webServerConfig.getWebRoot().toPath(), "BlueMap v" + BlueMap.VERSION);
			
			//inject live api if enabled
			if (pluginConfig.isLiveUpdatesEnabled()) {
				requestHandler = new LiveAPIRequestHandler(serverInterface, pluginConfig, requestHandler);
			}
			
			webServer = new WebServer(
				webServerConfig.getWebserverPort(),
				webServerConfig.getWebserverMaxConnections(),
				webServerConfig.getWebserverBindAdress(),
				requestHandler
			);
			webServer.start();
		}
		
		//metrics
		metricsThread = new Thread(() -> {
			try {
				Thread.sleep(TimeUnit.MINUTES.toMillis(1));
				
				while (true) {
					if (serverInterface.isMetricsEnabled(coreConfig.isMetricsEnabled())) Metrics.sendReport(this.implementationType);
					
					Thread.sleep(TimeUnit.MINUTES.toMillis(30));
				}
			} catch (InterruptedException ex){
				Thread.currentThread().interrupt();
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
		if (metricsThread != null) {
			metricsThread.interrupt();
			try {metricsThread.join(1000);} catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
			if (metricsThread.isAlive()) Logger.global.logWarning("The metricsThread did not terminate correctly in time!");
			metricsThread = null;
		}
		
		if (periodicalSaveThread != null) {
			periodicalSaveThread.interrupt();
			try {periodicalSaveThread.join(1000);} catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
			if (periodicalSaveThread.isAlive()) Logger.global.logWarning("The periodicalSaveThread did not terminate correctly in time!");
			periodicalSaveThread = null;
		}
		
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
		if (maps != null) {
			for (MapType map : maps.values()) {
				map.getTileRenderer().save();
			}
		}
		
		//clear resources and configs
		blueMap = null;
		worlds = null;
		maps = null;
		renderManager = null;
		webServer = null;
		updateHandler = null;
		pluginConfig = null;
		
		loaded = false;
	}
	
	public void saveRenderManagerState() throws IOException {
		File saveFile = getRenderManagerSaveFile();
		
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
	
	public CoreConfig getCoreConfig() throws IOException {
		return blueMap.getCoreConfig();
	}
	
	public RenderConfig getRenderConfig() throws IOException {
		return blueMap.getRenderConfig();
	}
	
	public WebServerConfig getWebServerConfig() throws IOException {
		return blueMap.getWebServerConfig();
	}
	
	public PluginConfig getPluginConfig() {
		return pluginConfig;
	}
	
	public ResourcePack getResourcePack() throws IOException {
		return blueMap.getResourcePack();
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
	
	public File getRenderManagerSaveFile() throws IOException {
		if (blueMap == null) return null;
		
		File saveFile = new File(blueMap.getCoreConfig().getDataFolder(), "rmstate");
		saveFile.getParentFile().mkdirs();
		
		return saveFile;
	}
	
	public MapUpdateHandler getUpdateHandler() {
		return updateHandler;
	}
	
	public WebServer getWebServer() {
		return webServer;
	}
	
	public boolean isLoaded() {
		return loaded;
	}
	
}
