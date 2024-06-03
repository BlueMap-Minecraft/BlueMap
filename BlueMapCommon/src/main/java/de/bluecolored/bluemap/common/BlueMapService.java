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

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.common.config.MapConfig;
import de.bluecolored.bluemap.common.config.storage.StorageConfig;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.debug.StateDumper;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.resources.MinecraftVersion;
import de.bluecolored.bluemap.core.resources.VersionManifest;
import de.bluecolored.bluemap.core.resources.pack.datapack.DataPack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.util.FileHelper;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.mca.MCAWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.loader.HeaderMode;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * This is the attempt to generalize as many actions as possible to have CLI and Plugins run on the same general setup-code.
 */
public class BlueMapService implements Closeable {

    private final BlueMapConfiguration config;
    private final WebFilesManager webFilesManager;

    private MinecraftVersion minecraftVersion;
    private ResourcePack resourcePack;
    private final Map<String, World> worlds;
    private final Map<String, BmMap> maps;
    private final Map<String, Storage> storages;


    public BlueMapService(BlueMapConfiguration configuration, @Nullable ResourcePack preloadedResourcePack) {
        this(configuration);
        this.resourcePack = preloadedResourcePack;
    }

    public BlueMapService(BlueMapConfiguration configuration) {
        this.config = configuration;
        this.webFilesManager = new WebFilesManager(config.getWebappConfig().getWebroot());

        this.worlds = new ConcurrentHashMap<>();
        this.maps = new ConcurrentHashMap<>();
        this.storages = new ConcurrentHashMap<>();

        StateDumper.global().register(this);
    }

    public WebFilesManager getWebFilesManager() {
        return webFilesManager;
    }

    public synchronized void createOrUpdateWebApp(boolean force) throws ConfigurationException {
        try {
            WebFilesManager webFilesManager = getWebFilesManager();

            // update web-app files
            if (force || webFilesManager.filesNeedUpdate()) {
                webFilesManager.updateFiles();
            }

            // update settings.json
            if (!config.getWebappConfig().isUpdateSettingsFile()) {
                webFilesManager.loadSettings();
                webFilesManager.addFrom(config.getWebappConfig());
            } else {
                webFilesManager.setFrom(config.getWebappConfig());
            }
            for (String mapId : config.getMapConfigs().keySet()) {
                webFilesManager.addMap(mapId);
            }
            webFilesManager.saveSettings();

        } catch (IOException ex) {
            throw new ConfigurationException("Failed to update web-app files!", ex);
        }
    }

    /**
     * Gets all loaded maps.
     * @return A map of loaded maps
     */
    public Map<String, BmMap> getMaps() {
        return Collections.unmodifiableMap(maps);
    }

    /**
     * Gets all loaded worlds.
     * @return A map of loaded worlds
     */
    public Map<String, World> getWorlds() {
        return Collections.unmodifiableMap(worlds);
    }

    /**
     * Gets or loads configured maps.
     * @return A map of loaded maps
     */
    public Map<String, BmMap> getOrLoadMaps() throws InterruptedException {
        return getOrLoadMaps(mapId -> true);
    }

    /**
     * Gets or loads configured maps.
     * @param filter A predicate filtering map-ids that should be loaded
     *               (if maps are already loaded, they will be returned as well)
     * @return A map of all loaded maps
     */
    public synchronized Map<String, BmMap> getOrLoadMaps(Predicate<String> filter) throws InterruptedException {
        for (var entry : config.getMapConfigs().entrySet()) {
            if (Thread.interrupted()) throw new InterruptedException();

            if (!filter.test(entry.getKey())) continue;
            if (maps.containsKey(entry.getKey())) continue;

            try {
                loadMap(entry.getKey(), entry.getValue());
            } catch (ConfigurationException ex) {
                Logger.global.logWarning(ex.getFormattedExplanation());
                Throwable cause = ex.getRootCause();
                if (cause != null) {
                    Logger.global.logError("Detailed error:", ex);
                }
            }
        }
        return Collections.unmodifiableMap(maps);
    }

    private synchronized void loadMap(String id, MapConfig mapConfig) throws ConfigurationException, InterruptedException {
        String name = mapConfig.getName();
        if (name == null) name = id;

        Path worldFolder = mapConfig.getWorld();
        Key dimension = mapConfig.getDimension();

        // if there is no world configured, we assume the map is static, or supplied from a different server
        if (worldFolder == null) {
            Logger.global.logInfo("The map '" + name + "' has no world configured. The map will be displayed, but not updated!");
            return;
        }

        // if there is no dimension configured, we assume world-folder is actually the dimension-folder and convert (backwards compatibility)
        if (dimension == null) {
            worldFolder = worldFolder.normalize();
            if (worldFolder.endsWith("DIM-1")) {
                worldFolder = worldFolder.getParent();
                dimension = DataPack.DIMENSION_THE_NETHER;
            } else if (worldFolder.endsWith("DIM1")) {
                worldFolder = worldFolder.getParent();
                dimension = DataPack.DIMENSION_THE_END;
            } else if (
                    worldFolder.getNameCount() > 3 &&
                    worldFolder.getName(worldFolder.getNameCount() - 3).toString().equals("dimensions")
            ) {
                String namespace = worldFolder.getName(worldFolder.getNameCount() - 2).toString();
                String value = worldFolder.getName(worldFolder.getNameCount() - 1).toString();
                worldFolder = worldFolder.subpath(0, worldFolder.getNameCount() - 3);
                dimension = new Key(namespace, value);
            } else {
                dimension = DataPack.DIMENSION_OVERWORLD;
            }

            Logger.global.logInfo("The map '" + name + "' has no dimension configured.\n" +
                    "Assuming world: '" + worldFolder + "' and dimension: '" + dimension + "'.");
        }

        if (!Files.isDirectory(worldFolder)) {
            throw new ConfigurationException(
                    "'" + worldFolder.toAbsolutePath().normalize() + "' does not exist or is no directory!\n" +
                    "Check if the 'world' setting in the config-file for that map is correct, or remove the entire config-file if you don't want that map.");
        }

        String worldId = World.id(worldFolder, dimension);
        World world = worlds.get(worldId);
        if (world == null) {
            try {
                Logger.global.logDebug("Loading world " + worldId + " ...");
                world = MCAWorld.load(worldFolder, dimension, loadDataPack(worldFolder));
                worlds.put(worldId, world);
            } catch (IOException ex) {
                throw new ConfigurationException(
                        "Failed to load world " + worldId + "!\n" +
                        "Is the level.dat of that world present and not corrupted?",
                        ex);
            }
        }

        Storage storage = getOrLoadStorage(mapConfig.getStorage());

        try {

            Logger.global.logInfo("Loading map '" + id + "'...");
            BmMap map = new BmMap(
                    id,
                    name,
                    world,
                    storage.map(id),
                    getOrLoadResourcePack(),
                    mapConfig
            );
            maps.put(id, map);

            // load marker-config by converting it first from hocon to json and then loading it with MarkerGson
            ConfigurationNode markerSetNode = mapConfig.getMarkerSets();
            if (markerSetNode != null && !markerSetNode.empty()) {
                String markerJson = GsonConfigurationLoader.builder()
                        .headerMode(HeaderMode.NONE)
                        .lenient(false)
                        .indent(0)
                        .buildAndSaveString(markerSetNode);
                Gson gson = MarkerGson.addAdapters(new GsonBuilder())
                        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                        .create();
                Type markerSetType = new TypeToken<Map<String, MarkerSet>>() {}.getType();
                Map<String, MarkerSet> markerSets = gson.fromJson(markerJson, markerSetType);
                map.getMarkerSets().putAll(markerSets);
            }

        } catch (ConfigurateException | JsonParseException ex) {
            throw new ConfigurationException(
                    "Failed to create the markers for map '" + id + "'!\n" +
                    "Make sure your marker-configuration for this map is valid.",
                    ex);
        } catch (IOException | ConfigurationException ex) {
            throw new ConfigurationException("Failed to load map '" + id + "'!", ex);
        }
    }

    public synchronized Storage getOrLoadStorage(String storageId) throws ConfigurationException, InterruptedException {
        Storage storage = storages.get(storageId);

        if (storage == null) {
            try {
                StorageConfig storageConfig = getConfig().getStorageConfigs().get(storageId);
                if (storageConfig == null) {
                    throw new ConfigurationException("There is no storage-configuration for '" + storageId + "'!\n" +
                            "You will either need to define that storage, or change the map-config to use a storage-config that exists.");
                }

                Logger.global.logInfo("Initializing Storage: '" + storageId + "' (Type: '" + storageConfig.getStorageType().getKey() + "')");

                storage = storageConfig.createStorage();
                storage.initialize();
            } catch (Exception ex) {
                ConfigurationException confEx = new ConfigurationException(
                        "Failed to load and initialize the storage '" + storageId + "'!",
                        ex
                );

                if (storage != null) {
                    try {
                        storage.close();
                    } catch (Exception closeEx) {
                        confEx.addSuppressed(closeEx);
                    }
                }

                throw confEx;
            }

            storages.put(storageId, storage);
        }

        return storage;
    }

    public @Nullable ResourcePack getResourcePack() {
        return resourcePack;
    }

    public synchronized ResourcePack getOrLoadResourcePack() throws ConfigurationException, InterruptedException {
        if (resourcePack == null) {
            MinecraftVersion minecraftVersion = getOrLoadMinecraftVersion();
            Path vanillaResourcePack = minecraftVersion.getResourcePack();

            if (Thread.interrupted()) throw new InterruptedException();

            Deque<Path> packRoots = getPackRoots();
            packRoots.addLast(vanillaResourcePack);

            try {
                ResourcePack resourcePack = new ResourcePack(minecraftVersion.getResourcePackVersion());
                resourcePack.loadResources(packRoots);
                this.resourcePack = resourcePack;
            } catch (IOException | RuntimeException e) {
                throw new ConfigurationException("Failed to parse resources!\n" +
                        "Is one of your resource-packs corrupted?", e);
            }
        }

        return this.resourcePack;
    }

    public synchronized DataPack loadDataPack(Path worldFolder) throws ConfigurationException, InterruptedException {
        MinecraftVersion minecraftVersion = getOrLoadMinecraftVersion();
        Path vanillaDataPack = minecraftVersion.getDataPack();

        if (Thread.interrupted()) throw new InterruptedException();

        // also load world datapacks
        Iterable<Path> worldPacks = List.of();
        Path worldPacksFolder = worldFolder.resolve("datapacks");
        if (Files.isDirectory(worldPacksFolder)) {
            try (Stream<Path> worldPacksStream = Files.list(worldPacksFolder)) {
                worldPacks = worldPacksStream.toList();
            } catch (IOException e) {
                throw new ConfigurationException("Failed to access the worlds datapacks folder.", e);
            }
        }

        Deque<Path> packRoots = getPackRoots(worldPacks);
        packRoots.addLast(vanillaDataPack);

        try {
            DataPack datapack = new DataPack(minecraftVersion.getDataPackVersion());
            datapack.loadResources(packRoots);
            return datapack;
        } catch (IOException | RuntimeException e) {
            throw new ConfigurationException("Failed to parse resources!\n" +
                    "Is one of your resource-packs corrupted?", e);
        }
    }

    private synchronized Deque<Path> getPackRoots(Path... additionalRoots) throws ConfigurationException, InterruptedException {
        return getPackRoots(List.of(additionalRoots));
    }

    private synchronized Deque<Path> getPackRoots(Iterable<Path> additionalRoots) throws ConfigurationException, InterruptedException {
        @Nullable Path packsFolder = config.getPacksFolder();
        @Nullable Path modsFolder = config.getModsFolder();

        try {
            FileHelper.createDirectories(packsFolder);
        } catch (IOException ex) {
            throw new ConfigurationException(
                    "BlueMap failed to create this folder:\n" +
                            packsFolder + "\n" +
                            "Does BlueMap have sufficient permissions?",
                    ex);
        }

        Path resourceExtensionsFile = config.getCoreConfig().getData().resolve("resourceExtensions.zip");

        if (Thread.interrupted()) throw new InterruptedException();

        try {
            Files.deleteIfExists(resourceExtensionsFile);
            FileHelper.createDirectories(resourceExtensionsFile.getParent());
            URL resourceExtensionsUrl = Objects.requireNonNull(
                    Plugin.class.getResource("/de/bluecolored/bluemap/resourceExtensions.zip")
            );
            FileHelper.copy(resourceExtensionsUrl, resourceExtensionsFile);
        } catch (IOException ex) {
            throw new ConfigurationException(
                    "Failed to create resourceExtensions.zip!\n" +
                            "Does BlueMap has sufficient write permissions?",
                    ex);
        }

        Deque<Path> packRoots = new LinkedList<>();

        // load from pack folder
        if (packsFolder != null && Files.isDirectory(packsFolder)) {
            try (Stream<Path> packFiles = Files.list(packsFolder)) {
                packFiles
                        .sorted(Comparator.reverseOrder())
                        .forEach(packRoots::add);
            } catch (IOException e) {
                throw new ConfigurationException("Failed to access packs folder.", e);
            }
        }

        // add additional roots
        additionalRoots.forEach(packRoots::add);

        // load from mods folder
        if (config.getCoreConfig().isScanForModResources() && modsFolder != null && Files.isDirectory(modsFolder)) {
            try (Stream<Path> packFiles = Files.list(modsFolder)) {
                packFiles
                        .filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(".jar"))
                        .forEach(packRoots::add);
            } catch (IOException e) {
                throw new ConfigurationException("Failed to access packs folder.", e);
            }
        }

        packRoots.add(resourceExtensionsFile);
        return packRoots;
    }

    public synchronized MinecraftVersion getOrLoadMinecraftVersion() throws ConfigurationException {
        if (this.minecraftVersion == null) {
            try {
                this.minecraftVersion = MinecraftVersion.load(
                        config.getMinecraftVersion(),
                        config.getCoreConfig().getData(),
                        config.getCoreConfig().isAcceptDownload()
                );
            } catch (IOException ex) {
                if (!config.getCoreConfig().isAcceptDownload()) {
                    throw new MissingResourcesException();
                } else {
                    throw new ConfigurationException("""
                            BlueMap was not able to download some important resources!
                            Make sure BlueMap is able to connect to mojang-servers (%s)."""
                            .formatted(VersionManifest.DOMAIN), ex);
                }
            }
        }

        return this.minecraftVersion;
    }

    public BlueMapConfiguration getConfig() {
        return config;
    }

    @Override
    public void close() throws IOException {
        IOException exception = null;

        for (Storage storage : storages.values()) {
            try {
                if (storage != null) {
                    storage.close();
                }
            } catch (IOException ex) {
                if (exception == null) exception = ex;
                else exception.addSuppressed(ex);
            }
        }

        if (exception != null)
            throw exception;
    }

}
