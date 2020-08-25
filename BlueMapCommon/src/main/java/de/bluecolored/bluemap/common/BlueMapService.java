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
package de.bluecolored.bluemap.common;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.flowpowered.math.vector.Vector2i;

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.config.ConfigManager;
import de.bluecolored.bluemap.core.config.CoreConfig;
import de.bluecolored.bluemap.core.config.MapConfig;
import de.bluecolored.bluemap.core.config.RenderConfig;
import de.bluecolored.bluemap.core.config.WebServerConfig;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.MCAWorld;
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

/**
 * This is the attempt to generalize as many actions as possible to have CLI and Plugins run on the same general setup-code.
 */
public class BlueMapService {
	private MinecraftVersion minecraftVersion;
	private File configFolder;
	private ThrowingFunction<File, UUID, IOException> worldUUIDProvider;
	private ThrowingFunction<UUID, String, IOException> worldNameProvider;

	private ConfigManager configManager;
	private boolean resourceConfigLoaded = false;
	
	private CoreConfig coreConfig;
	private RenderConfig renderConfig;
	private WebServerConfig webServerConfig;
	
	private ResourcePack resourcePack;

	private Map<UUID, World> worlds;
	private Map<String, MapType> maps;
	
	public BlueMapService(MinecraftVersion minecraftVersion, File configFolder) {
		this.minecraftVersion = minecraftVersion;
		this.configFolder = configFolder;
		
		Map<File, UUID> uuids = new HashMap<>();
		this.worldUUIDProvider = file -> {
			UUID uuid = uuids.get(file);
			if (uuid == null) {
				uuid = UUID.randomUUID();
				uuids.put(file, uuid);
			}
			return uuid;
		};
		
		this.worldNameProvider = uuid -> null;
		
		configManager = new ConfigManager();
	}

	public BlueMapService(MinecraftVersion minecraftVersion, ServerInterface serverInterface) {
		this.minecraftVersion = minecraftVersion;
		this.configFolder = serverInterface.getConfigFolder();
		this.worldUUIDProvider = serverInterface::getUUIDForWorld;
		this.worldNameProvider = serverInterface::getWorldName;
		
		this.configManager = new ConfigManager();
	}
	
	public synchronized void createOrUpdateWebApp(boolean force) throws IOException {
		WebFilesManager webFilesManager = new WebFilesManager(getRenderConfig().getWebRoot());
		if (force || webFilesManager.needsUpdate()) {
			webFilesManager.updateFiles();
		}
	}
	
	public synchronized WebSettings updateWebAppSettings() throws IOException {
		WebSettings webSettings = new WebSettings(new File(getRenderConfig().getWebRoot(), "data" + File.separator + "settings.json"));
		webSettings.set(getRenderConfig().isUseCookies(), "useCookies");
		webSettings.setAllMapsEnabled(false);
		for (MapType map : getMaps().values()) {
			webSettings.setMapEnabled(true, map.getId());
			webSettings.setFrom(map.getTileRenderer(), map.getId());
			webSettings.setFrom(map.getWorld(), map.getId());
		}
		int ordinal = 0;
		for (MapConfig map : getRenderConfig().getMapConfigs()) {
			if (!getMaps().containsKey(map.getId())) continue; //don't add not loaded maps
			webSettings.setOrdinal(ordinal++, map.getId());
			webSettings.setFrom(map, map.getId());
		}
		webSettings.save();
		
		return webSettings;
	}
	
	public synchronized Map<UUID, World> getWorlds() throws IOException {
		if (worlds == null) loadWorldsAndMaps();
		return worlds;
	}
	
	public synchronized Map<String, MapType> getMaps() throws IOException {
		if (maps == null) loadWorldsAndMaps();
		return maps;
	}
	
	private synchronized void loadWorldsAndMaps() throws IOException {
		maps = new HashMap<>();
		worlds = new HashMap<>();
		
		for (MapConfig mapConfig : getRenderConfig().getMapConfigs()) {
			String id = mapConfig.getId();
			String name = mapConfig.getName();
			
			File worldFolder = new File(mapConfig.getWorldPath());
			if (!worldFolder.exists() || !worldFolder.isDirectory()) {
				Logger.global.logWarning("Failed to load map '" + id + "': '" + worldFolder.getCanonicalPath() + "' does not exist or is no directory!");
				continue;
			}
			
			UUID worldUUID;
			try {
				worldUUID = worldUUIDProvider.apply(worldFolder);
			} catch (IOException e) {
				Logger.global.logError("Failed to load map '" + id + "': Failed to get UUID for the world!", e);
				continue;
			}
			
			World world = worlds.get(worldUUID);
			if (world == null) {
				try {
					world = MCAWorld.load(worldFolder.toPath(), worldUUID, minecraftVersion, getConfigManager().getBlockIdConfig(), getConfigManager().getBlockPropertiesConfig(), getConfigManager().getBiomeConfig(), worldNameProvider.apply(worldUUID), true);
					worlds.put(worldUUID, world);
				} catch (MissingResourcesException e) {
					throw e; // rethrow this to stop loading and display resource-missing message
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
					getRenderConfig().getWebRoot().toPath().resolve("data").resolve(id).resolve("hires"),
					getResourcePack(),
					mapConfig,
					new Vector2i(mapConfig.getHiresTileSize(), mapConfig.getHiresTileSize())
					);
			
			LowresModelManager lowresModelManager = new LowresModelManager(
					getRenderConfig().getWebRoot().toPath().resolve("data").resolve(id).resolve("lowres"), 
					new Vector2i(mapConfig.getLowresPointsPerLowresTile(), mapConfig.getLowresPointsPerLowresTile()),
					new Vector2i(mapConfig.getLowresPointsPerHiresTile(), mapConfig.getLowresPointsPerHiresTile()),
					mapConfig.useGzipCompression()
					);
			
			TileRenderer tileRenderer = new TileRenderer(hiresModelManager, lowresModelManager);
			
			MapType mapType = new MapType(id, name, world, tileRenderer);
			maps.put(id, mapType);
		}
		
		worlds = Collections.unmodifiableMap(worlds);
		maps = Collections.unmodifiableMap(maps);
	}
	
	public synchronized ResourcePack getResourcePack() throws IOException, MissingResourcesException {
		if (resourcePack == null) {
			File defaultResourceFile = new File(getCoreConfig().getDataFolder(), "minecraft-client-" + minecraftVersion.getVersionString() + ".jar");
			File resourceExtensionsFile = new File(getCoreConfig().getDataFolder(), minecraftVersion.getResourcePrefix() + File.separator + "resourceExtensions.zip");

			File textureExportFile = new File(getRenderConfig().getWebRoot(), "data" + File.separator + "textures.json");
			
			if (!defaultResourceFile.exists()) {
				if (getCoreConfig().isDownloadAccepted()) {
					
					//download file
					try {
						Logger.global.logInfo("Downloading " + minecraftVersion.getClientDownloadUrl() + " to " + defaultResourceFile + " ...");
						FileUtils.copyURLToFile(new URL(minecraftVersion.getClientDownloadUrl()), defaultResourceFile, 10000, 10000);
					} catch (IOException e) {
						throw new IOException("Failed to download resources!", e);
					}
					
				} else {
					throw new MissingResourcesException();
				}
			}

			Logger.global.logInfo("Loading resources...");
			
			resourceExtensionsFile.delete();
			FileUtils.copyURLToFile(Plugin.class.getResource("/de/bluecolored/bluemap/resourceExtensions.zip"), resourceExtensionsFile, 10000, 10000);
			
			//find more resource packs
			File resourcePackFolder = new File(configFolder, "resourcepacks");
			resourcePackFolder.mkdirs();
			File[] resourcePacks = resourcePackFolder.listFiles();
			Arrays.sort(resourcePacks); //load resource packs in alphabetical order so you can reorder them by renaming
			
			List<File> resources = new ArrayList<>(resourcePacks.length + 1);
			resources.add(defaultResourceFile);
			for (File file : resourcePacks) resources.add(file);
			resources.add(resourceExtensionsFile);
			
			try {
				resourcePack = new ResourcePack(minecraftVersion);
				if (textureExportFile.exists()) resourcePack.loadTextureFile(textureExportFile);
				resourcePack.load(resources);
				resourcePack.saveTextureFile(textureExportFile);
			} catch (ParseResourceException e) {
				throw new IOException("Failed to parse resources!", e);
			}
			
		}
		
		return resourcePack;
	}
	
	public synchronized ConfigManager getConfigManager() throws IOException {
		if (!resourceConfigLoaded) {
			configManager.loadResourceConfigs(configFolder, getResourcePack());
			resourceConfigLoaded = true;
		}
		
		return configManager;
	}
	
	public File getCoreConfigFile() {
		return new File(configFolder, "core.conf");
	}
	
	public synchronized CoreConfig getCoreConfig() throws IOException {
		if (coreConfig == null) {
			coreConfig = new CoreConfig(configManager.loadOrCreate(
					getCoreConfigFile(), 
					Plugin.class.getResource("/de/bluecolored/bluemap/core.conf"), 
					Plugin.class.getResource("/de/bluecolored/bluemap/core-defaults.conf"), 
					true, 
					true
			));
		}
		
		return coreConfig;
	}
	
	public File getRenderConfigFile() {
		return new File(configFolder, "render.conf");
	}
	
	public synchronized RenderConfig getRenderConfig() throws IOException {
		if (renderConfig == null) {
			renderConfig = new RenderConfig(configManager.loadOrCreate(
					getRenderConfigFile(), 
					Plugin.class.getResource("/de/bluecolored/bluemap/render.conf"), 
					Plugin.class.getResource("/de/bluecolored/bluemap/render-defaults.conf"), 
					true, 
					true
			));
		}
		
		return renderConfig;
	}
	
	public File getWebServerConfigFile() {
		return new File(configFolder, "webserver.conf");
	}
	
	public synchronized WebServerConfig getWebServerConfig() throws IOException {
		if (webServerConfig == null) {
			webServerConfig = new WebServerConfig(configManager.loadOrCreate(
					getWebServerConfigFile(), 
					Plugin.class.getResource("/de/bluecolored/bluemap/webserver.conf"), 
					Plugin.class.getResource("/de/bluecolored/bluemap/webserver-defaults.conf"), 
					true, 
					true
			));
		}
		
		return webServerConfig;
	}
	
	public File getConfigFolder() {
		return configFolder;
	}
	
}
