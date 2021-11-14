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

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.common.web.WebSettings;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.config.ConfigManager;
import de.bluecolored.bluemap.core.config.ConfigurationException;
import de.bluecolored.bluemap.core.config.old.CoreConfig;
import de.bluecolored.bluemap.core.config.old.MapConfig;
import de.bluecolored.bluemap.core.config.old.RenderConfig;
import de.bluecolored.bluemap.core.config.old.WebServerConfig;
import de.bluecolored.bluemap.core.config.storage.StorageConfig;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.resourcepack.ParseResourceException;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.world.World;
import org.apache.commons.io.FileUtils;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * This is the attempt to generalize as many actions as possible to have CLI and Plugins run on the same general setup-code.
 */
@DebugDump
public class BlueMapService {
    private final MinecraftVersion minecraftVersion;
    private final File configFolder;
    private final ThrowingFunction<File, UUID, IOException> worldUUIDProvider;
    private final ThrowingFunction<UUID, String, IOException> worldNameProvider;

    private final ConfigManager configManager;
    private final de.bluecolored.bluemap.core.config.old.ConfigManager configManagerOld;

    private CoreConfig coreConfig;
    private RenderConfig renderConfig;
    private WebServerConfig webServerConfig;

    private final Map<String, Storage> storages;

    private ResourcePack resourcePack;

    private Map<UUID, World> worlds;
    private Map<String, BmMap> maps;
    private Map<String, Storage> mapStorages;

    public BlueMapService(MinecraftVersion minecraftVersion, File configFolder) {
        this.minecraftVersion = minecraftVersion;
        this.configFolder = configFolder;

        Map<File, UUID> uuids = new HashMap<>();
        this.worldUUIDProvider = file -> {
            UUID uuid = uuids.get(file);
            if (uuid == null) {
                uuid = UUID.randomUUID();
                uuids.put(file, uuid);
            }
            return uuid;
        };

        this.worldNameProvider = uuid -> null;

        this.storages = new HashMap<>();

        configManagerOld = new de.bluecolored.bluemap.core.config.old.ConfigManager();
        configManager = new ConfigManager(this.configFolder.toPath());
    }

    public BlueMapService(MinecraftVersion minecraftVersion, ServerInterface serverInterface) {
        this.minecraftVersion = minecraftVersion;
        this.configFolder = serverInterface.getConfigFolder();
        this.worldUUIDProvider = serverInterface::getUUIDForWorld;
        this.worldNameProvider = serverInterface::getWorldName;

        this.storages = new HashMap<>();

        this.configManagerOld = new de.bluecolored.bluemap.core.config.old.ConfigManager();
        configManager = new ConfigManager(this.configFolder.toPath());
    }

    public synchronized void createOrUpdateWebApp(boolean force) throws ConfigurationException {
        WebFilesManager webFilesManager = new WebFilesManager(getRenderConfig().getWebRoot());
        if (force || webFilesManager.needsUpdate()) {
            try {
                webFilesManager.updateFiles();
            } catch (IOException ex) {
                throw new ConfigurationException("Failed to update web-app files!", ex);
            }
        }
    }

    public synchronized WebSettings updateWebAppSettings() throws ConfigurationException, InterruptedException {
        try {
            WebSettings webSettings = new WebSettings(new File(getRenderConfig().getWebRoot(),
                    "data" + File.separator + "settings.json"));

            webSettings.set(getRenderConfig().isUseCookies(), "useCookies");
            webSettings.set(getRenderConfig().isEnableFreeFlight(), "freeFlightEnabled");
            webSettings.setAllMapsEnabled(false);
            for (BmMap map : getMaps().values()) {
                webSettings.setMapEnabled(true, map.getId());
                webSettings.setFrom(map);
            }
            int ordinal = 0;
            for (MapConfig map : getRenderConfig().getMapConfigs()) {
                if (!getMaps().containsKey(map.getId())) continue; //don't add not loaded maps
                webSettings.setOrdinal(ordinal++, map.getId());
                webSettings.setFrom(map);
            }
            webSettings.save();

            return webSettings;
        } catch (IOException ex) {
            throw new ConfigurationException("Failed to update web-app settings!", ex);
        }
    }

    public synchronized Map<UUID, World> getWorlds() throws ConfigurationException, InterruptedException {
        if (worlds == null) loadWorldsAndMaps();
        return worlds;
    }

    public synchronized Map<String, BmMap> getMaps() throws ConfigurationException, InterruptedException {
        if (maps == null) loadWorldsAndMaps();
        return maps;
    }

    public synchronized Map<String, Storage> getMapStorages()  throws ConfigurationException {
        if (mapStorages == null) {
            mapStorages = new HashMap<>();
            if (maps == null) {
                for (MapConfig mapConfig : getRenderConfig().getMapConfigs()) {
                    mapStorages.put(mapConfig.getId(), getStorage(mapConfig.getStorage()));
                }
            } else {
                for (BmMap map : maps.values()) {
                    mapStorages.put(map.getId(), map.getStorage());
                }
            }
        }
        return mapStorages;
    }

    private synchronized void loadWorldsAndMaps() throws ConfigurationException, InterruptedException {
        maps = new HashMap<>();
        worlds = new HashMap<>();

        for (MapConfig mapConfig : getRenderConfig().getMapConfigs()) {
            String id = mapConfig.getId();
            String name = mapConfig.getName();

            File worldFolder = new File(mapConfig.getWorldPath());
            if (!worldFolder.exists() || !worldFolder.isDirectory()) {
                Logger.global.logWarning("Failed to load map '" + id + "': '" + worldFolder.getAbsolutePath() + "' does not exist or is no directory!");
                continue;
            }

            UUID worldUUID;
            try {
                worldUUID = worldUUIDProvider.apply(worldFolder);
            } catch (IOException e) {
                Logger.global.logError("Failed to load map '" + id + "': Failed to get UUID for the world!", e);
                continue;
            }

            World world = worlds.get(worldUUID);
            if (world == null) {
                try {
                    world = MCAWorld.load(worldFolder.toPath(), worldUUID, worldNameProvider.apply(worldUUID), mapConfig.getWorldSkyLight(), mapConfig.isIgnoreMissingLightData());
                    worlds.put(worldUUID, world);
                } catch (IOException e) {
                    Logger.global.logError("Failed to load map '" + id + "'!", e);
                    continue;
                }
            }

            Storage storage = getStorage(mapConfig.getStorage());
            try {
                BmMap map = new BmMap(
                        id,
                        name,
                        world,
                        storage,
                        getResourcePack(),
                        mapConfig
                );

                maps.put(id, map);
            } catch (IOException ex) {
                Logger.global.logError("Failed to load map '" + id + "'!", ex);
            }
        }

        worlds = Collections.unmodifiableMap(worlds);
        maps = Collections.unmodifiableMap(maps);
    }

    public synchronized Storage getStorage(String id) throws ConfigurationException {
        Storage storage = storages.get(id);

        if (storage == null) {
            storage = loadStorage(id);
            storages.put(id, storage);
        }

        return storage;
    }

    private synchronized Storage loadStorage(String id) throws ConfigurationException {
        Logger.global.logInfo("Loading storage '" + id + "'...");

        Path storageFolder = Paths.get("storages");
        Path storageConfigFolder = configManager.getConfigRoot().resolve(storageFolder);

        if (!Files.exists(storageConfigFolder)){
            try {
                Files.createDirectories(storageConfigFolder);

                Files.copy(
                        Objects.requireNonNull(BlueMapService.class
                                .getResourceAsStream("/de/bluecolored/bluemap/config/storages/file.conf")),
                        storageConfigFolder.resolve("file.conf")
                );
                Files.copy(
                        Objects.requireNonNull(BlueMapService.class
                                .getResourceAsStream("/de/bluecolored/bluemap/config/storages/sql.conf")),
                        storageConfigFolder.resolve("sql.conf")
                );
            } catch (IOException | NullPointerException ex) {
                  Logger.global.logWarning("Failed to create default storage-configuration-files: " + ex);
            }
        }

        try {
            ConfigurationNode node = configManager.loadConfig(storageFolder.resolve(id));
            StorageConfig storageConfig = Objects.requireNonNull(node.get(StorageConfig.class));
            Storage storage = storageConfig.getStorageType().create(node);
            storage.initialize();
            return storage;
        } catch (Exception ex) {
            throw new ConfigurationException(
                    "BlueMap tried to create the storage '" + id + "' but something went wrong.\n" +
                    "Check if that storage is configured correctly.",
                    ex
            );
        }
    }

    public synchronized ResourcePack getResourcePack() throws ConfigurationException, InterruptedException {
        if (resourcePack == null) {
            File defaultResourceFile = new File(getCoreConfig().getDataFolder(), "minecraft-client-" + minecraftVersion.getResource().getVersion().getVersionString() + ".jar");
            File resourceExtensionsFile = new File(getCoreConfig().getDataFolder(), "resourceExtensions.zip");

            File textureExportFile = new File(getRenderConfig().getWebRoot(), "data" + File.separator + "textures.json");

            File resourcePackFolder = new File(configFolder, "resourcepacks");
            try {
                FileUtils.forceMkdir(resourcePackFolder);
            } catch (IOException ex) {
                throw new ConfigurationException(
                        "BlueMap failed to create this folder:\n" +
                        resourcePackFolder + "\n" +
                        "Does BlueMap has sufficient permissions?",
                        ex);
            }

            if (!defaultResourceFile.exists()) {
                if (getCoreConfig().isDownloadAccepted()) {

                    //download file
                    try {
                        Logger.global.logInfo("Downloading " + minecraftVersion.getResource().getClientUrl() + " to " + defaultResourceFile + " ...");
                        FileUtils.forceMkdirParent(defaultResourceFile);
                        FileUtils.copyURLToFile(new URL(minecraftVersion.getResource().getClientUrl()), defaultResourceFile, 10000, 10000);
                    } catch (IOException ex) {
                        throw new ConfigurationException("Failed to download resources!", ex);
                    }

                } else {
                    throw new MissingResourcesException();
                }
            }

            Logger.global.logInfo("Loading resources...");

            try {
                if (resourceExtensionsFile.exists()) FileUtils.forceDelete(resourceExtensionsFile);
                FileUtils.forceMkdirParent(resourceExtensionsFile);
                URL resourceExtensionsUrl = Objects.requireNonNull(
                        Plugin.class.getResource(
                                "/de/bluecolored/bluemap/" + minecraftVersion.getResource().getResourcePrefix() +
                                "/resourceExtensions.zip")
                );
                FileUtils.copyURLToFile(resourceExtensionsUrl, resourceExtensionsFile, 10000, 10000);
            } catch (IOException ex) {
                throw new ConfigurationException(
                        "Failed to create resourceExtensions.zip!\n" +
                        "Does BlueMap has sufficient write permissions?",
                        ex);
            }

            //find more resource packs
            File[] resourcePacks = resourcePackFolder.listFiles();
            if (resourcePacks == null) resourcePacks = new File[0];
            Arrays.sort(resourcePacks); //load resource packs in alphabetical order so you can reorder them by renaming

            List<File> resources = new ArrayList<>(resourcePacks.length + 1);
            resources.add(defaultResourceFile);
            resources.addAll(Arrays.asList(resourcePacks));
            resources.add(resourceExtensionsFile);

            try {
                resourcePack = new ResourcePack();
                if (textureExportFile.exists()) resourcePack.loadTextureFile(textureExportFile);
                resourcePack.load(resources);
                resourcePack.saveTextureFile(textureExportFile);
            } catch (IOException | ParseResourceException e) {
                throw new ConfigurationException("Failed to parse resources!", e);
            }

        }

        return resourcePack;
    }

    @Deprecated
    public synchronized de.bluecolored.bluemap.core.config.old.ConfigManager getConfigManagerOld() {
        return configManagerOld;
    }

    public File getCoreConfigFile() {
        return new File(configFolder, "core.conf");
    }

    public synchronized CoreConfig getCoreConfig() throws ConfigurationException {
        if (coreConfig == null) {
            coreConfig = new CoreConfig(configManagerOld.loadOrCreate(
                    getCoreConfigFile(),
                    Plugin.class.getResource("/de/bluecolored/bluemap/core.conf"),
                    Plugin.class.getResource("/de/bluecolored/bluemap/core-defaults.conf"),
                    true,
                    true
            ));
        }

        return coreConfig;
    }

    public File getRenderConfigFile() {
        return new File(configFolder, "render.conf");
    }

    public synchronized RenderConfig getRenderConfig() throws ConfigurationException {
        if (renderConfig == null) {
            renderConfig = new RenderConfig(configManagerOld.loadOrCreate(
                    getRenderConfigFile(),
                    Plugin.class.getResource("/de/bluecolored/bluemap/render.conf"),
                    Plugin.class.getResource("/de/bluecolored/bluemap/render-defaults.conf"),
                    true,
                    true
            ));
        }

        return renderConfig;
    }

    public File getWebServerConfigFile() {
        return new File(configFolder, "webserver.conf");
    }

    public synchronized WebServerConfig getWebServerConfig() throws ConfigurationException {
        if (webServerConfig == null) {
            webServerConfig = new WebServerConfig(configManagerOld.loadOrCreate(
                    getWebServerConfigFile(),
                    Plugin.class.getResource("/de/bluecolored/bluemap/webserver.conf"),
                    Plugin.class.getResource("/de/bluecolored/bluemap/webserver-defaults.conf"),
                    true,
                    true
            ));
        }

        return webServerConfig;
    }

    public File getConfigFolder() {
        return configFolder;
    }

    private interface ThrowingFunction<T, R, E extends Throwable> {
        R apply(T t) throws E;
    }

}
