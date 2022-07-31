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
import de.bluecolored.bluemap.api.markers.MarkerGson;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.common.config.MapConfig;
import de.bluecolored.bluemap.common.config.storage.StorageConfig;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.ServerInterface;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.debug.StateDumper;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.resources.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.util.AtomicFileHelper;
import de.bluecolored.bluemap.core.world.World;
import org.apache.commons.io.FileUtils;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.loader.HeaderMode;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Stream;

/**
 * This is the attempt to generalize as many actions as possible to have CLI and Plugins run on the same general setup-code.
 */
@DebugDump
public class BlueMapService {
    private final ServerInterface serverInterface;
    private final BlueMapConfigProvider configs;

    private final Map<Path, String> worldIds;
    private final Map<String, Storage> storages;

    private Map<String, World> worlds;
    private Map<String, BmMap> maps;

    private ResourcePack resourcePack;

    public BlueMapService(ServerInterface serverInterface, BlueMapConfigProvider configProvider) {
        this.serverInterface = serverInterface;
        this.configs = configProvider;

        this.worldIds = new HashMap<>();
        this.storages = new HashMap<>();

        StateDumper.global().register(this);
    }

    public synchronized String getWorldId(Path worldFolder) throws IOException {
        // fast-path
        String id = worldIds.get(worldFolder);
        if (id != null) return id;

        // second try with normalized absolute path
        worldFolder = worldFolder.toAbsolutePath().normalize();
        id = worldIds.get(worldFolder);
        if (id != null) return id;

        // secure (slower) query with real path
        worldFolder = worldFolder.toRealPath();
        id = worldIds.get(worldFolder);
        if (id != null) return id;

        // now we can be sure it wasn't loaded yet .. load

        Path idFile = worldFolder.resolve("bluemap.id");
        id = this.serverInterface.getWorld(worldFolder)
                .flatMap(ServerWorld::getId)
                .orElse(null);

        if (id != null) {
            // create/update id-file in worldfolder
            Files.writeString(idFile, id, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            worldIds.put(worldFolder, id);
            return id;
        }

        if (!Files.exists(idFile)) {
            id = UUID.randomUUID().toString();
            Files.writeString(idFile, id, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            worldIds.put(worldFolder, id);
            return id;
        }

        id = Files.readString(idFile);
        worldIds.put(worldFolder, id);
        return id;
    }

    public synchronized void createOrUpdateWebApp(boolean force) throws ConfigurationException {
        try {
            WebFilesManager webFilesManager = new WebFilesManager(configs.getWebappConfig().getWebroot());

            // update web-app files
            if (force || webFilesManager.filesNeedUpdate()) {
                webFilesManager.updateFiles();
            }

            // update settings.json
            if (!configs.getWebappConfig().isUpdateSettingsFile()) {
                webFilesManager.loadSettings();
            } else {
                webFilesManager.setFrom(configs.getWebappConfig());
            }
            for (String mapId : configs.getMapConfigs().keySet()) {
                webFilesManager.addMap(mapId);
            }
            webFilesManager.saveSettings();

        } catch (IOException ex) {
            throw new ConfigurationException("Failed to update web-app files!", ex);
        }
    }

    public synchronized Map<String, World> getWorlds() throws ConfigurationException, InterruptedException {
        if (worlds == null) loadWorldsAndMaps();
        return worlds;
    }

    public synchronized Map<String, BmMap> getMaps() throws ConfigurationException, InterruptedException {
        if (maps == null) loadWorldsAndMaps();
        return maps;
    }

    private synchronized void loadWorldsAndMaps() throws ConfigurationException, InterruptedException {
        maps = new HashMap<>();
        worlds = new HashMap<>();

        for (var entry : configs.getMapConfigs().entrySet()) {
            MapConfig mapConfig = entry.getValue();

            String id = entry.getKey();
            String name = mapConfig.getName();

            Path worldFolder = mapConfig.getWorld();
            if (!Files.isDirectory(worldFolder)) {
                throw new ConfigurationException("Failed to load map '" + id + "': \n" +
                        "'" + worldFolder.toAbsolutePath().normalize() + "' does not exist or is no directory!\n" +
                        "Check if the 'world' setting in the config-file for that map is correct, or remove the entire config-file if you don't want that map.");
            }

            String worldId;
            try {
                worldId = getWorldId(worldFolder);
            } catch (IOException ex) {
                throw new ConfigurationException("Failed to load map '" + id + "': \n" +
                        "Could not load the ID for the world!\n" +
                        "Make sure BlueMap has read and write access/permissions to the world-files for this map.",
                        ex);
            }

            World world = worlds.get(worldId);
            if (world == null) {
                try {
                    world = new MCAWorld(worldFolder, mapConfig.getWorldSkyLight(), mapConfig.isIgnoreMissingLightData());
                    worlds.put(worldId, world);
                } catch (IOException ex) {
                    throw new ConfigurationException("Failed to load world (" + worldId + ") for map '" + id + "'!\n" +
                            "Is the level.dat of that world present and not corrupted?",
                            ex);
                }
            }

            Storage storage = getStorage(mapConfig.getStorage());

            try {
                BmMap map = new BmMap(
                        id,
                        name,
                        worldId,
                        world,
                        storage,
                        getResourcePack(),
                        mapConfig
                );
                maps.put(id, map);

                // load marker-config by converting it first from hocon to json and then loading it with MarkerGson
                if (!mapConfig.getMarkerSets().empty()) {
                    String markerJson = GsonConfigurationLoader.builder()
                            .headerMode(HeaderMode.NONE)
                            .lenient(false)
                            .indent(0)
                            .buildAndSaveString(mapConfig.getMarkerSets());
                    Gson gson = MarkerGson.addAdapters(new GsonBuilder())
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES)
                            .create();
                    Type markerSetType = new TypeToken<Map<String, MarkerSet>>() {}.getType();
                    Map<String, MarkerSet> markerSets = gson.fromJson(markerJson, markerSetType);
                    map.getMarkerSets().putAll(markerSets);
                }

            } catch (ConfigurateException | JsonParseException ex) {
                throw new ConfigurationException("Failed to load map '" + id + "': \n" +
                        "Failed to create the markers for this map!\n" +
                        "Make sure your marker-configuration for this map is valid.",
                        ex);
            } catch (IOException ex) {
                throw new ConfigurationException("Failed to load map '" + id + "'!", ex);
            }

        }

        worlds = Collections.unmodifiableMap(worlds);
        maps = Collections.unmodifiableMap(maps);
    }

    public synchronized Storage getStorage(String storageId) throws ConfigurationException {
        Storage storage = storages.get(storageId);

        if (storage == null) {
            try {
                StorageConfig storageConfig = getConfigs().getStorageConfigs().get(storageId);
                if (storageConfig == null) {
                    throw new ConfigurationException("There is no storage-configuration for '" + storageId + "'!\n" +
                            "You will either need to define that storage, or change the map-config to use a storage-config that exists.");
                }

                storage = storageConfig.createStorage();
                storage.initialize();
            } catch (Exception ex) {
                throw new ConfigurationException("Failed to load and initialize the storage '" + storageId + "'!",
                        ex);
            }

            storages.put(storageId, storage);
        }

        return storage;
    }

    public synchronized ResourcePack getResourcePack() throws ConfigurationException, InterruptedException {
        if (resourcePack == null) {
            MinecraftVersion minecraftVersion = serverInterface.getMinecraftVersion();

            Path defaultResourceFile = configs.getCoreConfig().getData().resolve("minecraft-client-" + minecraftVersion.getResource().getVersion().getVersionString() + ".jar");
            Path resourceExtensionsFile = configs.getCoreConfig().getData().resolve("resourceExtensions.zip");

            Path resourcePackFolder = serverInterface.getConfigFolder().resolve("resourcepacks");

            try {
                Files.createDirectories(resourcePackFolder);
            } catch (IOException ex) {
                throw new ConfigurationException(
                        "BlueMap failed to create this folder:\n" +
                        resourcePackFolder + "\n" +
                        "Does BlueMap has sufficient permissions?",
                        ex);
            }

            if (!Files.exists(defaultResourceFile)) {
                if (configs.getCoreConfig().isAcceptDownload()) {
                    //download file
                    try {
                        Logger.global.logInfo("Downloading " + minecraftVersion.getResource().getClientUrl() + " to " + defaultResourceFile + " ...");

                        Files.createDirectories(defaultResourceFile.getParent());
                        Path tempResourceFile = defaultResourceFile.getParent().resolve(defaultResourceFile.getFileName() + ".filepart");
                        Files.deleteIfExists(tempResourceFile);
                        FileUtils.copyURLToFile(new URL(minecraftVersion.getResource().getClientUrl()), tempResourceFile.toFile(), 10000, 10000);
                        AtomicFileHelper.move(tempResourceFile, defaultResourceFile);
                    } catch (IOException ex) {
                        throw new ConfigurationException("Failed to download resources!", ex);
                    }

                } else {
                    throw new MissingResourcesException();
                }
            }

            try {
                Files.deleteIfExists(resourceExtensionsFile);
                Files.createDirectories(resourceExtensionsFile.getParent());
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

                if (configs.getCoreConfig().isScanForModResources()) {

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

    private Collection<Path> getWorldFolders() {
        Set<Path> folders = new HashSet<>();
        for (MapConfig mapConfig : configs.getMapConfigs().values()) {
            Path folder = mapConfig.getWorld().toAbsolutePath().normalize();
            if (Files.isDirectory(folder)) {
                folders.add(folder);
            }
        }
        return folders;
    }

    public BlueMapConfigProvider getConfigs() {
        return configs;
    }

}
