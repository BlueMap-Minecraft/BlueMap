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

import de.bluecolored.bluemap.common.BlueMapConfiguration;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.MissingResourcesException;
import de.bluecolored.bluemap.common.addons.Addons;
import de.bluecolored.bluemap.common.api.BlueMapAPIImpl;
import de.bluecolored.bluemap.common.commands.TextFormat;
import de.bluecolored.bluemap.common.config.BlueMapConfigManager;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.common.config.CoreConfig;
import de.bluecolored.bluemap.common.config.WebserverConfig;
import de.bluecolored.bluemap.common.metrics.Metrics;
import de.bluecolored.bluemap.common.plugin.MapUpdateService;
import de.bluecolored.bluemap.common.rendermanager.*;
import de.bluecolored.bluemap.common.web.*;
import de.bluecolored.bluemap.common.web.http.HttpRequestHandler;
import de.bluecolored.bluemap.common.web.http.HttpServer;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.storage.MapStorage;
import de.bluecolored.bluemap.core.util.FileHelper;
import org.apache.commons.cli.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class BlueMapCLI {

    private static boolean shutdownInProgress = false;

    private String minecraftVersion = null;
    private Path configFolder = Path.of("config");
    private Path modsFolder = null;


    public void renderMaps(BlueMapService blueMap, boolean watch, TileUpdateStrategy force, boolean forceGenerateWebapp,
                           @Nullable String mapsToRender) throws ConfigurationException, IOException, InterruptedException {

        if (blueMap.getConfig().getWebappConfig().isEnabled())
            blueMap.createOrUpdateWebApp(forceGenerateWebapp);

        //try load resources
        blueMap.getOrLoadResourcePack();

        //create renderManager
        RenderManager renderManager = new RenderManager();

        //load maps
        Predicate<String> mapFilter = mapId -> true;
        if (mapsToRender != null) {
            Set<String> mapsToRenderSet = Set.of(mapsToRender.split(","));
            mapFilter = mapsToRenderSet::contains;
        }
        Map<String, BmMap> maps = blueMap.getOrLoadMaps(mapFilter);

        //watcher
        List<MapUpdateService> mapUpdateServices = new ArrayList<>();
        if (watch) {
            for (BmMap map : maps.values()) {
                try {
                    MapUpdateService watcher = new MapUpdateService(renderManager, map);
                    watcher.start();
                    mapUpdateServices.add(watcher);
                } catch (IOException ex) {
                    Logger.global.logError("Failed to create update-watcher for map: " + map.getId() +
                            " (This means the map might not automatically update)", ex);
                } catch (UnsupportedOperationException ex) {
                    Logger.global.logWarning("Update-watcher for map '" + map.getId() + "' is not supported for the world-type." +
                            " (This means the map might not automatically update)");
                }
            }
        }

        //update all maps
        for (BmMap map : maps.values()) {
            renderManager.scheduleRenderTask(MapUpdatePreparationTask.builder()
                    .map(map)
                    .force(force)
                    .taskConsumer(renderManager::scheduleRenderTaskNext)
                    .build());
        }

        // enable api
        BlueMapAPIImpl api = new BlueMapAPIImpl(blueMap, null);
        api.register();

        Logger.global.logInfo("Start updating " + maps.size() + " maps ...");

        // start rendering
        renderManager.start(blueMap.getConfig().getCoreConfig().resolveRenderThreadCount());

        Timer timer = new Timer("BlueMap-CLI-Timer", true);
        TimerTask updateInfoTask = new TimerTask() {
            @Override
            public void run() {
                RenderTask task = renderManager.getCurrentRenderTask();
                if (task == null) return;

                double progress = task.estimateProgress();
                long etaMs = renderManager.estimateCurrentRenderTaskTimeRemaining();

                String eta = "";
                if (etaMs > 0) {
                    Duration duration = Duration.of(etaMs, ChronoUnit.MILLIS);
                    eta = " (ETA: %s)".formatted(TextFormat.duration(duration));
                }
                Logger.global.logInfo(task.getDescription() + ": " + (Math.round(progress * 100000) / 1000.0) + "%" + eta);
            }
        };
        timer.scheduleAtFixedRate(updateInfoTask, TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(10));

        TimerTask saveTask = new TimerTask() {
            @Override
            public void run() {
                for (BmMap map : maps.values()) {
                    map.save();
                }
            }
        };
        timer.scheduleAtFixedRate(saveTask, TimeUnit.MINUTES.toMillis(2), TimeUnit.MINUTES.toMillis(2));

        Runnable shutdown = () -> {
            Logger.global.logInfo("Stopping...");
            api.unregister();

            updateInfoTask.cancel();
            saveTask.cancel();

            mapUpdateServices.forEach(MapUpdateService::close);
            mapUpdateServices.clear();

            renderManager.removeAllRenderTasks();
            try {
                renderManager.awaitIdle(true);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }

            renderManager.stop();
            try {
                renderManager.awaitShutdown();
            } catch (InterruptedException e) {
                Logger.global.logError("Unexpected interruption: ", e);
            }

            Logger.global.logInfo("Saving...");
            saveTask.run();

            Logger.global.logInfo("Stopped.");
        };

        Thread shutdownHook = new Thread(() -> {
            shutdownInProgress = true;
            shutdown.run();
        }, "BlueMap-CLI-ShutdownHook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        //metrics report
        if (blueMap.getConfig().getCoreConfig().isMetrics()) Metrics.sendReportAsync(
                "cli",
                blueMap.getOrLoadMinecraftVersion().getId()
        );

        // wait until done, then shutdown if not watching
        renderManager.awaitIdle();
        if (shutdownInProgress) return;

        Logger.global.logInfo("Your maps are now all up-to-date!");

        if (watch) {
            updateInfoTask.cancel();
            Logger.global.logInfo("Waiting for changes on the world-files...");
        } else {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
            shutdown.run();
        }
    }

    public void startWebserver(BlueMapService blueMap, boolean verbose) throws IOException, ConfigurationException, InterruptedException {
        Logger.global.logInfo("Starting webserver ...");

        WebserverConfig config = blueMap.getConfig().getWebserverConfig();
        FileHelper.createDirectories(config.getWebroot());

        RoutingRequestHandler routingRequestHandler = new RoutingRequestHandler();

        // default route
        routingRequestHandler.register(".*", new FileRequestHandler(config.getWebroot()));

        // map route
        for (var mapConfigEntry : blueMap.getConfig().getMapConfigs().entrySet()) {
            MapStorage storage = blueMap.getOrLoadStorage(mapConfigEntry.getValue().getStorage())
                    .map(mapConfigEntry.getKey());

            routingRequestHandler.register(
                    "maps/" + Pattern.quote(mapConfigEntry.getKey()) + "/(.*)",
                    "$1",
                    new MapRequestHandler(storage)
            );
        }

        List<Logger> webLoggerList = new ArrayList<>();
        if (verbose) webLoggerList.add(Logger.stdOut(true));
        if (config.getLog().getFile() != null) {
            ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
            webLoggerList.add(Logger.file(
                    Path.of(String.format(config.getLog().getFile(), zdt)),
                    config.getLog().isAppend()
            ));
        }

        HttpRequestHandler handler = new BlueMapResponseModifier(routingRequestHandler);
        handler = new LoggingRequestHandler(
                handler,
                config.getLog().getFormat(),
                Logger.combine(webLoggerList)
        );

        try {
            //noinspection resource
            HttpServer webServer = new HttpServer(handler);
            webServer.bind(new InetSocketAddress(
                    config.resolveIp(),
                    config.getPort()
            ));
            webServer.start();
        } catch (UnknownHostException ex) {
            throw new ConfigurationException("BlueMap failed to resolve the ip in your webserver-config.\n" +
                    "Check if that is correctly configured.", ex);
        } catch (BindException ex) {
            throw new ConfigurationException("BlueMap failed to bind to the configured address.\n" +
                    "This usually happens when the configured port (" + config.getPort() + ") is already in use by some other program.", ex);
        } catch (IOException ex) {
            throw new ConfigurationException("""
                    BlueMap failed to initialize the webserver.
                    Check your webserver-config if everything is configured correctly.
                    (Make sure you DON'T use the same port for bluemap that you also use for your minecraft server)""", ex);
        }
    }

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();

        BlueMapCLI cli = new BlueMapCLI();
        BlueMapService blueMap = null;

        try {
            CommandLine cmd = parser.parse(BlueMapCLI.createOptions(), args, false);

            if (cmd.hasOption("b")) {
                Logger.global.clear();
                Logger.global.put(Logger.stdOut(true));
            }

            if (cmd.hasOption("l")) {
                Logger.global.put(Logger.file(Path.of(cmd.getOptionValue("l")), cmd.hasOption("a")));
            }

            //help
            if (cmd.hasOption("h")) {
                BlueMapCLI.printHelp();
                return;
            }

            //version
            if (cmd.hasOption("V")) {
                BlueMapCLI.printVersion();
                return;
            }

            //config folder
            if (cmd.hasOption("c")) {
                cli.configFolder = Path.of(cmd.getOptionValue("c"));
                FileHelper.createDirectories(cli.configFolder);
            }

            //mods folder
            if (cmd.hasOption("n")) {
                cli.modsFolder = Path.of(cmd.getOptionValue("n"));
                if (!Files.isDirectory(cli.modsFolder)) {
                    throw new ConfigurationException("Mods folder does not exist: " + cli.modsFolder);
                }
            }

            //minecraft version
            if (cmd.hasOption("v")) {
                cli.minecraftVersion = cmd.getOptionValue("v");
            }

            // load addons
            Path packsFolder = cli.configFolder.resolve("packs");
            Files.createDirectories(packsFolder);
            Addons.tryLoadAddons(packsFolder);

            // load configs
            BlueMapConfigManager configs = BlueMapConfigManager.builder()
                    .minecraftVersion(cli.minecraftVersion)
                    .configRoot(cli.configFolder)
                    .modsFolder(cli.modsFolder)
                    .packsFolder(packsFolder)
                    .usePluginConfig(false)
                    .defaultDataFolder(Path.of("data"))
                    .defaultWebroot(Path.of("web"))
                    .build();

            //apply new file-logger config
            CoreConfig coreConfig = configs.getCoreConfig();
            if (coreConfig.getLog().getFile() != null) {
                ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
                Logger.global.put(Logger.file(
                        Path.of(String.format(coreConfig.getLog().getFile(), zdt)),
                        coreConfig.getLog().isAppend()
                ));
            }

            blueMap = new BlueMapService(configs);
            boolean noActions = true;

            if (cmd.hasOption("w")) {
                noActions = false;

                cli.startWebserver(blueMap, cmd.hasOption("b"));
                Thread.sleep(1000); //wait a second to let the webserver start, looks nicer in the log if anything comes after that
            }

            if (cmd.hasOption("r") || cmd.hasOption("f") || cmd.hasOption("u") || cmd.hasOption("e")) {
                noActions = false;

                boolean watch = cmd.hasOption("u");
                TileUpdateStrategy force = TileUpdateStrategy.FORCE_NONE;
                if (cmd.hasOption("f")) force = TileUpdateStrategy.FORCE_ALL;
                else if (cmd.hasOption("e")) force = TileUpdateStrategy.FORCE_EDGE;
                boolean generateWebappFiles = cmd.hasOption("g");
                String mapsToRender = cmd.getOptionValue("m", null);
                cli.renderMaps(blueMap, watch, force, generateWebappFiles, mapsToRender);
            } else {
                if (cmd.hasOption("g")) {
                    noActions = false;
                    blueMap.createOrUpdateWebApp(true);
                }
                if (cmd.hasOption("s")) {
                    noActions = false;
                    blueMap.createOrUpdateWebApp(false);
                }
            }

            // if nothing has been defined to do
            if (noActions) {
                Logger.global.logInfo("Generated default config files for you, here: " + cli.configFolder.toAbsolutePath().normalize() + "\n");

                //create resourcepacks folder
                FileHelper.createDirectories(cli.configFolder.resolve("packs"));

                //print help
                BlueMapCLI.printHelp();
                System.exit(1);
            }

        } catch (MissingResourcesException e) {
            Logger.global.logWarning("BlueMap is missing important resources!");
            Logger.global.logWarning("You must accept the required file download in order for BlueMap to work!");
            if (blueMap != null) {
                BlueMapConfiguration configProvider = blueMap.getConfig();
                if (configProvider instanceof BlueMapConfigManager) {
                    Logger.global.logWarning("Please check: " + ((BlueMapConfigManager) configProvider).getConfigManager().resolveConfigFile(BlueMapConfigManager.CORE_CONFIG_NAME).toAbsolutePath().normalize());
                }
            }
            System.exit(2);
        } catch (ParseException e) {
            Logger.global.logError("Failed to parse provided arguments!", e);
            BlueMapCLI.printHelp();
            System.exit(1);
        } catch (ConfigurationException e) {
            Logger.global.logWarning(e.getFormattedExplanation());
            Throwable cause = e.getRootCause();
            if (cause != null) {
                Logger.global.logError("Detailed error:", e);
            }
        } catch (IOException e) {
            Logger.global.logError("An IO-error occurred!", e);
            System.exit(1);
        } catch (InterruptedException ex) {
            System.exit(1);
        } catch (RuntimeException e) {
            Logger.global.logError("An unexpected error occurred!", e);
            System.exit(1);
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

        options.addOption(
            Option.builder("n")
                .longOpt("mods")
                .hasArg()
                .argName("mods-folder")
                .desc("Sets path of the folder containing the mods that contain extra resources for rendering.")
                .build()
        );

        options.addOption(
                Option.builder("v")
                .longOpt("mc-version")
                .hasArg()
                .argName("mc-version")
                .desc("Sets the minecraft-version, used e.g. to load resource-packs correctly. Defaults to the latest compatible version.")
                .build()
            );

        options.addOption(
                Option.builder("l")
                .longOpt("log-file")
                .hasArg()
                .argName("file-name")
                .desc("Sets a file to save the log to. If not specified, no log will be saved.")
                .build()
            );
        options.addOption("a", "append", false, "Causes log save file to be appended rather than replaced.");

        options.addOption("w", "webserver", false, "Starts the web-server, configured in the 'webserver.conf' file");
        options.addOption("b", "verbose", false, "Causes the web-server to log requests to the console");

        options.addOption("g", "generate-webapp", false, "Generates the files for the web-app to the folder configured in the 'webapp.conf' file");
        options.addOption("s", "generate-websettings", false, "Updates the settings.json for the web-app");

        options.addOption("r", "render", false, "Renders the maps configured in the 'render.conf' file");
        options.addOption("e", "fix-edges", false, "Forces rendering the map-edges, instead of only rendering chunks that have been modified since the last render");
        options.addOption("f", "force-render", false, "Forces rendering everything, instead of only rendering chunks that have been modified since the last render");
        options.addOption("m", "maps", true, "A comma-separated list of map-id's that should be rendered. Example: 'world,nether'");

        options.addOption("u", "watch", false, "Watches for file-changes after rendering and updates the map");

        options.addOption("V", "version", false, "Print the current BlueMap version");

        return options;
    }

    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();

        String command = getCliCommand();

        @SuppressWarnings("StringBufferReplaceableByString")
        StringBuilder footer = new StringBuilder();
        footer.append("Examples:\n\n");
        footer.append(command).append(" -c './config/'\n");
        footer.append("Generates the default/example configurations in a folder named 'config' if they are not already present\n\n");
        footer.append(command).append(" -r\n");
        footer.append("Render the configured maps\n\n");
        footer.append(command).append(" -w\n");
        footer.append("Start only the webserver without doing anything else\n\n");
        footer.append(command).append(" -ru\n");
        footer.append("Render the configured maps and then keeps watching the world-files and updates the map once something changed.\n\n");

        formatter.printHelp(command + " [options]", "\nOptions:", createOptions(), "\n" + footer);
    }

    private static String getCliCommand() {
        String filename = "bluemap-cli.jar";
        try {
            Path file = Path.of(BlueMapCLI.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            if (Files.isRegularFile(file)) {
                try {
                    filename = "." + file.getFileSystem().getSeparator() +
                            Path.of("").toRealPath().relativize(file.toRealPath());
                } catch (IllegalArgumentException ex) {
                    filename = file.toAbsolutePath().toString();
                }
            }
        } catch (Exception ignore) {}
        return "java -jar " + filename;
    }

    private static void printVersion() {
        System.out.printf("%s\n%s\n", BlueMap.VERSION, BlueMap.GIT_HASH);
    }
}
