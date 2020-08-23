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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector2i;
import com.google.common.base.Preconditions;

import de.bluecolored.bluemap.common.BlueMapWebServer;
import de.bluecolored.bluemap.common.MapType;
import de.bluecolored.bluemap.common.RenderManager;
import de.bluecolored.bluemap.common.RenderTask;
import de.bluecolored.bluemap.core.config.ConfigManager;
import de.bluecolored.bluemap.core.config.CoreConfig;
import de.bluecolored.bluemap.core.config.CoreConfig.MapConfig;
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

public class BlueMapCLI {

	private ConfigManager configManager;
	private File configFolder;
	private ResourcePack resourcePack;
	private boolean forceRender;
	
	public BlueMapCLI(ConfigManager configManager, File configFolder, boolean forceRender) {
		this.configManager = configManager;
		this.configFolder = configFolder;
		this.forceRender = forceRender;
		this.resourcePack = null;
	}
	
	public void renderMaps() throws IOException {
		Preconditions.checkNotNull(resourcePack);
		
		CoreConfig config = configManager.getCoreConfig();
		configManager.loadResourceConfigs(resourcePack);
		
		config.getWebDataPath().toFile().mkdirs();
		
		Map<String, MapType> maps = new HashMap<>(); 

		for (MapConfig mapConfig : config.getMapConfigs()) {
			File worldFolder = new File(mapConfig.getWorldPath());
			if (!worldFolder.exists() || !worldFolder.isDirectory()) {
				throw new IOException("Save folder '" + worldFolder + "' does not exist or is not a directory!");
			}
	
			Logger.global.logInfo("Preparing renderer for map '" + mapConfig.getId() + "' ...");
			World world = MCAWorld.load(worldFolder.toPath(), UUID.randomUUID(), configManager.getBlockIdConfig(), configManager.getBlockPropertiesConfig(), configManager.getBiomeConfig());
			
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
					config.getWebDataPath().resolve(mapConfig.getId()).resolve("hires"),
					resourcePack,
					mapConfig,
					new Vector2i(mapConfig.getHiresTileSize(), mapConfig.getHiresTileSize())
					);
			
			LowresModelManager lowresModelManager = new LowresModelManager(
					config.getWebDataPath().resolve(mapConfig.getId()).resolve("lowres"), 
					new Vector2i(mapConfig.getLowresPointsPerLowresTile(), mapConfig.getLowresPointsPerLowresTile()),
					new Vector2i(mapConfig.getLowresPointsPerHiresTile(), mapConfig.getLowresPointsPerHiresTile()),
					mapConfig.useGzipCompression()
					);
			
			TileRenderer tileRenderer = new TileRenderer(hiresModelManager, lowresModelManager);
			
			MapType mapType = new MapType(mapConfig.getId(), mapConfig.getName(), world, tileRenderer);
			maps.put(mapConfig.getId(), mapType);
		}

		Logger.global.logInfo("Writing settings.json ...");
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

		Logger.global.logInfo("Writing textures.json ...");
		File textureExportFile = config.getWebDataPath().resolve("textures.json").toFile();
		resourcePack.saveTextureFile(textureExportFile);

		RenderManager renderManager = new RenderManager(config.getRenderThreadCount());
		File rmstate = new File(configFolder, "rmstate");
		
		if (rmstate.exists()) {
			try (
				InputStream in = new GZIPInputStream(new FileInputStream(rmstate));
				DataInputStream din = new DataInputStream(in);
			){
				renderManager.readState(din, maps.values());
				Logger.global.logInfo("Found unfinished render, continuing ... (If you want to start a new render, delete the this file: " + rmstate.getCanonicalPath());
			} catch (IOException ex) {
				Logger.global.logError("Failed to read saved render-state! Remove the file " + rmstate.getCanonicalPath() + " to start a new render.", ex);
				return;
			}
		} else {
			for (MapType map : maps.values()) {
				Logger.global.logInfo("Creating render-task for map '" + map.getId() + "' ...");
				Logger.global.logInfo("Collecting tiles ...");
		
				Collection<Vector2i> chunks;
				if (!forceRender) {
					long lastRender = webSettings.getLong("maps", map.getId(), "last-render");
					chunks = map.getWorld().getChunkList(lastRender);
				} else {
					chunks = map.getWorld().getChunkList();
				}
				
				HiresModelManager hiresModelManager = map.getTileRenderer().getHiresModelManager();
				Collection<Vector2i> tiles = hiresModelManager.getTilesForChunks(chunks);
				Logger.global.logInfo("Found " + tiles.size() + " tiles to render! (" + chunks.size() + " chunks)");
				if (!forceRender && chunks.size() == 0) {
					Logger.global.logInfo("(This is normal if nothing has changed in the world since the last render. Use -f on the command-line to force a render of all chunks)");
				}
				
				if (tiles.isEmpty()) {
					continue;
				}
				
				RenderTask task = new RenderTask(map.getId(), map);
				task.addTiles(tiles);
				task.optimizeQueue();
				
				renderManager.addRenderTask(task);
			}
		}

		Logger.global.logInfo("Starting render ...");
		renderManager.start();
		
		long startTime = System.currentTimeMillis();
		
		long lastLogUpdate = startTime;
		long lastSave = startTime;
		
		while(renderManager.getRenderTaskCount() != 0) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
			

			long now = System.currentTimeMillis();
			
			if (lastLogUpdate < now - 10000) { // print update all 10 seconds
				RenderTask currentTask = renderManager.getCurrentRenderTask();
				if (currentTask == null) continue;
				
				lastLogUpdate = now;
				long time = currentTask.getActiveTime();
				
				String durationString = DurationFormatUtils.formatDurationWords(time, true, true);
				int tileCount = currentTask.getRemainingTileCount() + currentTask.getRenderedTileCount();
				double pct = (double)currentTask.getRenderedTileCount() / (double) tileCount;
				
				long ert = (long)((time / pct) * (1d - pct));
				String ertDurationString = DurationFormatUtils.formatDurationWords(ert, true, true);
				
				double tps = currentTask.getRenderedTileCount() / (time / 1000.0);
				
				Logger.global.logInfo("Rendering map '" + currentTask.getName() + "':");
				Logger.global.logInfo("Rendered " + currentTask.getRenderedTileCount() + " of " + tileCount + " tiles in " + durationString + " | " + GenericMath.round(tps, 3) + " tiles/s");
				Logger.global.logInfo(GenericMath.round(pct * 100, 3) + "% | Estimated remaining time: " + ertDurationString);
			}
			
			if (lastSave < now - 1 * 60000) { // save every minute
				RenderTask currentTask = renderManager.getCurrentRenderTask();
				if (currentTask == null) continue;
				
				lastSave = now;
				currentTask.getMapType().getTileRenderer().save();

				try (
					OutputStream os = new GZIPOutputStream(new FileOutputStream(rmstate));
					DataOutputStream dos = new DataOutputStream(os);
				){
					renderManager.writeState(dos);
				} catch (IOException ex) {
					Logger.global.logError("Failed to save render-state!", ex);
				}
			}
		}

		renderManager.stop();
		
		//render finished, so remove render state file
		rmstate.delete();

		for (MapType map : maps.values()) {
			webSettings.set(startTime, "maps", map.getId(), "last-render");
		}
		
		try {
			webSettings.save();
		} catch (IOException e) {
			Logger.global.logError("Failed to update web-settings!", e);
		}

		Logger.global.logInfo("Render finished!");
	}
	
	public void startWebserver() throws IOException {
		Logger.global.logInfo("Starting webserver ...");
		
		BlueMapWebServer webserver = new BlueMapWebServer(configManager.getCoreConfig());
		webserver.start();
	}
	
	private boolean loadResources() throws IOException, ParseResourceException {
		Logger.global.logInfo("Loading resources ...");

		CoreConfig config = configManager.getCoreConfig();
		
		File defaultResourceFile = config.getDataPath().resolve("minecraft-client-" + ResourcePack.MINECRAFT_CLIENT_VERSION + ".jar").toFile();
		File resourceExtensionsFile = config.getDataPath().resolve("resourceExtensions.zip").toFile();
		File textureExportFile = config.getWebDataPath().resolve("textures.json").toFile();
		
		if (!defaultResourceFile.exists()) {
			if (!handleMissingResources(defaultResourceFile)) return false;
		}
		
		resourceExtensionsFile.delete();
		FileUtils.copyURLToFile(BlueMapCLI.class.getResource("/resourceExtensions.zip"), resourceExtensionsFile, 10000, 10000);
		
		//find more resource packs
		File resourcePackFolder = configFolder.toPath().resolve("resourcepacks").toFile();
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
		
		return true;
	}
	
	private boolean handleMissingResources(File resourceFile) throws IOException {
		if (configManager.getCoreConfig().isDownloadAccepted()) {
			try {
				Logger.global.logInfo("Downloading " + ResourcePack.MINECRAFT_CLIENT_URL + " to " + resourceFile.getCanonicalPath() + " ...");
				ResourcePack.downloadDefaultResource(resourceFile);
				return true;
			} catch (IOException e) {
				Logger.global.logError("Failed to download resources!", e);
				return false;
			}
		} else {
			Logger.global.logWarning("BlueMap is missing important resources!");
			Logger.global.logWarning("You need to accept the download of the required files in order of BlueMap to work!");
			Logger.global.logWarning("Please check " + configManager.getCoreConfigFile().getCanonicalPath() + " and try again!");
			return false;
		}
	}
	
	public static void main(String[] args) throws IOException, ParseResourceException {
		CommandLineParser parser = new DefaultParser();
		
		try {
			CommandLine cmd = parser.parse(BlueMapCLI.createOptions(), args, false);

			//help
			if (cmd.hasOption("h")) {
				BlueMapCLI.printHelp();
				return;
			}
			
			//load config
			File configFolder = new File(".");
			if (cmd.hasOption("c")) {
				configFolder = new File(cmd.getOptionValue("c"));
				configFolder.mkdirs();
			}
			
			URL cliConfigUrl = BlueMapCLI.class.getResource("/bluemap-cli.conf");
			URL cliDefaultsUrl = BlueMapCLI.class.getResource("/bluemap-cli-defaults.conf");
			
			ConfigManager config = new ConfigManager(configFolder, cliConfigUrl, cliDefaultsUrl);
			boolean configCreated = !config.getCoreConfigFile().exists();
			config.loadMainConfig();
			
			if (configCreated) {
				Logger.global.logInfo("No config file found! Created default config here: " + config.getCoreConfigFile().getCanonicalPath());
				return;
			}
			
			WebFilesManager webFilesManager = new WebFilesManager(config.getCoreConfig().getWebRoot());
			if (webFilesManager.needsUpdate()) {
				Logger.global.logInfo("Updating webfiles in " + config.getCoreConfig().getWebRoot().normalize() + "...");
				webFilesManager.updateFiles();
			}
			
			BlueMapCLI bluemap = new BlueMapCLI(config, configFolder, cmd.hasOption("f"));

			if (config.getCoreConfig().isWebserverEnabled()) {
				//start webserver
				bluemap.startWebserver();

				//wait a second to let the webserver start, looks nicer in the log
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
			}
			
			
			if (!config.getCoreConfig().getMapConfigs().isEmpty()) {
				//load resources
				if (bluemap.loadResources()) {
					
					//metrics
					if (config.getCoreConfig().isMetricsEnabled()) Metrics.sendReportAsync("CLI");
					
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
				.argName("config-folder")
				.desc("Sets path of the folder containing the configuration-files to use (configurations will be generated here if they don't exist)")
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
					filename = "." + File.separator + new File("").getCanonicalFile().toPath().relativize(file.toPath()).toString();
				} catch (IllegalArgumentException ex) {
					filename = file.getAbsolutePath();
				}
			}
		} catch (IOException ex) {}
		
		String command = "java -jar " + filename;
		
		formatter.printHelp(command + " [options]", "\nOptions:", createOptions(), "");
	}
	
}
