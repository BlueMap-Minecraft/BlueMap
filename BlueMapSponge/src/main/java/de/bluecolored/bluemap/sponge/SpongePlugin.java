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
package de.bluecolored.bluemap.sponge;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.inject.Inject;

import org.bstats.sponge.MetricsLite2;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.SpongeExecutorService;

import com.flowpowered.math.vector.Vector2i;

import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.config.Configuration;
import de.bluecolored.bluemap.core.config.Configuration.MapConfig;
import de.bluecolored.bluemap.core.config.ConfigurationFile;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.metrics.Metrics;
import de.bluecolored.bluemap.core.render.TileRenderer;
import de.bluecolored.bluemap.core.render.hires.HiresModelManager;
import de.bluecolored.bluemap.core.render.lowres.LowresModelManager;
import de.bluecolored.bluemap.core.resourcepack.NoSuchResourceException;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.web.BlueMapWebServer;
import de.bluecolored.bluemap.core.web.WebFilesManager;
import de.bluecolored.bluemap.core.web.WebSettings;
import de.bluecolored.bluemap.core.world.World;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.NBTUtil;

@Plugin(
		id = SpongePlugin.PLUGIN_ID, 
		name = SpongePlugin.PLUGIN_NAME,
		authors = { "Blue (Lukas Rieger)" },
		description = "This plugin provides a fully 3D map of your world for your browser!",
		version = SpongePlugin.PLUGIN_VERSION
		)
public class SpongePlugin {

	public static final String PLUGIN_ID = "bluemap";
	public static final String PLUGIN_NAME = "BlueMap";
	public static final String PLUGIN_VERSION = BlueMap.VERSION;

	private static SpongePlugin instance;
	
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configurationDir;
	
	@SuppressWarnings("unused")
	@Inject
    private MetricsLite2 metrics;
	
	private Configuration config;
	private ResourcePack resourcePack;

	private Map<UUID, World> worlds;
	private Map<String, MapType> maps;

	private RenderManager renderManager;
	private MapUpdateHandler updateHandler;
	private BlueMapWebServer webServer;

	private SpongeExecutorService syncExecutor;
	private SpongeExecutorService asyncExecutor;
	
	private boolean loaded = false;
	
	@Inject
	public SpongePlugin(org.slf4j.Logger logger) {
		Logger.global = new Slf4jLogger(logger);
		
		this.maps = new HashMap<>();
		this.worlds = new HashMap<>();
		
		instance = this;
	}
	
	public synchronized void load() throws ExecutionException, IOException, NoSuchResourceException, InterruptedException {
		if (loaded) return;
		unload(); //ensure nothing is left running (from a failed load or something)
		
		//load configs
		File configFile = getConfigPath().resolve("bluemap.conf").toFile();
		config = ConfigurationFile.loadOrCreate(configFile, SpongePlugin.class.getResource("/bluemap-sponge.conf")).getConfig();
		
		//load resources
		File defaultResourceFile = config.getDataPath().resolve("minecraft-client-" + ResourcePack.MINECRAFT_CLIENT_VERSION + ".jar").toFile();
		File textureExportFile = config.getWebDataPath().resolve("textures.json").toFile();
		
		if (!defaultResourceFile.exists()) {
			handleMissingResources(defaultResourceFile, configFile);
			unload();
			
			Sponge.getCommandManager().register(this, new Commands(this).createStandaloneReloadCommand(), "bluemap");
			return;
		}
		
		//find more resource packs
		File resourcePackFolder = getConfigPath().resolve("resourcepacks").toFile();
		resourcePackFolder.mkdirs();
		File[] resourcePacks = resourcePackFolder.listFiles();
		Arrays.sort(resourcePacks);
		
		List<File> resources = new ArrayList<>(resourcePacks.length + 1);
		resources.add(defaultResourceFile);
		for (File file : resourcePacks) resources.add(file);

		resourcePack = new ResourcePack(resources, textureExportFile);
		
		//load maps
		for (MapConfig mapConfig : config.getMapConfigs()) {
			String id = mapConfig.getId();
			String name = mapConfig.getName();
			
			File worldFolder = new File(mapConfig.getWorldPath());
			if (!worldFolder.exists() || !worldFolder.isDirectory()) {
				Logger.global.logError("Failed to load map '" + id + "': '" + worldFolder + "' does not exist or is no directory!", new IOException());
				continue;
			}
			
			UUID worldUUID;
			try {
				CompoundTag levelSponge = (CompoundTag) NBTUtil.readTag(new File(worldFolder, "level_sponge.dat"));
				CompoundTag spongeData = levelSponge.getCompoundTag("SpongeData");
				long most = spongeData.getLong("UUIDMost");
				long least = spongeData.getLong("UUIDLeast");
				worldUUID = new UUID(most, least);
			} catch (Exception e) {
				Logger.global.logError("Failed to load map '" + id + "': Failed to read level_sponge.dat", e);
				continue;
			}
			
			World world = worlds.get(worldUUID);
			if (world == null) {
				try {
					world = MCAWorld.load(worldFolder.toPath(), worldUUID);
					worlds.put(worldUUID, world);
				} catch (IOException e) {
					Logger.global.logError("Failed to load map '" + id + "': Failed to read level.dat", e);
					continue;
				}
			}
			
			HiresModelManager hiresModelManager = new HiresModelManager(
					config.getWebDataPath().resolve("hires").resolve(id),
					resourcePack,
					new Vector2i(mapConfig.getHiresTileSize(), mapConfig.getHiresTileSize()),
					getAsyncExecutor()
					);
			
			LowresModelManager lowresModelManager = new LowresModelManager(
					config.getWebDataPath().resolve("lowres").resolve(id), 
					new Vector2i(mapConfig.getLowresPointsPerLowresTile(), mapConfig.getLowresPointsPerLowresTile()),
					new Vector2i(mapConfig.getLowresPointsPerHiresTile(), mapConfig.getLowresPointsPerHiresTile())
					);
			
			TileRenderer tileRenderer = new TileRenderer(hiresModelManager, lowresModelManager, mapConfig);
			
			MapType mapType = new MapType(id, name, world, tileRenderer);
			maps.put(id, mapType);
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
					renderManager.readState(in);
				}
			}
			saveFile.delete();
		} catch (IOException ex) {
			Logger.global.logError("Failed to load render-manager state!", ex);
		}
		
		//start map updater
		this.updateHandler = new MapUpdateHandler();
		
		//create/update webfiles
		WebFilesManager webFilesManager = new WebFilesManager(config.getWebRoot());
		if (webFilesManager.needsUpdate()) {
			webFilesManager.updateFiles();
		}

		WebSettings webSettings = new WebSettings(config.getWebDataPath().resolve("settings.json").toFile());
		for (MapType map : maps.values()) {
			webSettings.setName(map.getName(), map.getId());
			webSettings.setFrom(map.getTileRenderer(), map.getId());
		}
		for (MapConfig map : config.getMapConfigs()) {
			webSettings.setHiresViewDistance(map.getHiresViewDistance(), map.getId());
			webSettings.setLowresViewDistance(map.getLowresViewDistance(), map.getId());
		}
		webSettings.save();
		
		//start webserver
		if (config.isWebserverEnabled()) {
			webServer = new BlueMapWebServer(config);
			webServer.updateWebfiles();
			webServer.start();
		}

		//init commands
		Sponge.getCommandManager().register(this, new Commands(this).createRootCommand(), "bluemap");
		
		//metrics
		Sponge.getScheduler().createTaskBuilder()
		.async()
		.delay(1, TimeUnit.MINUTES)
		.interval(30, TimeUnit.MINUTES)
		.execute(() -> {
			if (Sponge.getMetricsConfigManager().areMetricsEnabled(this)) Metrics.sendReport("Sponge");
		})
		.submit(this);
		
		loaded = true;
	}
	
	public synchronized void unload() {
		//unregister commands
		Sponge.getCommandManager().getOwnedBy(this).forEach(Sponge.getCommandManager()::removeMapping);

		//unregister listeners
		if (updateHandler != null) Sponge.getEventManager().unregisterListeners(updateHandler);

		//stop scheduled tasks
		Sponge.getScheduler().getScheduledTasks(this).forEach(t -> t.cancel());
		
		//stop services
		if (renderManager != null) renderManager.stop();
		if (webServer != null) webServer.close();
		
		//save render-manager state
		if (updateHandler != null) updateHandler.flushTileBuffer(); //first write all buffered tiles to the render manager to save them too
		if (renderManager != null) {
			try {
				File saveFile = config.getDataPath().resolve("rmstate").toFile();
				saveFile.getParentFile().mkdirs();
				if (saveFile.exists()) saveFile.delete();
				saveFile.createNewFile();
				
				try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(saveFile)))) {
					renderManager.writeState(out);
				}
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
	
	public synchronized void reload() throws IOException, NoSuchResourceException, ExecutionException, InterruptedException {
		unload();
		load();
	}
	
	@Listener
	public void onServerStart(GameStartingServerEvent evt) {
		syncExecutor = Sponge.getScheduler().createSyncExecutor(this);
		asyncExecutor = Sponge.getScheduler().createAsyncExecutor(this);
		
		asyncExecutor.execute(() -> {
			try {
				load();
				if (isLoaded()) Logger.global.logInfo("Loaded!");
			} catch (Exception e) {
				Logger.global.logError("Failed to load!", e);
			}
		});
	}

	@Listener
	public void onServerStop(GameStoppingEvent evt) {
		unload();
		Logger.global.logInfo("Saved and stopped!");
	}
	
	@Listener
	public void onServerReload(GameReloadEvent evt) {
		asyncExecutor.execute(() -> {
			try {
				Logger.global.logInfo("Reloading...");
				reload();
				Logger.global.logInfo("Reloaded!");
			} catch (Exception e) {
				Logger.global.logError("Failed to load!", e);
			}
		});
	}
	
	private void handleMissingResources(File resourceFile, File configFile) {
		if (config.isDownloadAccepted()) {
			
			//download file async
			asyncExecutor.execute(() -> {
					try {
						Logger.global.logInfo("Downloading " + ResourcePack.MINECRAFT_CLIENT_URL + " to " + resourceFile + " ...");
						ResourcePack.downloadDefaultResource(resourceFile);
					} catch (IOException e) {
						Logger.global.logError("Failed to download resources!", e);
						return;
					}

					// and reload
					Logger.global.logInfo("Download finished! Reloading...");
					try {
						reload();
					} catch (Exception e) {
						Logger.global.logError("Failed to reload Bluemap!", e);
						return;
					}
					Logger.global.logInfo("Reloaded!");
				});
			
		} else {
			Logger.global.logWarning("BlueMap is missing important resources!");
			Logger.global.logWarning("You need to accept the download of the required files in order of BlueMap to work!");
			Logger.global.logWarning("Please check: " + configFile);
			Logger.global.logInfo("If you have changed the config you can simply reload the plugin using: /bluemap reload");
		}
	}
	
	public SpongeExecutorService getSyncExecutor(){
		return syncExecutor;
	}
	
	public SpongeExecutorService getAsyncExecutor(){
		return asyncExecutor;
	}

	public World getWorld(UUID uuid){
		return worlds.get(uuid);
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
	
	public boolean isLoaded() {
		return loaded;
	}
	
	public Path getConfigPath(){
		return configurationDir;
	}

	public static SpongePlugin getInstance() {
		return instance;
	}
	
}
