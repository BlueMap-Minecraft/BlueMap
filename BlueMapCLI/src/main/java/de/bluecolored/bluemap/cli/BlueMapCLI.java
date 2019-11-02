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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.render.StaticRenderSettings;
import de.bluecolored.bluemap.core.render.TileRenderer;
import de.bluecolored.bluemap.core.render.hires.HiresModelManager;
import de.bluecolored.bluemap.core.render.lowres.LowresModelManager;
import de.bluecolored.bluemap.core.resourcepack.NoSuchResourceException;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.web.BlueMapWebRequestHandler;
import de.bluecolored.bluemap.core.web.WebFilesManager;
import de.bluecolored.bluemap.core.web.WebSettings;
import de.bluecolored.bluemap.core.webserver.WebServer;
import de.bluecolored.bluemap.core.world.World;

public class BlueMapCLI {
	
	private File webroot = new File("web");
	private File dataPath = new File(webroot, "data");
	
	private File extraResourceFile = null;
	private int threadCount;
	
	private String mapId = null;
	private String mapName = null;
	
	private int highresTileSize = 32;
	private int lowresTileSize = 50;
	private int samplesPerHighresTile = 4;
	
	private float highresViewDistance = 6f;
	private float lowresViewDistance = 5f;
	
	private boolean excludeFacesWithoutSunlight = true;
	private float ambientOcclusion = 0.25f;
	private float lighting = 0.8f;
	private int sliceY = Integer.MAX_VALUE;
	private int maxY = Integer.MAX_VALUE;
	private int minY = 0;
	
	private int port = 8100;
	private int maxConnections = 100;
	private InetAddress bindAdress = null;
	
	public BlueMapCLI() {
		threadCount = Runtime.getRuntime().availableProcessors();
	}
	
	public void renderMap(File mapPath, boolean updateOnly) throws IOException, NoSuchResourceException {
		dataPath.mkdirs();

		if (!mapPath.exists() || !mapPath.isDirectory()) {
			throw new IOException("Save folder '" + mapPath + "' does not exist or is not a directory!");
		}

		Logger.global.logInfo("Reading world...");
		World world = MCAWorld.load(mapPath.toPath(), UUID.randomUUID());
		
		if (mapName == null) {
			mapName = world.getName();
		}
		
		if (mapId == null) {
			mapId = mapPath.getName().toLowerCase();
		}
		
		Logger.global.logInfo("Starting Render:"
				+ "\n map: " + mapPath.getAbsolutePath()
				+ "\n map-id: " + mapId
				+ "\n map-name: " + mapName
				+ "\n thread-count: " + threadCount
				+ "\n data-path: " + dataPath.getAbsolutePath()
				+ "\n render-all: " + !excludeFacesWithoutSunlight
				+ "\n ambient-occlusion: " + ambientOcclusion
				+ "\n lighting: " + lighting
				+ "\n sliceY: " + (sliceY < Integer.MAX_VALUE ? sliceY : "-")
				+ "\n maxY: " + (maxY < Integer.MAX_VALUE ? maxY : "-")
				+ "\n minY: " + (minY > 0 ? minY : "-")
				+ "\n hr-tilesize: " + highresTileSize
				+ "\n lr-tilesize: " + lowresTileSize
				+ "\n lr-resolution: " + samplesPerHighresTile
				+ "\n hr-viewdistance: " + highresViewDistance
				+ "\n lr-viewdistance: " + lowresViewDistance
		);

		Logger.global.logInfo("Loading Resources...");
		ResourcePack resourcePack = loadResources();
		
		Logger.global.logInfo("Initializing renderer...");
		HiresModelManager hiresModelManager = new HiresModelManager(
				dataPath.toPath().resolve("hires").resolve(mapId),
				resourcePack,
				new Vector2i(highresTileSize, highresTileSize),
				ForkJoinPool.commonPool()
				);
		
		LowresModelManager lowresModelManager = new LowresModelManager(
				dataPath.toPath().resolve("lowres").resolve(mapId), 
				new Vector2i(lowresTileSize, lowresTileSize),
				new Vector2i(samplesPerHighresTile, samplesPerHighresTile)
				);
		
		TileRenderer tileRenderer = new TileRenderer(hiresModelManager, lowresModelManager, new StaticRenderSettings(
				ambientOcclusion, 
				excludeFacesWithoutSunlight, 
				lighting, 
				maxY, 
				minY, 
				sliceY
				));
		
		File webSettingsFile = new File(dataPath, "settings.json");
		Logger.global.logInfo("Writing '" + webSettingsFile.getAbsolutePath() + "'...");
		WebSettings webSettings = new WebSettings(webSettingsFile);
		webSettings.setName(mapName, mapId);
		webSettings.setFrom(tileRenderer, mapId);
		webSettings.setHiresViewDistance(highresViewDistance, mapId);
		webSettings.setLowresViewDistance(lowresViewDistance, mapId);
		webSettings.save();
		
		
		Logger.global.logInfo("Collecting tiles to render...");

		Collection<Vector2i> chunks;
		if (updateOnly) {
			long lastRender = webSettings.getLong(mapId, "last-render");
			chunks = world.getChunkList(lastRender);
		} else {
			chunks = world.getChunkList();
		}
		
		Set<Vector2i> tiles = new HashSet<>();
		for (Vector2i chunk : chunks) {
			Vector3i minBlockPos = new Vector3i(chunk.getX() * 16, 0, chunk.getY() * 16);
			tiles.add(hiresModelManager.posToTile(minBlockPos));
			tiles.add(hiresModelManager.posToTile(minBlockPos.add(0, 0, 15)));
			tiles.add(hiresModelManager.posToTile(minBlockPos.add(15, 0, 0)));
			tiles.add(hiresModelManager.posToTile(minBlockPos.add(15, 0, 15)));
		}
		Logger.global.logInfo("Found " + tiles.size() + " tiles to render! (" + chunks.size() + " chunks)");
		
		if (tiles.isEmpty()) {
			Logger.global.logInfo("Render finished!");
			return;
		}
		
		Logger.global.logInfo("Starting Render...");
		long starttime = System.currentTimeMillis();
		RenderManager renderManager = new RenderManager(world, tileRenderer, tiles, threadCount);
		renderManager.start(() -> {
			Logger.global.logInfo("Waiting for threads to quit...");
			if (!ForkJoinPool.commonPool().awaitQuiescence(30, TimeUnit.SECONDS)) {
				Logger.global.logWarning("Some save-threads are taking very long to exit (>30s), they will be ignored.");
			}

			try {
				webSettings.set(starttime, mapId, "last-render");
				webSettings.save();
			} catch (IOException e) {
				Logger.global.logError("Failed to update web-settings!", e);
			}
			
			Logger.global.logInfo("Render finished!");
		});
	}
	
	public void updateWebFiles() throws IOException {
		webroot.mkdirs();

		Logger.global.logInfo("Creating webfiles in " + webroot.getAbsolutePath());
		WebFilesManager webFilesManager = new WebFilesManager(webroot.toPath());
		webFilesManager.updateFiles();
	}
	
	public void startWebserver() throws UnknownHostException {
		if (bindAdress == null) bindAdress = InetAddress.getLocalHost();
		
		Logger.global.logInfo("Starting webserver:"
				+ "\n address: " + this.bindAdress.toString() + ""
				+ "\n port: " + this.port
				+ "\n max connections: " + this.maxConnections
				+ "\n webroot: " + this.webroot.getAbsolutePath()
		);
		
		WebServer webserver = new WebServer(
				this.port, 
				this.maxConnections, 
				this.bindAdress, 
				new BlueMapWebRequestHandler(this.webroot.toPath())
			);
		
		webserver.start();
	}
	
	private ResourcePack loadResources() throws IOException, NoSuchResourceException {
		File defaultResourceFile;
		try {
			defaultResourceFile = File.createTempFile("res", ".zip");
			defaultResourceFile.delete();
		} catch (IOException e) {
			throw new IOException("Failed to create temporary resource file!", e);
		}
		try {
			ResourcePack.createDefaultResource(defaultResourceFile);
		} catch (IOException e) {
			throw new IOException("Failed to create default resources!", e);
		}
		
		List<File> resourcePacks = new ArrayList<>();
		resourcePacks.add(defaultResourceFile);
		if (this.extraResourceFile != null) resourcePacks.add(extraResourceFile);
		
		ResourcePack resourcePack = new ResourcePack(resourcePacks, new File(dataPath, "textures.json"));
		
		defaultResourceFile.delete();
		
		return resourcePack;
	}
	
	public static void main(String[] args) throws IOException, NoSuchResourceException {
		CommandLineParser parser = new DefaultParser();
		
		try {
			CommandLine cmd = parser.parse(BlueMapCLI.createOptions(), args, false);
			
			if (cmd.hasOption("h")) {
				BlueMapCLI.printHelp();
				return;
			}

			boolean executed = false;
			
			BlueMapCLI bluemapcli = new BlueMapCLI();
			
			if (cmd.hasOption("o")) bluemapcli.dataPath = new File(cmd.getOptionValue("o"));
			if (cmd.hasOption("r")) bluemapcli.extraResourceFile = new File(cmd.getOptionValue("r"));
			if (cmd.hasOption("t")) bluemapcli.threadCount = Integer.parseInt(cmd.getOptionValue("t"));

			if (cmd.hasOption("d")) bluemapcli.webroot = new File(cmd.getOptionValue("d"));
			if (cmd.hasOption("i")) bluemapcli.bindAdress = InetAddress.getByName(cmd.getOptionValue("i"));
			bluemapcli.port = Integer.parseInt(cmd.getOptionValue("p", Integer.toString(bluemapcli.port)));
			bluemapcli.maxConnections = Integer.parseInt(cmd.getOptionValue("connections", Integer.toString(bluemapcli.maxConnections)));
			
			bluemapcli.mapName = cmd.getOptionValue("n", bluemapcli.mapName);
			bluemapcli.mapId = cmd.getOptionValue("id", bluemapcli.mapId);
			
			bluemapcli.ambientOcclusion = Float.parseFloat(cmd.getOptionValue("ao", Float.toString(bluemapcli.ambientOcclusion)));
			bluemapcli.lighting = Float.parseFloat(cmd.getOptionValue("lighting", Float.toString(bluemapcli.lighting)));
			bluemapcli.sliceY = Integer.parseInt(cmd.getOptionValue("y-slice", Integer.toString(bluemapcli.sliceY)));
			bluemapcli.maxY = Integer.parseInt(cmd.getOptionValue("y-max", Integer.toString(bluemapcli.maxY)));
			bluemapcli.minY = Integer.parseInt(cmd.getOptionValue("y-min", Integer.toString(bluemapcli.minY)));
			
			bluemapcli.highresTileSize = Integer.parseInt(cmd.getOptionValue("hr-tilesize", Integer.toString(bluemapcli.highresTileSize)));
			bluemapcli.highresViewDistance = Float.parseFloat(cmd.getOptionValue("hr-viewdist", Float.toString(bluemapcli.highresViewDistance)));
			bluemapcli.lowresTileSize = Integer.parseInt(cmd.getOptionValue("lr-tilesize", Integer.toString(bluemapcli.lowresTileSize)));
			bluemapcli.samplesPerHighresTile = Integer.parseInt(cmd.getOptionValue("lr-resolution", Integer.toString(bluemapcli.samplesPerHighresTile)));
			bluemapcli.lowresViewDistance = Float.parseFloat(cmd.getOptionValue("lr-viewdist", Float.toString(bluemapcli.lowresViewDistance)));
			
			if (cmd.hasOption("c")) {
				bluemapcli.updateWebFiles();
				executed = true;
			}
			
			if (cmd.hasOption("s")) {
				bluemapcli.startWebserver();
				executed = true;
			}
			
			if (cmd.hasOption("w")) {
				bluemapcli.renderMap(new File(cmd.getOptionValue("w")), !cmd.hasOption("f"));
				executed = true;
			}
			
			if (executed) return;
			
		} catch (ParseException e) {
			Logger.global.logError("Failed to parse provided arguments!", e);
		} catch (NumberFormatException e) {
			Logger.global.logError("One argument expected a number but got the wrong format!", e);
		}

		BlueMapCLI.printHelp();
	}
	
	private static Options createOptions() {
		Options options = new Options();
		
		options.addOption("h", "help", false, "Displays this message");
		
		options.addOption(
				Option.builder("o")
					.longOpt("out")
					.hasArg()
					.argName("directory-path")
					.desc("Defines the render-output directory. Default is '<webroot>/data' (See option -d)")
					.build()
				);
		options.addOption(
				Option.builder("d")
					.longOpt("dir")
					.hasArg()
					.argName("directory-path")
					.desc("Defines the webroot directory. Default is './web'")
					.build()
				);
		
		options.addOption("s", "webserver", false, "Starts the integrated webserver");
		options.addOption(
				Option.builder("c")
					.longOpt("create-web")
					.desc("The webfiles will be (re)created, existing web-files in the webroot will be replaced!")
					.build()
				);
		options.addOption(
				Option.builder("i")
					.longOpt("ip")
					.hasArg()
					.argName("ip-adress")
					.desc("Specifies the IP adress the webserver will use")
					.build()
				);
		options.addOption(
				Option.builder("p")
					.longOpt("port")
					.hasArg()
					.argName("port")
					.desc("Specifies the port the webserver will use. Default is 8100")
					.build()
				);
		options.addOption(
				Option.builder()
					.longOpt("connections")
					.hasArg()
					.argName("count")
					.desc("Sets the maximum count of simultaneous client-connections that the webserver will allow. Default is 100")
					.build()
				);
		
		options.addOption(
				Option.builder("w")
					.longOpt("world")
					.hasArg()
					.argName("directory-path")
					.desc("Defines the world-save folder that will be rendered")
					.build()
				);
		options.addOption(
				Option.builder("f")
					.longOpt("force-render")
					.desc("Rerenders all tiles even if there are no changes since the last render")
					.build()
				);
		options.addOption(
				Option.builder("r")
					.longOpt("resource")
					.hasArg()
					.argName("file")
					.desc("Defines the resourcepack that will be used to render the map")
					.build()
				);
		options.addOption(
				Option.builder("t")
					.longOpt("threads")
					.hasArg()
					.argName("thread-count")
					.desc("Defines the number of threads that will be used to render the map. Default is the number of system cores")
					.build()
				);
		options.addOption(
				Option.builder("I")
					.longOpt("id")
					.hasArg()
					.argName("id")
					.desc("The id of the world. Default is the name of the world-folder")
					.build()
				);
		options.addOption(
				Option.builder("n")
					.longOpt("name")
					.hasArg()
					.argName("name")
					.desc("The name of the world. Default is the world-name defined in the level.dat")
					.build()
				);
		options.addOption(
				Option.builder()
					.longOpt("render-all")
					.desc("Also renders blocks that are normally omitted due to a sunlight value of 0. Enabling this can cause a big performance impact in the web-viewer, but it might fix some cases where blocks are missing.")
					.build()
				);
		options.addOption(
				Option.builder("ao")
					.longOpt("ambient-occlusion")
					.hasArg()
					.argName("value")
					.desc("The strength of ambient-occlusion baked into the model (a value between 0 and 1). Default is 0.25")
					.build()
				);
		options.addOption(
				Option.builder("l")
					.longOpt("lighting")
					.hasArg()
					.argName("value")
					.desc("The max strength of shadows baked into the model (a value between 0 and 1 where 0 is fully bright (no lighting) and 1 is max lighting-contrast). Default is 0.8")
					.build()
				);
		options.addOption(
				Option.builder("ys")
					.longOpt("y-slice")
					.hasArg()
					.argName("value")
					.desc("Using this, BlueMap pretends that every Block above the defined value is AIR. Default is disabled")
					.build()
				);
		options.addOption(
				Option.builder("yM")
					.longOpt("y-max")
					.hasArg()
					.argName("value")
					.desc("Blocks above this height will not be rendered. Default is no limit")
					.build()
				);
		options.addOption(
				Option.builder("ym")
					.longOpt("y-min")
					.hasArg()
					.argName("value")
					.desc("Blocks below this height will not be rendered. Default is no limit")
					.build()
				);

		options.addOption(
				Option.builder()
					.longOpt("hr-tilesize")
					.hasArg()
					.argName("value")
					.desc("Defines the size of one map-tile in blocks. If you change this value, the lowres values might need adjustment as well! Default is 32")
					.build()
				);
		options.addOption(
				Option.builder()
					.longOpt("hr-viewdist")
					.hasArg()
					.argName("value")
					.desc("The View-Distance for hires tiles on the web-map (the value is the radius in tiles). Default is 6")
					.build()
				);
		options.addOption(
				Option.builder()
					.longOpt("lr-tilesize")
					.hasArg()
					.argName("value")
					.desc("Defines the size of one lowres-map-tile in grid-points. Default is 50")
					.build()
				);
		options.addOption(
				Option.builder()
					.longOpt("lr-resolution")
					.hasArg()
					.argName("value")
					.desc("Defines resolution of the lowres model. E.g. If the hires.tileSize is 32, a value of 4 means that every 8*8 blocks will be summarized by one point on the lowres map. Calculation: 32 / 4 = 8! You have to use values that result in an integer if you use the above calculation! Default is 4")
					.build()
				);
		options.addOption(
				Option.builder()
					.longOpt("lr-viewdist")
					.hasArg()
					.argName("value")
					.desc("The View-Distance for lowres tiles on the web-map (the value is the radius in tiles). Default is 5")
					.build()
				);
		
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
					filename = "./" + new File(".").toPath().relativize(file.toPath()).toString();
				} catch (IllegalArgumentException ex) {
					filename = file.getAbsolutePath();
				}
			}
		} catch (Exception ex) {}
		
		String command = "java -jar " + filename;
		
		formatter.printHelp(command + " [options]", "\nOptions:", createOptions(), "\n"
				+ "Examples:\n\n"
				+ command + " -w ./world/\n"
				+ " -> Renders the whole world to ./web/data/\n\n"
				+ command + " -csi localhost\n"
				+ " -> Creates all neccesary web-files in ./web/ and starts the webserver. (Open http://localhost:8100/ in your browser)"
			);
	}
	
}
