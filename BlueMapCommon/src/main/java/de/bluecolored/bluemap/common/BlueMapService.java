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
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.common.config.MapConfig;
import de.bluecolored.bluemap.common.config.storage.StorageConfig;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.debug.StateDumper;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.resources.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.util.FileHelper;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.mca.MCAWorld;
import lombok.EqualsAndHashCode;
import org.apache.commons.io.FileUtils;
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
@DebugDump
public class BlueMapService implements Closeable {

    private final BlueMapConfiguration config;
    private final WebFilesManager webFilesManager;

    private final Map<WorldKey, World> worlds;
    private final Map<String, BmMap> maps;
    private final Map<String, Storage> storages;

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

    public @Nullable Collection<World> getWorlds() {
        return worlds.values();
    }

    public @Nullable Map<String, BmMap> getMaps() {
        return maps;
    }

    public Map<String, BmMap> loadMaps() throws InterruptedException {
        loadWorldsAndMaps(mapId -> true);
        return maps;
    }

    public Map<String, BmMap> loadMaps(Predicate<String> mapFilter) throws InterruptedException {
        loadWorldsAndMaps(mapFilter);
        return maps;
    }

    private synchronized void loadWorldsAndMaps(Predicate<String> mapFilter) throws InterruptedException {
        for (var entry : config.getMapConfigs().entrySet()) {
            if (!mapFilter.test(entry.getKey())) continue;
            try {
                loadMapConfig(entry.getKey(), entry.getValue());
            } catch (ConfigurationException ex) {
                Logger.global.logWarning(ex.getFormattedExplanation());
                Throwable cause = ex.getRootCause();
                if (cause != null) {
                    Logger.global.logError("Detailed error:", ex);
                }
            }
        }
    }

    private synchronized void loadMapConfig(String id, MapConfig mapConfig) throws ConfigurationException, InterruptedException {
        String name = mapConfig.getName();
        if (name == null) name = id;

        Path worldFolder = mapConfig.getWorld();
        Key dimension = mapConfig.getDimension();

        // if there is no world configured, we assume the map is static, or supplied from a different server
        if (worldFolder == null) {
            Logger.global.logInfo("The map '" + name + "' has no world configured. The map will be displayed, but not updated!");
            return;
        }

        if (!Files.isDirectory(worldFolder)) {
            throw new ConfigurationException(
                    "'" + worldFolder.toAbsolutePath().normalize() + "' does not exist or is no directory!\n" +
                    "Check if the 'world' setting in the config-file for that map is correct, or remove the entire config-file if you don't want that map.");
        }

        WorldKey worldKey = new WorldKey(worldFolder, dimension);
        World world = worlds.get(worldKey);
        if (world == null) {
            try {
                Logger.global.logInfo("Loading world " + worldKey + " ...");
                world = MCAWorld.load(worldFolder, dimension);
                worlds.put(worldKey, world);
            } catch (IOException ex) {
                throw new ConfigurationException(
                        "Failed to load world " + worldKey + "!\n" +
                        "Is the level.dat of that world present and not corrupted?",
                        ex);
            }
        }

        Storage storage = getStorage(mapConfig.getStorage());

        try {

            Logger.global.logInfo("Loading map '" + name + "'...");
            BmMap map = new BmMap(
                    id,
                    name,
                    world,
                    storage,
                    loadResourcePack(),
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

    public synchronized Storage getStorage(String storageId) throws ConfigurationException {
        Storage storage = storages.get(storageId);

        if (storage == null) {
            try {
                StorageConfig storageConfig = getConfig().getStorageConfigs().get(storageId);
                if (storageConfig == null) {
                    throw new ConfigurationException("There is no storage-configuration for '" + storageId + "'!\n" +
                            "You will either need to define that storage, or change the map-config to use a storage-config that exists.");
                }

                Logger.global.logInfo("Initializing Storage: '" + storageId + "' (Type: " + storageConfig.getStorageType() + ")");

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

    public synchronized ResourcePack loadResourcePack() throws ConfigurationException, InterruptedException {
        if (resourcePack == null) {
            MinecraftVersion minecraftVersion = serverInterface.getMinecraftVersion();

            Path defaultResourceFile = config.getCoreConfig().getData().resolve("minecraft-client-" + minecraftVersion.getResource().getVersion().getVersionString() + ".jar");
            Path resourceExtensionsFile = config.getCoreConfig().getData().resolve("resourceExtensions.zip");

            Path resourcePackFolder = serverInterface.getConfigFolder().resolve("resourcepacks");

            try {
                FileHelper.createDirectories(resourcePackFolder);
            } catch (IOException ex) {
                throw new ConfigurationException(
                        "BlueMap failed to create this folder:\n" +
                        resourcePackFolder + "\n" +
                        "Does BlueMap have sufficient permissions?",
                        ex);
            }

            if (!Files.exists(defaultResourceFile)) {
                if (config.getCoreConfig().isAcceptDownload()) {
                    //download file
                    try {
                        Logger.global.logInfo("Downloading " + minecraftVersion.getResource().getClientUrl() + " to " + defaultResourceFile + " ...");

                        FileHelper.createDirectories(defaultResourceFile.getParent());
                        Path tempResourceFile = defaultResourceFile.getParent().resolve(defaultResourceFile.getFileName() + ".filepart");
                        Files.deleteIfExists(tempResourceFile);
                        FileUtils.copyURLToFile(new URL(minecraftVersion.getResource().getClientUrl()), tempResourceFile.toFile(), 10000, 10000);
                        FileHelper.move(tempResourceFile, defaultResourceFile);
                    } catch (IOException ex) {
                        throw new ConfigurationException("Failed to download resources!", ex);
                    }

                } else {
                    throw new MissingResourcesException();
                }
            }

            try {
                Files.deleteIfExists(resourceExtensionsFile);
                FileHelper.createDirectories(resourceExtensionsFile.getParent());
                URL resourceExtensionsUrl = Objects.requireNonNull(
                        Plugin.class.getResource(
                                "/de/bluecolored/bluemap/" + minecraftVersion.getResource().getResourcePrefix() +
                                "/resourceExtensions.zip")
                );
                FileUtils.copyURLToFile(resourceExtensionsUrl, resourceExtensionsFile.toFile(), 10000, 10000);
            } catch (IOException ex) {
                throw new ConfigurationException(
                        "Failed to create resourceExtensions.zip!\n" +
                        "Does BlueMap has sufficient write permissions?",
                        ex);
            }

            try {
                resourcePack = new ResourcePack();

                List<Path> resourcePackRoots = new ArrayList<>();
                // load from resourcepack folder
                try (Stream<Path> resourcepackFiles = Files.list(resourcePackFolder)) {
                    resourcepackFiles
                            .sorted(Comparator.reverseOrder())
                            .forEach(resourcePackRoots::add);
                }

                if (config.getCoreConfig().isScanForModResources()) {

                    // load from mods folder
                    Path modsFolder = serverInterface.getModsFolder().orElse(null);
                    if (modsFolder != null && Files.isDirectory(modsFolder)) {
                        try (Stream<Path> resourcepackFiles = Files.list(modsFolder)) {
                            resourcepackFiles
                                    .filter(Files::isRegularFile)
                                    .filter(file -> file.getFileName().toString().endsWith(".jar"))
                                    .forEach(resourcePackRoots::add);
                        }
                    }

                    // load from datapacks
                    for (Path worldFolder : getWorldFolders()) {
                        Path datapacksFolder = worldFolder.resolve("datapacks");
                        if (!Files.isDirectory(datapacksFolder)) continue;

                        try (Stream<Path> resourcepackFiles = Files.list(worldFolder.resolve("datapacks"))) {
                            resourcepackFiles.forEach(resourcePackRoots::add);
                        }
                    }

                }

                resourcePackRoots.add(resourceExtensionsFile);
                resourcePackRoots.add(defaultResourceFile);

                resourcePack.loadResources(resourcePackRoots);
            } catch (IOException | RuntimeException e) {
                throw new ConfigurationException("Failed to parse resources!\n" +
                        "Is one of your resource-packs corrupted?", e);
            }

        }

        return resourcePack;
    }

    public Optional<ResourcePack> getResourcePackIfLoaded() {
        return Optional.ofNullable(this.resourcePack);
    }

    private Collection<Path> getWorldFolders() {
        Set<Path> folders = new HashSet<>();
        for (MapConfig mapConfig : config.getMapConfigs().values()) {
            Path folder = mapConfig.getWorld();
            if (folder == null) continue;
            folder = folder.toAbsolutePath().normalize();
            if (Files.isDirectory(folder)) {
                folders.add(folder);
            }
        }
        return folders;
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

    @EqualsAndHashCode
    private static class WorldKey {

        private final Path worldFolder;
        private final Key dimension;

        public WorldKey(Path worldFolder, Key dimension) {
            this.worldFolder = worldFolder;
            this.dimension = dimension;
        }

        @Override
        public String toString() {
            return worldFolder + "[" + dimension + "]";
        }
    }

}
