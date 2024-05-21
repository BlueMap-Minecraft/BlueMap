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
package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.common.BlueMapConfiguration;
import de.bluecolored.bluemap.common.config.storage.StorageConfig;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.pack.datapack.DataPack;
import de.bluecolored.bluemap.core.util.FileHelper;
import de.bluecolored.bluemap.core.util.Key;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

@Getter
public class BlueMapConfigManager implements BlueMapConfiguration {

    public static final String CORE_CONFIG_NAME = "core";
    public static final String WEBSERVER_CONFIG_NAME = "webserver";
    public static final String WEBAPP_CONFIG_NAME = "webapp";
    public static final String PLUGIN_CONFIG_NAME = "plugin";
    public static final String MAPS_CONFIG_FOLDER_NAME = "maps";
    public static final String STORAGES_CONFIG_FOLDER_NAME = "storages";

    private final ConfigManager configManager;

    private final CoreConfig coreConfig;
    private final WebserverConfig webserverConfig;
    private final WebappConfig webappConfig;
    private final PluginConfig pluginConfig;
    private final Map<String, MapConfig> mapConfigs;
    private final Map<String, StorageConfig> storageConfigs;
    private final Path packsFolder;
    private final @Nullable String minecraftVersion;
    private final @Nullable Path modsFolder;

    @Builder
    private BlueMapConfigManager(
            @NonNull Path configRoot,
            @Nullable String minecraftVersion,
            @Nullable Path defaultDataFolder,
            @Nullable Path defaultWebroot,
            @Nullable Collection<ServerWorld> autoConfigWorlds,
            @Nullable Boolean usePluginConfig,
            @Nullable Boolean useMetricsConfig,
            @Nullable Path packsFolder,
            @Nullable Path modsFolder
    ) throws ConfigurationException {
        // set defaults
        if (defaultDataFolder == null) defaultDataFolder = Path.of("bluemap");
        if (defaultWebroot == null) defaultWebroot = Path.of("bluemap", "web");
        if (autoConfigWorlds == null) autoConfigWorlds = Collections.emptyList();
        if (usePluginConfig == null) usePluginConfig = true;
        if (useMetricsConfig == null) useMetricsConfig = true;
        if (packsFolder == null) packsFolder = configRoot.resolve("packs");

        // load
        this.configManager = new ConfigManager(configRoot);
        this.coreConfig = loadCoreConfig(defaultDataFolder, useMetricsConfig);
        this.webappConfig = loadWebappConfig(defaultWebroot);
        this.webserverConfig = loadWebserverConfig(webappConfig.getWebroot(), coreConfig.getData());
        this.pluginConfig = usePluginConfig ? loadPluginConfig() : new PluginConfig();
        this.storageConfigs = Collections.unmodifiableMap(loadStorageConfigs(webappConfig.getWebroot()));
        this.mapConfigs = Collections.unmodifiableMap(loadMapConfigs(autoConfigWorlds));
        this.packsFolder = packsFolder;
        this.minecraftVersion = minecraftVersion;
        this.modsFolder = modsFolder;
    }

    private CoreConfig loadCoreConfig(Path defaultDataFolder, boolean useMetricsConfig) throws ConfigurationException {
        Path configFile = configManager.resolveConfigFile(CORE_CONFIG_NAME);
        Path configFolder = configFile.getParent();

        if (!Files.exists(configFile)) {
            try {
                FileHelper.createDirectories(configFolder);
                Files.writeString(
                        configFile,
                        configManager.loadConfigTemplate(CORE_CONFIG_NAME)
                                .setConditional("metrics", useMetricsConfig)
                                .setVariable("timestamp", LocalDateTime.now().withNano(0).toString())
                                .setVariable("version", BlueMap.VERSION)
                                .setVariable("data", formatPath(defaultDataFolder))
                                .setVariable("implementation", "bukkit")
                                .setVariable("render-thread-count", Integer.toString(suggestRenderThreadCount()))
                                .setVariable("logfile", formatPath(defaultDataFolder.resolve("logs").resolve("debug.log")))
                                .setVariable("logfile-with-time", formatPath(defaultDataFolder.resolve("logs").resolve("debug_%1$tF_%1$tT.log")))
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                Logger.global.logWarning("Failed to create default core-configuration-file: " + ex);
            }
        }

        return configManager.loadConfig(CORE_CONFIG_NAME, CoreConfig.class);
    }

    /**
     * determine render-thread preset (very pessimistic, rather let people increase it themselves)
     */
    private int suggestRenderThreadCount() {
        Runtime runtime = Runtime.getRuntime();
        int availableCores = runtime.availableProcessors();
        long availableMemoryMiB = runtime.maxMemory() / 1024L / 1024L;
        int presetRenderThreadCount = 1;
        if (availableCores >= 6 && availableMemoryMiB >= 4096)
            presetRenderThreadCount = 2;
        if (availableCores >= 10 && availableMemoryMiB >= 8192)
            presetRenderThreadCount = 3;
        return presetRenderThreadCount;
    }

    private WebserverConfig loadWebserverConfig(Path defaultWebroot, Path dataRoot) throws ConfigurationException {
        Path configFile = configManager.resolveConfigFile(WEBSERVER_CONFIG_NAME);
        Path configFolder = configFile.getParent();

        if (!Files.exists(configFile)) {
            try {
                FileHelper.createDirectories(configFolder);
                Files.writeString(
                        configFile,
                        configManager.loadConfigTemplate(WEBSERVER_CONFIG_NAME)
                                .setVariable("webroot", formatPath(defaultWebroot))
                                .setVariable("logfile", formatPath(dataRoot.resolve("logs").resolve("webserver.log")))
                                .setVariable("logfile-with-time", formatPath(dataRoot.resolve("logs").resolve("webserver_%1$tF_%1$tT.log")))
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                Logger.global.logWarning("Failed to create default webserver-configuration-file: " + ex);
            }
        }

        return configManager.loadConfig(WEBSERVER_CONFIG_NAME, WebserverConfig.class);
    }

    private WebappConfig loadWebappConfig(Path defaultWebroot) throws ConfigurationException {
        Path configFile = configManager.resolveConfigFile(WEBAPP_CONFIG_NAME);
        Path configFolder = configFile.getParent();

        if (!Files.exists(configFile)) {
            try {
                FileHelper.createDirectories(configFolder);
                Files.writeString(
                        configFile,
                        configManager.loadConfigTemplate(WEBAPP_CONFIG_NAME)
                                .setVariable("webroot", formatPath(defaultWebroot))
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                Logger.global.logWarning("Failed to create default webapp-configuration-file: " + ex);
            }
        }

        return configManager.loadConfig(WEBAPP_CONFIG_NAME, WebappConfig.class);
    }

    private PluginConfig loadPluginConfig() throws ConfigurationException {
        Path configFile = configManager.resolveConfigFile(PLUGIN_CONFIG_NAME);
        Path configFolder = configFile.getParent();

        if (!Files.exists(configFile)) {
            try {
                FileHelper.createDirectories(configFolder);
                Files.writeString(
                        configFile,
                        configManager.loadConfigTemplate(PLUGIN_CONFIG_NAME)
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                Logger.global.logWarning("Failed to create default webapp-configuration-file: " + ex);
            }
        }

        return configManager.loadConfig(PLUGIN_CONFIG_NAME, PluginConfig.class);
    }

    private Map<String, MapConfig> loadMapConfigs(Collection<ServerWorld> autoConfigWorlds) throws ConfigurationException {
        Map<String, MapConfig> mapConfigs = new HashMap<>();

        Path mapConfigFolder = configManager.getConfigRoot().resolve(MAPS_CONFIG_FOLDER_NAME);

        if (!Files.exists(mapConfigFolder)){
            try {
                FileHelper.createDirectories(mapConfigFolder);
                if (autoConfigWorlds.isEmpty()) {
                    Path worldFolder = Path.of("world");
                    Files.writeString(
                            mapConfigFolder.resolve("overworld.conf"),
                            createOverworldMapTemplate("Overworld", worldFolder,
                                    DataPack.DIMENSION_OVERWORLD, 0).build(),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                    );
                    Files.writeString(
                            mapConfigFolder.resolve("nether.conf"),
                            createNetherMapTemplate("Nether", worldFolder,
                                    DataPack.DIMENSION_THE_NETHER, 0).build(),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                    );
                    Files.writeString(
                            mapConfigFolder.resolve("end.conf"),
                            createEndMapTemplate("End", worldFolder,
                                    DataPack.DIMENSION_THE_END, 0).build(),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                    );
                } else {
                    // make sure overworld-dimensions come first, so they are the ones where the
                    // dimension-key is omitted in the generated map-id
                    List<ServerWorld> overworldFirstAutoConfigWorlds = new ArrayList<>(autoConfigWorlds.size());
                    overworldFirstAutoConfigWorlds.addAll(autoConfigWorlds);
                    overworldFirstAutoConfigWorlds.sort(Comparator.comparingInt(w ->
                            DataPack.DIMENSION_OVERWORLD.equals(w.getDimension()) ? 0 : 1
                    ));

                    Set<String> mapIds = new HashSet<>();
                    for (var world : overworldFirstAutoConfigWorlds) {
                        Path worldFolder = world.getWorldFolder().normalize();
                        Key dimension = world.getDimension();

                        String dimensionName = dimension.getNamespace().equals("minecraft") ?
                                dimension.getValue() : dimension.getFormatted();

                        // find unique map id
                        String id = sanitiseMapId(worldFolder.getFileName().toString()).toLowerCase(Locale.ROOT);
                        if (mapIds.contains(id))
                            id = sanitiseMapId(worldFolder.getFileName() + "_" + dimensionName).toLowerCase(Locale.ROOT);
                        int i = 1;
                        String uniqueId = id;
                        while (mapIds.contains(uniqueId))
                            uniqueId = id + "_" + (++i);
                        mapIds.add(uniqueId);

                        Path configFile = mapConfigFolder.resolve(uniqueId + ".conf");
                        String name = worldFolder.getFileName() + " (" + dimensionName + ")";
                        if (i > 1) name = name + " (" + i + ")";

                        ConfigTemplate template = switch (world.getDimension().getFormatted()) {
                            case "minecraft:the_nether" -> createNetherMapTemplate(name, worldFolder, dimension, i - 1);
                            case "minecraft:the_end" -> createEndMapTemplate(name, worldFolder, dimension, i - 1);
                            default -> createOverworldMapTemplate(name, worldFolder, dimension, i - 1);
                        };

                        Files.writeString(
                                configFile,
                                template.build(),
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                        );
                    }
                }
            } catch (IOException | NullPointerException ex) {
                throw new ConfigurationException("BlueMap failed to create default map-configuration-files in\n" +
                        mapConfigFolder.toAbsolutePath().normalize() + "\n" +
                        "Check if BlueMap has the permission to create and read from this folder.",
                        ex);
            }
        }

        try (Stream<Path> configFiles = Files.list(mapConfigFolder)) {
            for (var configFile : configFiles.toArray(Path[]::new)) {
                if (!configManager.isConfigFile(configFile)) continue;
                String id = sanitiseMapId(configManager.getConfigName(configFile));

                if (mapConfigs.containsKey(id)) {
                    throw new ConfigurationException("At least two of your map-config file-names result in ambiguous map-id's!\n" +
                            configFile.toAbsolutePath().normalize() + "\n" +
                            "To resolve this issue, rename this file to something else.");
                }

                MapConfig mapConfig = configManager.loadConfig(configFile, MapConfig.class);
                mapConfigs.put(id, mapConfig);
            }
        } catch (IOException ex) {
            throw new ConfigurationException("BlueMap failed to read your map configuration from\n" +
                            mapConfigFolder.toAbsolutePath().normalize() + "\n" +
                            "Check if BlueMap has the permission to create and read from this folder.",
                    ex);
        }

        return mapConfigs;
    }

    private Map<String, StorageConfig> loadStorageConfigs(Path defaultWebroot) throws ConfigurationException {
        Map<String, StorageConfig> storageConfigs = new HashMap<>();

        Path storageConfigFolder = configManager.getConfigRoot().resolve(STORAGES_CONFIG_FOLDER_NAME);

        if (!Files.exists(storageConfigFolder)){
            try {
                FileHelper.createDirectories(storageConfigFolder);
                Files.writeString(
                        storageConfigFolder.resolve("file.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/storages/file.conf")
                                .setVariable("root", formatPath(defaultWebroot.resolve("maps")))
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
                Files.writeString(
                        storageConfigFolder.resolve("sql.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/storages/sql.conf").build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                throw new ConfigurationException("BlueMap failed to create default storage-configuration-files in\n" +
                                storageConfigFolder.toAbsolutePath().normalize() + "\n" +
                                "Check if BlueMap has the permission to create and read from this folder.",
                        ex);
            }
        }


        try (Stream<Path> configFiles = Files.list(storageConfigFolder)) {
            for (var configFile : configFiles.toArray(Path[]::new)) {
                if (!configManager.isConfigFile(configFile)) continue;
                String id = configManager.getConfigName(configFile);

                StorageConfig storageConfig = configManager.loadConfig(configFile, StorageConfig.Base.class); // load superclass
                storageConfig = configManager.loadConfig(configFile, storageConfig.getStorageType().getConfigType()); // load actual config type

                storageConfigs.put(id, storageConfig);
            }
        } catch (IOException ex) {
            throw new ConfigurationException("BlueMap failed to read your map configuration from\n" +
                            storageConfigFolder.toAbsolutePath().normalize() + "\n" +
                            "Check if BlueMap has the permission to create and read from this folder.",
                    ex);
        }

        return storageConfigs;
    }

    private String sanitiseMapId(String id) {
        return id.replaceAll("\\W", "_");
    }

    private ConfigTemplate createOverworldMapTemplate(String name, Path worldFolder, Key dimension, int index) throws IOException {
        return configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/maps/map.conf")
                .setVariable("name", name)
                .setVariable("sorting", "" + index)
                .setVariable("world", formatPath(worldFolder))
                .setVariable("dimension", dimension.getFormatted())
                .setVariable("sky-color", "#7dabff")
                .setVariable("void-color", "#000000")
                .setVariable("ambient-light", "0.1")
                .setVariable("remove-caves-below-y", "55")
                .setConditional("max-y-comment", true)
                .setVariable("max-y", "100");
    }

    private ConfigTemplate createNetherMapTemplate(String name, Path worldFolder, Key dimension, int index) throws IOException {
        return configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/maps/map.conf")
                .setVariable("name", name)
                .setVariable("sorting", "" + (100 + index))
                .setVariable("world", formatPath(worldFolder))
                .setVariable("dimension", dimension.getFormatted())
                .setVariable("sky-color", "#290000")
                .setVariable("void-color", "#150000")
                .setVariable("ambient-light", "0.6")
                .setVariable("remove-caves-below-y", "-10000")
                .setConditional("max-y-comment", false)
                .setVariable("max-y", "90");
    }

    private ConfigTemplate createEndMapTemplate(String name, Path worldFolder, Key dimension, int index) throws IOException {
        return configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/maps/map.conf")
                .setVariable("name", name)
                .setVariable("sorting", "" + (200 + index))
                .setVariable("world", formatPath(worldFolder))
                .setVariable("dimension", dimension.getFormatted())
                .setVariable("sky-color", "#080010")
                .setVariable("void-color", "#080010")
                .setVariable("ambient-light", "0.6")
                .setVariable("remove-caves-below-y", "-10000")
                .setConditional("max-y-comment", true)
                .setVariable("max-y", "100");
    }

    private String formatPath(Path path) {
        // normalize path
        path = Path.of("")
                .toAbsolutePath()
                .relativize(path.toAbsolutePath())
                .normalize();
        String pathString = path.toString();

        String formatted = pathString;
        String separator = FileSystems.getDefault().getSeparator();

        // try to replace separator with standardized forward slash
        if (!separator.equals("/"))
            formatted = pathString.replace(separator, "/");

        // sanity check forward slash compatibility
        if (!Path.of(formatted).equals(path))
            formatted = pathString;

        // escape all backslashes
        formatted = formatted.replace("\\", "\\\\");

        return formatted;
    }

}
