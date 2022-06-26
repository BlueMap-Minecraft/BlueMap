package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.common.config.PluginConfig;
import de.bluecolored.bluemap.common.live.LivePlayersDataSupplier;
import de.bluecolored.bluemap.common.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.storage.Storage;

public class MapRequestHandler extends RoutingRequestHandler {

    public MapRequestHandler(BmMap map, ServerInterface serverInterface, PluginConfig pluginConfig) {
        this(map.getId(), map.getWorldId(), map.getStorage(), serverInterface, pluginConfig);
    }

    public MapRequestHandler(String mapId, String worldId, Storage mapStorage, ServerInterface serverInterface, PluginConfig pluginConfig) {
        register(".*", new MapStorageRequestHandler(mapId, mapStorage));

        register("live/players", "", new JsonDataRequestHandler(
                new CachedRateLimitDataSupplier(
                        new LivePlayersDataSupplier(serverInterface, pluginConfig, worldId),
                        1000)
        ));
    }

}
