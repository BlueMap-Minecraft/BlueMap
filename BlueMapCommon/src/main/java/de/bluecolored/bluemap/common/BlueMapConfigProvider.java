package de.bluecolored.bluemap.common;

import de.bluecolored.bluemap.common.config.*;
import de.bluecolored.bluemap.common.config.storage.StorageConfig;

import java.util.Map;

public interface BlueMapConfigProvider {
    CoreConfig getCoreConfig();

    WebappConfig getWebappConfig();

    WebserverConfig getWebserverConfig();

    PluginConfig getPluginConfig();

    Map<String, MapConfig> getMapConfigs();

    Map<String, StorageConfig> getStorageConfigs();

}
