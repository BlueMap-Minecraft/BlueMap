package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.common.config.PluginConfig;
import de.bluecolored.bluemap.common.live.LiveMarkersDataSupplier;
import de.bluecolored.bluemap.common.live.LivePlayersDataSupplier;
import de.bluecolored.bluemap.common.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.storage.Storage;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MapRequestHandler extends RoutingRequestHandler {

    public MapRequestHandler(BmMap map, ServerInterface serverInterface, PluginConfig pluginConfig, Predicate<UUID> playerFilter) {
        this(map.getId(), map.getStorage(),
                new LivePlayersDataSupplier(serverInterface, pluginConfig, map.getWorldId(), playerFilter),
                new LiveMarkersDataSupplier(map.getMarkerSets()));
    }

    public MapRequestHandler(String mapId, Storage mapStorage) {
        this(mapId, mapStorage, null, null);
    }

    public MapRequestHandler(String mapId, Storage mapStorage,
                             @Nullable Supplier<String> livePlayersDataSupplier,
                             @Nullable Supplier<String> liveMarkerDataSupplier) {

        register(".*", new MapStorageRequestHandler(mapId, mapStorage));

        if (livePlayersDataSupplier != null) {
            register("live/players", "", new JsonDataRequestHandler(
                    new CachedRateLimitDataSupplier(livePlayersDataSupplier,1000)
            ));
        }

        if (liveMarkerDataSupplier != null) {
            register("live/markers", "", new JsonDataRequestHandler(
                    new CachedRateLimitDataSupplier(liveMarkerDataSupplier,10000)
            ));
        }
    }

}
