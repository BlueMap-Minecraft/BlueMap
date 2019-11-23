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
package de.bluecolored.bluemap.cli;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.Preconditions;

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
import de.bluecolored.bluemap.core.web.WebSettings;
import de.bluecolored.bluemap.core.world.World;

public class BlueMapCLI {

	private ConfigurationFile configFile;
	private Configuration config;
	private ResourcePack resourcePack;
	private boolean forceRender;
	
	public BlueMapCLI(ConfigurationFile configFile, boolean forceRender) {
		this.configFile = configFile;
		this.config = configFile.getConfig();
		this.forceRender = forceRender;
		this.resourcePack = null;
	}
	
	public void renderMaps() throws IOException, NoSuchResourceException {
		Preconditions.checkNotNull(resourcePack);
		
		config.getWebDataPath().toFile().mkdirs();
		
		Map<String, MapType> maps = new HashMap<>(); 
		
		for (MapConfig mapConfig : config.getMapConfigs()) {
			File mapPath = new File(mapConfig.getWorldPath());
			if (!mapPath.exists() || !mapPath.isDirectory()) {
				throw new IOException("Save folder '" + mapPath + "' does not exist or is not a directory!");
			}
	
			Logger.global.logInfo("Preparing renderer for map '" + mapConfig.getId() + "' ...");
			World world = MCAWorld.load(mapPath.toPath(), UUID.randomUUID());
			
			HiresModelManager hiresModelManager = new HiresModelManager(
					config.getWebDataPath().resolve("hires").resolve(mapConfig.getId()),
					resourcePack,
					new Vector2i(mapConfig.getHiresTileSize(), mapConfig.getHiresTileSize()),
					ForkJoinPool.commonPool()
					);
			
			LowresModelManager lowresModelManager = new LowresModelManager(
					config.getWebDataPath().resolve("lowres").resolve(mapConfig.getId()), 
					new Vector2i(mapConfig.getLowresPointsPerLowresTile(), mapConfig.getLowresPointsPerLowresTile()),
					new Vector2i(mapConfig.getLowresPointsPerHiresTile(), mapConfig.getLowresPointsPerHiresTile())
					);
			
			TileRenderer tileRenderer = new TileRenderer(hiresModelManager, lowresModelManager, mapConfig);
			
			MapType mapType = new MapType(mapConfig.getId(), mapConfig.getName(), world, tileRenderer);
			maps.put(mapConfig.getId(), mapType);
		}

		Logger.global.logInfo("Writing settings.json ...");
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

		for (MapType map : maps.values()) {
			Logger.global.logInfo("Rendering map '" + map.getId() + "' ...");
			Logger.global.logInfo("Collecting tiles to render...");
	
			Collection<Vector2i> chunks;
			if (!forceRender) {
				long lastRender = webSettings.getLong(map.getId(), "last-render");
				chunks = map.getWorld().getChunkList(lastRender);
			} else {
				chunks = map.getWorld().getChunkList();
			}
			
			HiresModelManager hiresModelManager = map.getTileRenderer().getHiresModelManager();
			Set<Vector2i> tiles = new HashSet<>();
			for (Vector2i chunk : chunks) {
				Vector3i minBlockPos = new Vector3i(chunk.getX() * 16, 0, chunk.getY() * 16);
				tiles.add(hiresModelManager.posToTile(minBlockPos));
				tiles.add(hiresModelManager.posToTile(minBlockPos.add(0, 0, 15)));
				tiles.add(hiresModelManager.posToTile(minBlockPos.add(15, 0, 0)));
				tiles.add(hiresModelManager.posToTile(minBlockPos.add(15, 0, 15)));
			}
			Logger.global.logInfo("Found " + tiles.size() + " tiles to render! (" + chunks.size() + " chunks)");
			if (!forceRender && chunks.size() == 0) {
				Logger.global.logInfo("(This is normal if nothing has changed in the world since the last render. Use -f on the command-line to force a render of all chunks)");
			}
			
			if (tiles.isEmpty()) {
				Logger.global.logInfo("Render finished!");
				return;
			}
		
			Logger.global.logInfo("Starting Render...");
			long starttime = System.currentTimeMillis();
			
			RenderTask task = new RenderTask(map, tiles, config.getRenderThreadCount());
			task.render();
	
			try {
				webSettings.set(starttime, map.getId(), "last-render");
				webSettings.save();
			} catch (IOException e) {
				Logger.global.logError("Failed to update web-settings!", e);
			}
		}

		Logger.global.logInfo("Waiting for all threads to quit...");
		if (!ForkJoinPool.commonPool().awaitQuiescence(30, TimeUnit.SECONDS)) {
			Logger.global.logWarning("Some save-threads are taking very long to exit (>30s), they will be ignored.");
		}

		Logger.global.logInfo("Render finished!");
	}
	
	public void startWebserver() throws IOException {
		Logger.global.logInfo("Starting webserver...");
		
		BlueMapWebServer webserver = new BlueMapWebServer(config);
		webserver.updateWebfiles();
		webserver.start();
	}
	
	private boolean loadResources() throws IOException, NoSuchResourceException {
		File defaultResourceFile = config.getDataPath().resolve("minecraft-client-" + ResourcePack.MINECRAFT_CLIENT_VERSION + ".jar").toFile();
		File textureExportFile = config.getWebDataPath().resolve("textures.json").toFile();
		
		if (!defaultResourceFile.exists()) {
			if (!handleMissingResources(defaultResourceFile)) return false;
		}
		
		//find more resource packs
		File resourcePackFolder = configFile.getFile().toPath().resolveSibling("resourcepacks").toFile();
		resourcePackFolder.mkdirs();
		File[] resourcePacks = resourcePackFolder.listFiles();
		Arrays.sort(resourcePacks);
		
		List<File> resources = new ArrayList<>(resourcePacks.length + 1);
		resources.add(defaultResourceFile);
		for (File file : resourcePacks) resources.add(file);
		
		resourcePack = new ResourcePack(resources, textureExportFile);
		
		return true;
	}
	
	private boolean handleMissingResources(File resourceFile) {
		if (config.isDownloadAccepted()) {
			try {
				Logger.global.logInfo("Downloading " + ResourcePack.MINECRAFT_CLIENT_URL + " to " + resourceFile + " ...");
				ResourcePack.downloadDefaultResource(resourceFile);
				return true;
			} catch (IOException e) {
				Logger.global.logError("Failed to download resources!", e);
				return false;
			}
		} else {
			Logger.global.logWarning("BlueMap is missing important resources!");
			Logger.global.logWarning("You need to accept the download of the required files in order of BlueMap to work!");
			Logger.global.logWarning("Please check: " + configFile.getFile() + " and try again!");
			return false;
		}
	}
	
	public static void main(String[] args) throws IOException, NoSuchResourceException {
		CommandLineParser parser = new DefaultParser();
		
		try {
			CommandLine cmd = parser.parse(BlueMapCLI.createOptions(), args, false);

			//help
			if (cmd.hasOption("h")) {
				BlueMapCLI.printHelp();
				return;
			}
			
			//load config
			File configFile = new File("bluemap.conf").getAbsoluteFile();
			if (cmd.hasOption("c")) {
				configFile = new File(cmd.getOptionValue("c"));
				configFile.getParentFile().mkdirs();
			}
			
			boolean configCreated = !configFile.exists();
			
			ConfigurationFile config = ConfigurationFile.loadOrCreate(configFile, BlueMapCLI.class.getResource("/bluemap-cli.conf"));
			
			if (configCreated) {
				Logger.global.logInfo("No config file found! Created an example config here: " + configFile);
				return;
			}
			
			BlueMapCLI bluemap = new BlueMapCLI(config, cmd.hasOption("f"));

			if (config.getConfig().isWebserverEnabled()) {
				//start webserver
				bluemap.startWebserver();
			}
			
			if (!config.getConfig().getMapConfigs().isEmpty()) {
				//load resources
				if (bluemap.loadResources()) {
					
					//metrics
					if (config.getConfig().isMetricsEnabled()) Metrics.sendReportAsync("CLI");
					
					//render maps
					bluemap.renderMaps();
					
					//since we don't need it any more, free some memory
					bluemap.resourcePack = null;
				}
			}
			
		} catch (ParseException e) {
			Logger.global.logError("Failed to parse provided arguments!", e);
			BlueMapCLI.printHelp();
			return;
		}
	}
	
	private static Options createOptions() {
		Options options = new Options();
		
		options.addOption("h", "help", false, "Displays this message");

		options.addOption(
				Option.builder("c")
				.longOpt("config")
				.hasArg()
				.argName("config-file")
				.desc("Sets path of the configuration file to use")
				.build()
			);

		options.addOption("f", "force-render", false, "Forces rendering everything, instead of only rendering chunks that have been modified since the last render");
		
		return options;
	}

	private static void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		
		String filename = "bluemapcli.jar";
		try {
			File file = new File(BlueMapCLI.class.getProtectionDomain()
					.getCodeSource()
					.getLocation()
					.getPath());
			
			if (file.isFile()) {
				try {
					filename = "." + File.separator + new File("").getAbsoluteFile().toPath().relativize(file.toPath()).toString();
				} catch (IllegalArgumentException ex) {
					filename = file.getAbsolutePath();
				}
			}
		} catch (Exception ex) {}
		
		String command = "java -jar " + filename;
		
		formatter.printHelp(command + " [options]", "\nOptions:", createOptions(), "");
	}
	
}
