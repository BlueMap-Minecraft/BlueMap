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

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.common.BlueMapConfigProvider;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.InterruptableReentrantLock;
import de.bluecolored.bluemap.common.MissingResourcesException;
import de.bluecolored.bluemap.common.api.BlueMapAPIImpl;
import de.bluecolored.bluemap.common.config.*;
import de.bluecolored.bluemap.common.live.LivePlayersDataSupplier;
import de.bluecolored.bluemap.common.plugin.skins.PlayerSkinUpdater;
import de.bluecolored.bluemap.common.rendermanager.MapUpdateTask;
import de.bluecolored.bluemap.common.rendermanager.RenderManager;
import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.serverinterface.ServerInterface;
import de.bluecolored.bluemap.common.web.*;
import de.bluecolored.bluemap.common.web.http.HttpServer;
import de.bluecolored.bluemap.core.debug.StateDumper;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.metrics.Metrics;
import de.bluecolored.bluemap.core.resources.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.util.FileHelper;
import de.bluecolored.bluemap.core.world.World;
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
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@DebugDump
public class Plugin implements ServerEventListener {

    public static final String PLUGIN_ID = "bluemap";
    public static final String PLUGIN_NAME = "BlueMap";

    private static final String DEBUG_FILE_LOG_NAME = "file-debug-log";

    private final InterruptableReentrantLock loadingLock = new InterruptableReentrantLock();

    private final String implementationType;
    private final ServerInterface serverInterface;

    private BlueMapService blueMap;

    private PluginState pluginState;

    private Map<String, World> worlds;
    private Map<String, BmMap> maps;

    private RenderManager renderManager;
    private HttpServer webServer;
    private Logger webLogger;

    private BlueMapAPIImpl api;

    private Timer daemonTimer;

    private Map<String, RegionFileWatchService> regionFileWatchServices;

    private PlayerSkinUpdater skinUpdater;

    private boolean loaded = false;

    public Plugin(String implementationType, ServerInterface serverInterface) {
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

                //load configs
                blueMap = new BlueMapService(serverInterface, new BlueMapConfigs(serverInterface), preloadedResourcePack);
                CoreConfig coreConfig = getConfigs().getCoreConfig();
                WebserverConfig webserverConfig = getConfigs().getWebserverConfig();
                WebappConfig webappConfig = getConfigs().getWebappConfig();
                PluginConfig pluginConfig = getConfigs().getPluginConfig();

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

                //try load resources
                try {
                    blueMap.getResourcePack();
                } catch (MissingResourcesException ex) {
                    Logger.global.logWarning("BlueMap is missing important resources!");
                    Logger.global.logWarning("You must accept the required file download in order for BlueMap to work!");

                    BlueMapConfigProvider configProvider = blueMap.getConfigs();
                    if (configProvider instanceof BlueMapConfigs) {
                        Logger.global.logWarning("Please check: " + ((BlueMapConfigs) configProvider).getConfigManager().findConfigPath(Path.of("core")).toAbsolutePath().normalize());
                    }

                    Logger.global.logInfo("If you have changed the config you can simply reload the plugin using: /bluemap reload");

                    unload();
                    return;
                }

                //load worlds and maps
                worlds = blueMap.getWorlds();
                maps = blueMap.getMaps();

                //create and start webserver
                if (webserverConfig.isEnabled()) {
                    Path webroot = webserverConfig.getWebroot();
                    FileHelper.createDirectories(webroot);

                    RoutingRequestHandler routingRequestHandler = new RoutingRequestHandler();

                    // default route
                    routingRequestHandler.register(".*", new FileRequestHandler(webroot));

                    // map route
                    for (var mapConfigEntry : getConfigs().getMapConfigs().entrySet()) {
                        String id = mapConfigEntry.getKey();
                        MapConfig mapConfig = mapConfigEntry.getValue();

                        MapRequestHandler mapRequestHandler;
                        BmMap map = maps.get(id);
                        if (map != null) {
                            mapRequestHandler = new MapRequestHandler(map, serverInterface, pluginConfig, Predicate.not(pluginState::isPlayerHidden));
                        } else {
                            Storage storage = blueMap.getStorage(mapConfig.getStorage());
                            mapRequestHandler = new MapRequestHandler(id, storage);
                        }

                        routingRequestHandler.register(
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
                                routingRequestHandler,
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
                        throw new ConfigurationException("BlueMap failed to initialize the webserver.\n" +
                                "Check your webserver-config if everything is configured correctly.\n" +
                                "(Make sure you DON'T use the same port for bluemap that you also use for your minecraft server)", ex);
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

                //update all maps
                maps.values().stream()
                        .sorted(Comparator.comparing(bmMap -> bmMap.getMapSettings().getSorting()))
                        .forEach(map -> {
                    if (pluginState.getMapState(map).isUpdateEnabled()) {
                        renderManager.scheduleRenderTask(new MapUpdateTask(map));
                    }
                });

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
                daemonTimer.schedule(saveTask, TimeUnit.MINUTES.toMillis(2), TimeUnit.MINUTES.toMillis(2));

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
                        regionFileWatchServices.values().forEach(RegionFileWatchService::close);
                        regionFileWatchServices.clear();
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
                            for (BmMap map : maps.values()) {
                                if (pluginState.getMapState(map).isUpdateEnabled()) {
                                    renderManager.scheduleRenderTask(new MapUpdateTask(map));
                                }
                            }
                        }
                    };
                    daemonTimer.scheduleAtFixedRate(updateAllMapsTask, fullUpdateTime, fullUpdateTime);
                }

                //metrics
                TimerTask metricsTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (Plugin.this.serverInterface.isMetricsEnabled().getOr(coreConfig::isMetrics))
                            Metrics.sendReport(Plugin.this.implementationType);
                    }
                };
                daemonTimer.scheduleAtFixedRate(metricsTask, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(30));

                //watch map-changes
                this.regionFileWatchServices = new HashMap<>();
                initFileWatcherTasks();

                //register listener
                serverInterface.registerListener(this);

                //enable api
                this.api = new BlueMapAPIImpl(this);
                this.api.register();

                //save webapp settings again (for api-registered scripts and styles)
                if (webappConfig.isEnabled())
                    this.getBlueMap().getWebFilesManager().saveSettings();

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
                //save
                save();

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
                if (regionFileWatchServices != null) {
                    regionFileWatchServices.values().forEach(RegionFileWatchService::close);
                    regionFileWatchServices.clear();
                }
                regionFileWatchServices = null;

                //stop services
                if (renderManager != null){
                    renderManager.stop();
                    try {
                        renderManager.awaitShutdown();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
                renderManager = null;

                if (webServer != null && !keepWebserver) {
                    try {
                        webServer.close();
                    } catch (IOException ex) {
                        Logger.global.logError("Failed to close the webserver!", ex);
                    }
                    webServer = null;
                }

                if (webLogger != null) {
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
                worlds = null;
                maps = null;

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
                ResourcePack preloadedResourcePack = this.blueMap.getResourcePackIfLoaded().orElse(null);

                unload();
                load(preloadedResourcePack);

            }
        } finally {
            loadingLock.unlock();
        }
    }

    public synchronized void save() {
        if (pluginState != null) {
            try {
                GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                        .path(blueMap.getConfigs().getCoreConfig().getData().resolve("pluginState.json"))
                        .build();
                loader.save(loader.createNode().set(PluginState.class, pluginState));
            } catch (IOException ex) {
                Logger.global.logError("Failed to save pluginState.json!", ex);
            }
        }

        if (maps != null) {
            for (BmMap map : maps.values()) {
                map.save();
            }
        }
    }

    public void saveMarkerStates() {
        if (maps != null) {
            for (BmMap map : maps.values()) {
                map.saveMarkerState();
            }
        }
    }

    public void savePlayerStates() {
        if (maps != null) {
            for (BmMap map : maps.values()) {
                var dataSupplier = new LivePlayersDataSupplier(
                        serverInterface,
                        getConfigs().getPluginConfig(),
                        map.getWorldId(),
                        Predicate.not(pluginState::isPlayerHidden)
                );
                try (
                        OutputStream out = map.getStorage().writeMeta(map.getId(), BmMap.META_FILE_PLAYERS);
                        Writer writer = new OutputStreamWriter(out)
                ) {
                    writer.write(dataSupplier.get());
                } catch (Exception ex) {
                    Logger.global.logError("Failed to save players for map '" + map.getId() + "'!", ex);
                }
            }
        }
    }

    public synchronized void startWatchingMap(BmMap map) {
        stopWatchingMap(map);

        try {
            RegionFileWatchService watcher = new RegionFileWatchService(renderManager, map, false);
            watcher.start();
            regionFileWatchServices.put(map.getId(), watcher);
        } catch (IOException ex) {
            Logger.global.logError("Failed to create file-watcher for map: " + map.getId() + " (This means the map might not automatically update)", ex);
        }
    }

    public synchronized void stopWatchingMap(BmMap map) {
        RegionFileWatchService watcher = regionFileWatchServices.remove(map.getId());
        if (watcher != null) {
            watcher.close();
        }
    }

    public boolean flushWorldUpdates(World world) throws IOException {
        var implWorld = serverInterface.getWorld(world.getSaveFolder()).orElse(null);
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
        CoreConfig coreConfig = getConfigs().getCoreConfig();
        PluginConfig pluginConfig = getConfigs().getPluginConfig();

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

    public ServerInterface getServerInterface() {
        return serverInterface;
    }

    public BlueMapService getBlueMap() {
        return blueMap;
    }

    public BlueMapConfigProvider getConfigs() {
        return blueMap.getConfigs();
    }

    public PluginState getPluginState() {
        return pluginState;
    }

    public Map<String, World> getWorlds(){
        return worlds;
    }

    public Map<String, BmMap> getMaps(){
        return maps;
    }

    public RenderManager getRenderManager() {
        return renderManager;
    }

    public HttpServer getWebServer() {
        return webServer;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public String getImplementationType() {
        return implementationType;
    }

    public PlayerSkinUpdater getSkinUpdater() {
        return skinUpdater;
    }

    private void initFileWatcherTasks() {
        for (BmMap map : maps.values()) {
            if (pluginState.getMapState(map).isUpdateEnabled()) {
                startWatchingMap(map);
            }
        }
    }

}
