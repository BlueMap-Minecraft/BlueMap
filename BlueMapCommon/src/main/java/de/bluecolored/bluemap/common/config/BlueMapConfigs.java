package de.bluecolored.bluemap.common.config;

import de.bluecolored.bluemap.common.config.storage.StorageConfig;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.logger.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@DebugDump
public class BlueMapConfigs {

    private final ServerInterface serverInterface;
    private final ConfigManager configManager;

    private final CoreConfig coreConfig;
    private final WebserverConfig webserverConfig;
    private final WebappConfig webappConfig;
    private final PluginConfig pluginConfig;
    private final Map<String, MapConfig> mapConfigs;
    private final Map<String, StorageConfig> storageConfigs;

    public BlueMapConfigs(ServerInterface serverInterface) throws ConfigurationException {
        this.serverInterface = serverInterface;
        this.configManager = new ConfigManager(serverInterface.getConfigFolder());

        this.coreConfig = loadCoreConfig();
        this.webserverConfig = loadWebserverConfig();
        this.webappConfig = loadWebappConfig();
        this.pluginConfig = loadPluginConfig();
        this.storageConfigs = Collections.unmodifiableMap(loadStorageConfigs());
        this.mapConfigs = Collections.unmodifiableMap(loadMapConfigs());
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public CoreConfig getCoreConfig() {
        return coreConfig;
    }

    public WebappConfig getWebappConfig() {
        return webappConfig;
    }

    public WebserverConfig getWebserverConfig() {
        return webserverConfig;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public Map<String, MapConfig> getMapConfigs() {
        return mapConfigs;
    }

    public Map<String, StorageConfig> getStorageConfigs() {
        return storageConfigs;
    }

    private synchronized CoreConfig loadCoreConfig() throws ConfigurationException {
        Path configFileRaw = Path.of("core");
        Path configFile = configManager.findConfigPath(configFileRaw);
        Path configFolder = configFile.getParent();

        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configFolder);
                Files.writeString(
                        configFolder.resolve("core.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/core.conf")
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                Logger.global.logWarning("Failed to create default core-configuration-file: " + ex);
            }
        }

        return configManager.loadConfig(configFileRaw, CoreConfig.class);
    }

    private synchronized WebserverConfig loadWebserverConfig() throws ConfigurationException {
        Path configFileRaw = Path.of("webserver");
        Path configFile = configManager.findConfigPath(configFileRaw);
        Path configFolder = configFile.getParent();

        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configFolder);
                Files.writeString(
                        configFolder.resolve("webserver.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/webserver.conf")
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                Logger.global.logWarning("Failed to create default webserver-configuration-file: " + ex);
            }
        }

        return configManager.loadConfig(configFileRaw, WebserverConfig.class);
    }

    private synchronized WebappConfig loadWebappConfig() throws ConfigurationException {
        Path configFileRaw = Path.of("webapp");
        Path configFile = configManager.findConfigPath(configFileRaw);
        Path configFolder = configFile.getParent();

        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configFolder);
                Files.writeString(
                        configFolder.resolve("webapp.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/webapp.conf")
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                Logger.global.logWarning("Failed to create default webapp-configuration-file: " + ex);
            }
        }

        return configManager.loadConfig(configFileRaw, WebappConfig.class);
    }

    private synchronized PluginConfig loadPluginConfig() throws ConfigurationException {
        Path configFileRaw = Path.of("plugin");
        Path configFile = configManager.findConfigPath(configFileRaw);
        Path configFolder = configFile.getParent();

        if (!Files.exists(configFile)) {
            try {
                Files.createDirectories(configFolder);
                Files.writeString(
                        configFolder.resolve("plugin.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/plugin.conf")
                                .build(),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                );
            } catch (IOException | NullPointerException ex) {
                Logger.global.logWarning("Failed to create default webapp-configuration-file: " + ex);
            }
        }

        return configManager.loadConfig(configFileRaw, PluginConfig.class);
    }

    private synchronized Map<String, MapConfig> loadMapConfigs() throws ConfigurationException {
        Map<String, MapConfig> mapConfigs = new HashMap<>();

        Path mapFolder = Paths.get("maps");
        Path mapConfigFolder = configManager.getConfigRoot().resolve(mapFolder);

        if (!Files.exists(mapConfigFolder)){
            try {
                Files.createDirectories(mapConfigFolder);
                var worlds = serverInterface.getLoadedWorlds();
                if (worlds.isEmpty()) {
                    Files.writeString(
                            mapConfigFolder.resolve("overworld.conf"),
                            createOverworldMapTemplate("Overworld", Path.of("world")).build(),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                    );
                    Files.writeString(
                            mapConfigFolder.resolve("nether.conf"),
                            createNetherMapTemplate("Nether", Path.of("world", "DIM-1")).build(),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                    );
                    Files.writeString(
                            mapConfigFolder.resolve("end.conf"),
                            createEndMapTemplate("End", Path.of("world", "DIM1")).build(),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                    );
                } else {
                    for (var world : worlds) {
                        String name = world.getName().orElse(world.getDimension().getName());
                        Path worldFolder = world.getSaveFolder();
                        ConfigTemplate template;
                        switch (world.getDimension()) {
                            case NETHER: template = createNetherMapTemplate(name, worldFolder); break;
                            case END: template = createEndMapTemplate(name, worldFolder); break;
                            default: template = createOverworldMapTemplate(name, worldFolder); break;
                        }

                        Path configFile = mapConfigFolder.resolve(sanitiseMapId(name.toLowerCase(Locale.ROOT)) + ".conf");
                        int i = 1;
                        while (Files.exists(configFile)) {
                            configFile = mapConfigFolder.resolve(sanitiseMapId(name.toLowerCase(Locale.ROOT)) + '_' + (i++) + ".conf");
                        }

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

        try {
            for (var configFile : Files.list(mapConfigFolder).toArray(Path[]::new)) {
                if (!configManager.isConfigFile(configFile)) continue;
                Path rawConfig = configManager.getRaw(configFile);
                String id = sanitiseMapId(rawConfig.getFileName().toString());

                if (mapConfigs.containsKey(id)) {
                    throw new ConfigurationException("At least two of your map-config file-names result in ambiguous map-id's!\n" +
                            configFile.toAbsolutePath().normalize() + "\n" +
                            "To resolve this issue, rename this file to something else.");
                }

                MapConfig mapConfig = configManager.loadConfig(rawConfig, MapConfig.class);
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

    private synchronized Map<String, StorageConfig> loadStorageConfigs() throws ConfigurationException {
        Map<String, StorageConfig> storageConfigs = new HashMap<>();

        Path storageFolder = Paths.get("storages");
        Path storageConfigFolder = configManager.getConfigRoot().resolve(storageFolder);

        if (!Files.exists(storageConfigFolder)){
            try {
                Files.createDirectories(storageConfigFolder);
                Files.writeString(
                        storageConfigFolder.resolve("file.conf"),
                        configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/storages/file.conf").build(),
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


        try {
            for (var configFile : Files.list(storageConfigFolder).toArray(Path[]::new)) {
                if (!configManager.isConfigFile(configFile)) continue;
                Path rawConfig = configManager.getRaw(configFile);
                String id = rawConfig.getFileName().toString();

                StorageConfig storageConfig = configManager.loadConfig(rawConfig, StorageConfig.class); // load superclass
                storageConfig = configManager.loadConfig(rawConfig, storageConfig.getStorageType().getConfigType()); // load actual config type

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
        return id.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private ConfigTemplate createOverworldMapTemplate(String name, Path worldFolder) throws IOException {
        return configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/maps/map.conf")
                .setVariable("name", name)
                .setVariable("world", formatPath(worldFolder))
                .setVariable("sky-color", "#7dabff")
                .setVariable("ambient-light", "0.1")
                .setVariable("world-sky-light", "15")
                .setVariable("remove-caves-below-y", "55")
                .setConditional("max-y-comment", true)
                .setVariable("max-y", "100");
    }

    private ConfigTemplate createNetherMapTemplate(String name, Path worldFolder) throws IOException {
        return configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/maps/map.conf")
                .setVariable("name", name)
                .setVariable("world", formatPath(worldFolder))
                .setVariable("sky-color", "#290000")
                .setVariable("ambient-light", "0.6")
                .setVariable("world-sky-light", "0")
                .setVariable("remove-caves-below-y", "-10000")
                .setConditional("max-y-comment", false)
                .setVariable("max-y", "90");
    }

    private ConfigTemplate createEndMapTemplate(String name, Path worldFolder) throws IOException {
        return configManager.loadConfigTemplate("/de/bluecolored/bluemap/config/maps/map.conf")
                .setVariable("name", name)
                .setVariable("world", formatPath(worldFolder))
                .setVariable("sky-color", "#080010")
                .setVariable("ambient-light", "0.6")
                .setVariable("world-sky-light", "0")
                .setVariable("remove-caves-below-y", "-10000")
                .setConditional("max-y-comment", true)
                .setVariable("max-y", "100");
    }

    private String formatPath(Path path) {
        return Path.of("")
                .toAbsolutePath()
                .relativize(path.toAbsolutePath())
                .normalize()
                .toString()
                .replace("\\", "\\\\");
    }

}
