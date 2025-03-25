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

import de.bluecolored.bluemap.common.BlueMapConfiguration;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.InterruptableReentrantLock;
import de.bluecolored.bluemap.common.MissingResourcesException;
import de.bluecolored.bluemap.common.addons.Addons;
import de.bluecolored.bluemap.common.api.BlueMapAPIImpl;
import de.bluecolored.bluemap.common.config.*;
import de.bluecolored.bluemap.common.debug.StateDumper;
import de.bluecolored.bluemap.common.live.LivePlayersDataSupplier;
import de.bluecolored.bluemap.common.plugin.skins.PlayerSkinUpdater;
import de.bluecolored.bluemap.common.rendermanager.MapUpdatePreparationTask;
import de.bluecolored.bluemap.common.rendermanager.RenderManager;
import de.bluecolored.bluemap.common.rendermanager.RenderTask;
import de.bluecolored.bluemap.common.serverinterface.Server;
import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.common.web.*;
import de.bluecolored.bluemap.common.web.http.HttpServer;
import de.bluecolored.bluemap.common.metrics.Metrics;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.resources.MinecraftVersion;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.util.FileHelper;
import de.bluecolored.bluemap.core.util.Tristate;
import de.bluecolored.bluemap.core.world.World;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Getter
public class Plugin implements ServerEventListener {

    public static final String PLUGIN_ID = "bluemap";
    public static final String PLUGIN_NAME = "BlueMap";

    private static final String DEBUG_FILE_LOG_NAME = "file-debug-log";

    @Getter(AccessLevel.NONE)
    private final InterruptableReentrantLock loadingLock = new InterruptableReentrantLock();

    private final String implementationType;
    private final Server serverInterface;

    private BlueMapService blueMap;
    private PluginState pluginState;
    private RenderManager renderManager;
    private BlueMapAPIImpl api;

    private HttpServer webServer;
    private RoutingRequestHandler webRequestHandler;
    private Logger webLogger;

    private Timer daemonTimer;
    private Map<String, MapUpdateService> mapUpdateServices;
    private PlayerSkinUpdater skinUpdater;

    private boolean loaded = false;

    public Plugin(String implementationType, Server serverInterface) {
        this.implementationType = implementationType.toLowerCase();
        this.serverInterface = serverInterface;

        StateDumper.global().register(this);
    }

    public void load() throws IOException {
        load(null);
    }

    private void load(@Nullable ResourcePack preloadedResourcePack) throws IOException {
        loadingLock.lock();
        try {
            synchronized (this) {

                if (loaded) return;
                unload(); //ensure nothing is left running (from a failed load or something)

                //load addons
                Path packsFolder = serverInterface.getConfigFolder().resolve("packs");
                Files.createDirectories(packsFolder);
                Addons.tryLoadAddons(packsFolder);

                //load configs
                BlueMapConfigManager configManager = BlueMapConfigManager.builder()
                        .minecraftVersion(serverInterface.getMinecraftVersion())
                        .configRoot(serverInterface.getConfigFolder())
                        .packsFolder(packsFolder)
                        .modsFolder(serverInterface.getModsFolder().orElse(null))
                        .useMetricsConfig(serverInterface.isMetricsEnabled() == Tristate.UNDEFINED)
                        .autoConfigWorlds(serverInterface.getLoadedServerWorlds())
                        .build();
                CoreConfig coreConfig = configManager.getCoreConfig();
                WebserverConfig webserverConfig = configManager.getWebserverConfig();
                WebappConfig webappConfig = configManager.getWebappConfig();
                PluginConfig pluginConfig = configManager.getPluginConfig();

                //apply new file-logger config
                if (coreConfig.getLog().getFile() != null) {
                    ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
                    Logger.global.put(DEBUG_FILE_LOG_NAME, () -> Logger.file(
                            Path.of(String.format(coreConfig.getLog().getFile(), zdt)),
                            coreConfig.getLog().isAppend()
                    ));
                } else {
                    Logger.global.remove(DEBUG_FILE_LOG_NAME);
                }

                //load plugin state
                try {
                    GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                            .path(coreConfig.getData().resolve("pluginState.json"))
                            .build();
                    pluginState = loader.load().get(PluginState.class);
                } catch (SerializationException ex) {
                    Logger.global.logWarning("Failed to load pluginState.json (invalid format), creating a new one...");
                    pluginState = new PluginState();
                }

                //create bluemap-service
                blueMap = new BlueMapService(configManager, preloadedResourcePack);

                //try load resources
                try {
                    blueMap.getOrLoadResourcePack();
                } catch (MissingResourcesException ex) {
                    Logger.global.logWarning("BlueMap is missing important resources!");
                    Logger.global.logWarning("You must accept the required file download in order for BlueMap to work!");

                    BlueMapConfiguration configProvider = blueMap.getConfig();
                    if (configProvider instanceof BlueMapConfigManager) {
                        Logger.global.logWarning("Please check: " + ((BlueMapConfigManager) configProvider).getConfigManager().resolveConfigFile(BlueMapConfigManager.CORE_CONFIG_NAME).toAbsolutePath().normalize());
                    }

                    Logger.global.logInfo("If you have changed the config you can simply reload the plugin using: /bluemap reload");

                    unload();
                    return;
                }

                //load maps
                Map<String, BmMap> maps = blueMap.getOrLoadMaps();

                //create and start webserver
                if (webserverConfig.isEnabled()) {
                    Path webroot = webserverConfig.getWebroot();
                    FileHelper.createDirectories(webroot);

                    this.webRequestHandler = new RoutingRequestHandler();

                    // default route
                    webRequestHandler.register(".*", new FileRequestHandler(webroot));

                    // map route
                    for (var mapConfigEntry : configManager.getMapConfigs().entrySet()) {
                        String id = mapConfigEntry.getKey();
                        MapConfig mapConfig = mapConfigEntry.getValue();

                        MapRequestHandler mapRequestHandler;
                        BmMap map = maps.get(id);
                        if (map != null) {
                            mapRequestHandler = new MapRequestHandler(map, serverInterface, pluginConfig, Predicate.not(pluginState::isPlayerHidden));
                        } else {
                            Storage storage = blueMap.getOrLoadStorage(mapConfig.getStorage());
                            mapRequestHandler = new MapRequestHandler(storage.map(id));
                        }

                        webRequestHandler.register(
                                "maps/" + Pattern.quote(id) + "/(.*)",
                                "$1",
                                new BlueMapResponseModifier(mapRequestHandler)
                        );
                    }

                    // create web-logger
                    List<Logger> webLoggerList = new ArrayList<>();
                    if (webserverConfig.getLog().getFile() != null) {
                        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault());
                        webLoggerList.add(Logger.file(
                                Path.of(String.format(webserverConfig.getLog().getFile(), zdt)),
                                webserverConfig.getLog().isAppend()
                        ));
                    }
                    webLogger = Logger.combine(webLoggerList);

                    try {
                        webServer = new HttpServer(new LoggingRequestHandler(
                                webRequestHandler,
                                webserverConfig.getLog().getFormat(),
                                webLogger
                        ));
                        webServer.bind(new InetSocketAddress(
                                webserverConfig.resolveIp(),
                                webserverConfig.getPort()
                        ));
                        webServer.start();
                    } catch (UnknownHostException ex) {
                        throw new ConfigurationException("BlueMap failed to resolve the ip in your webserver-config.\n" +
                                "Check if that is correctly configured.", ex);
                    } catch (BindException ex) {
                        throw new ConfigurationException("BlueMap failed to bind to the configured address.\n" +
                                "This usually happens when the configured port (" + webserverConfig.getPort() + ") is already in use by some other program.", ex);
                    } catch (IOException ex) {
                        throw new ConfigurationException("""
                                BlueMap failed to initialize the webserver.
                                Check your webserver-config if everything is configured correctly.
                                (Make sure you DON'T use the same port for bluemap that you also use for your minecraft server)
                                """.strip(), ex);
                    }
                }

                //warn if no maps are configured
                if (maps.isEmpty()) {
                    Logger.global.logWarning("There are no valid maps configured, please check your map-configs! Disabling BlueMap...");
                    unload(true);
                    return;
                }

                //initialize render manager
                renderManager = new RenderManager();

                //update webapp and settings
                if (webappConfig.isEnabled())
                    blueMap.createOrUpdateWebApp(false);

                //start skin updater
                this.skinUpdater = new PlayerSkinUpdater(this);
                if (pluginConfig.isLivePlayerMarkers()) {
                    serverInterface.registerListener(skinUpdater);
                }

                //init timer
                daemonTimer = new Timer("BlueMap-Plugin-DaemonTimer", true);

                //periodically save
                TimerTask saveTask = new TimerTask() {
                    @Override
                    public void run() {
                        save();
                    }
                };
                daemonTimer.schedule(saveTask, TimeUnit.MINUTES.toMillis(10), TimeUnit.MINUTES.toMillis(10));

                //periodically save markers
                int writeMarkersInterval = pluginConfig.getWriteMarkersInterval();
                if (writeMarkersInterval > 0) {
                    TimerTask saveMarkersTask = new TimerTask() {
                        @Override
                        public void run() {
                            saveMarkerStates();
                        }
                    };
                    daemonTimer.schedule(saveMarkersTask, TimeUnit.SECONDS.toMillis(writeMarkersInterval), TimeUnit.SECONDS.toMillis(writeMarkersInterval));
                }

                //periodically save players
                int writePlayersInterval = pluginConfig.getWritePlayersInterval();
                if (writePlayersInterval > 0) {
                    TimerTask savePlayersTask = new TimerTask() {
                        @Override
                        public void run() {
                            savePlayerStates();
                        }
                    };
                    daemonTimer.schedule(savePlayersTask, TimeUnit.SECONDS.toMillis(writePlayersInterval), TimeUnit.SECONDS.toMillis(writePlayersInterval));
                }

                //periodically restart the file-watchers
                TimerTask fileWatcherRestartTask = new TimerTask() {
                    @Override
                    public void run() {
                        mapUpdateServices.values().forEach(MapUpdateService::close);
                        mapUpdateServices.clear();
                        initFileWatcherTasks();
                    }
                };
                daemonTimer.schedule(fileWatcherRestartTask, TimeUnit.HOURS.toMillis(1), TimeUnit.HOURS.toMillis(1));

                //periodically update all (non frozen) maps
                if (pluginConfig.getFullUpdateInterval() > 0) {
                    long fullUpdateTime = TimeUnit.MINUTES.toMillis(pluginConfig.getFullUpdateInterval());
                    TimerTask updateAllMapsTask = new TimerTask() {
                        @Override
                        public void run() {
                            renderManager.scheduleRenderTasksNext(maps.values().stream()
                                    .filter(map -> pluginState.getMapState(map).isUpdateEnabled())
                                    .sorted(Comparator.comparing(bmMap -> bmMap.getMapSettings().getSorting()))
                                    .map(map -> MapUpdatePreparationTask.updateMap(map, renderManager))
                                    .toArray(RenderTask[]::new));
                        }
                    };
                    daemonTimer.scheduleAtFixedRate(updateAllMapsTask, 0, fullUpdateTime);
                }

                //metrics
                MinecraftVersion minecraftVersion = blueMap.getOrLoadMinecraftVersion();
                TimerTask metricsTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (serverInterface.isMetricsEnabled().getOr(coreConfig::isMetrics))
                            Metrics.sendReport(implementationType, minecraftVersion.getId());
                    }
                };
                daemonTimer.scheduleAtFixedRate(metricsTask, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(30));

                //watch map-changes
                this.mapUpdateServices = new HashMap<>();
                initFileWatcherTasks();

                //register listener
                serverInterface.registerListener(this);

                //enable api
                this.api = new BlueMapAPIImpl(this);
                this.api.register();

                //start render-manager
                if (pluginState.isRenderThreadsEnabled()) {
                    checkPausedByPlayerCount(); // <- this also starts the render-manager if it should start
                } else {
                    Logger.global.logInfo("Render-Threads are STOPPED! Use the command 'bluemap start' to start them.");
                }

                //done
                loaded = true;
            }
        } catch (ConfigurationException ex) {
            Logger.global.logWarning(ex.getFormattedExplanation());
            throw new IOException(ex);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.global.logWarning("Loading has been interrupted!");
        } finally {
            loadingLock.unlock();
        }
    }

    public void unload() {
        this.unload(false);
    }

    public void unload(boolean keepWebserver) {
        loadingLock.interruptAndLock();
        try {
            synchronized (this) {

                //disable api
                if (api != null) api.unregister();
                api = null;

                //unregister listeners
                serverInterface.unregisterAllListeners();
                skinUpdater = null;

                //stop scheduled threads
                if (daemonTimer != null) daemonTimer.cancel();
                daemonTimer = null;

                //stop file-watchers
                if (mapUpdateServices != null) {
                    mapUpdateServices.values().forEach(MapUpdateService::close);
                    mapUpdateServices.clear();
                }
                mapUpdateServices = null;

                // stop render-manager
                if (renderManager != null){
                    if (renderManager.getCurrentRenderTask() != null) {
                        renderManager.removeAllRenderTasks();
                        if (!renderManager.isRunning()) renderManager.start(1);
                        try {
                            renderManager.awaitIdle(true);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }

                    renderManager.stop();
                    try {
                        renderManager.awaitShutdown();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }

                //save
                save();

                // stop webserver
                if (webServer != null && !keepWebserver) {
                    try {
                        webServer.close();
                    } catch (IOException ex) {
                        Logger.global.logError("Failed to close the webserver!", ex);
                    }
                    webServer = null;
                }

                if (webLogger != null && !keepWebserver) {
                    try {
                        webLogger.close();
                    } catch (Exception ex) {
                        Logger.global.logError("Failed to close the webserver-logger!", ex);
                    }
                    webLogger = null;
                }

                //close bluemap
                if (blueMap != null) {
                    try {
                        blueMap.close();
                    } catch (IOException ex) {
                        Logger.global.logError("Failed to close a bluemap-service!", ex);
                    }
                }
                blueMap = null;

                // remove file-logger
                Logger.global.remove(DEBUG_FILE_LOG_NAME);

                //clear resources
                pluginState = null;

                //done
                loaded = false;
            }
        } finally {
            loadingLock.unlock();
        }
    }

    public void reload() throws IOException {
        unload();
        load();
    }

    /**
     * {@link #reload()} but without reloading the resourcepack (if it is loaded).
     */
    public void lightReload() throws IOException {
        loadingLock.lock();
        try {
            synchronized (this) {

                if (!loaded) {
                    reload(); // reload normally
                    return;
                }

                // hold and reuse loaded resourcepack
                ResourcePack preloadedResourcePack = this.blueMap.getResourcePack();

                unload();
                load(preloadedResourcePack);

            }
        } finally {
            loadingLock.unlock();
        }
    }

    public synchronized void save() {
        if (blueMap == null) return;

        if (pluginState != null) {
            try {
                GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                        .path(blueMap.getConfig().getCoreConfig().getData().resolve("pluginState.json"))
                        .build();
                loader.save(loader.createNode().set(PluginState.class, pluginState));
            } catch (IOException ex) {
                Logger.global.logError("Failed to save pluginState.json!", ex);
            }
        }

        var maps = blueMap.getMaps();
        for (BmMap map : maps.values()) {
            map.save();
        }
    }

    public void saveMarkerStates() {
        if (blueMap == null) return;

        var maps = blueMap.getMaps();
        for (BmMap map : maps.values()) {
            map.saveMarkerState();
        }
    }

    public void savePlayerStates() {
        if (blueMap == null) return;

        var maps = blueMap.getMaps();
        for (BmMap map : maps.values()) {
            var serverWorld = serverInterface.getServerWorld(map.getWorld()).orElse(null);
            if (serverWorld == null) continue;
            var dataSupplier = new LivePlayersDataSupplier(
                    serverInterface,
                    getBlueMap().getConfig().getPluginConfig(),
                    serverWorld,
                    Predicate.not(pluginState::isPlayerHidden)
            );
            try (
                    OutputStream out = map.getStorage().players().write();
                    Writer writer = new OutputStreamWriter(out)
            ) {
                writer.write(dataSupplier.get());
            } catch (Exception ex) {
                Logger.global.logError("Failed to save players for map '" + map.getId() + "'!", ex);
            }
        }
    }

    public synchronized void startWatchingMap(BmMap map) {
        stopWatchingMap(map);

        try {
            MapUpdateService watcher = new MapUpdateService(renderManager, map);
            watcher.start();
            mapUpdateServices.put(map.getId(), watcher);
        } catch (IOException ex) {
            Logger.global.logError("Failed to create update-watcher for map: " + map.getId() +
                    " (This means the map might not automatically update)", ex);
        } catch (UnsupportedOperationException ex) {
            Logger.global.logWarning("Update-watcher for map '" + map.getId() + "' is not supported for the world-type." +
                    " (This means the map might not automatically update)");
        }
    }

    public synchronized void stopWatchingMap(BmMap map) {
        MapUpdateService watcher = mapUpdateServices.remove(map.getId());
        if (watcher != null) {
            watcher.close();
        }
    }

    public boolean flushWorldUpdates(World world) throws IOException {
        var implWorld = serverInterface.getServerWorld(world).orElse(null);
        if (implWorld != null) return implWorld.persistWorldChanges();
        return false;
    }

    @Override
    public void onPlayerJoin(UUID playerUuid) {
        checkPausedByPlayerCountSoon();
    }

    @Override
    public void onPlayerLeave(UUID playerUuid) {
        checkPausedByPlayerCountSoon();
    }

    private void checkPausedByPlayerCountSoon() {
        // check is done a second later to make sure the player has actually joined/left and is no longer on the list
        try {
            daemonTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    checkPausedByPlayerCount();
                }
            }, 1000);
        } catch (IllegalStateException ex) { // Timer is cancelled for some reason
            Logger.global.logWarning("Timer is already cancelled, skipping player-limit checks!");
        }
    }

    public boolean checkPausedByPlayerCount() {
        CoreConfig coreConfig = getBlueMap().getConfig().getCoreConfig();
        PluginConfig pluginConfig = getBlueMap().getConfig().getPluginConfig();

        if (
                pluginConfig.getPlayerRenderLimit() > 0 &&
                getServerInterface().getOnlinePlayers().size() >= pluginConfig.getPlayerRenderLimit()
        ) {
            if (renderManager.isRunning()) renderManager.stop();
            return true;
        } else {
            if (!renderManager.isRunning() && getPluginState().isRenderThreadsEnabled())
                renderManager.start(coreConfig.resolveRenderThreadCount());
            return false;
        }
    }

    public @Nullable World getWorld(ServerWorld serverWorld) {
        String id = World.id(serverWorld.getWorldFolder(), serverWorld.getDimension());
        return getBlueMap().getWorlds().get(id);
    }

    private void initFileWatcherTasks() {
        var maps = blueMap.getMaps();
        if (maps != null) {
            for (BmMap map : maps.values()) {
                if (pluginState.getMapState(map).isUpdateEnabled()) {
                    startWatchingMap(map);
                }
            }
        }
    }

    public boolean isLoading() {
        return loadingLock.isLocked();
    }

}
