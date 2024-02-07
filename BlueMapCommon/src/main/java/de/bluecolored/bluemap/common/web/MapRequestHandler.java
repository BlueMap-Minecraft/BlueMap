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
package de.bluecolored.bluemap.common.web;

import de.bluecolored.bluemap.common.config.PluginConfig;
import de.bluecolored.bluemap.common.live.LiveMarkersDataSupplier;
import de.bluecolored.bluemap.common.live.LivePlayersDataSupplier;
import de.bluecolored.bluemap.common.serverinterface.Server;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.storage.Storage;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MapRequestHandler extends RoutingRequestHandler {

    public MapRequestHandler(BmMap map, Server serverInterface, PluginConfig pluginConfig, Predicate<UUID> playerFilter) {
        this(map.getId(), map.getStorage(),
                createPlayersDataSupplier(map, serverInterface, pluginConfig, playerFilter),
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
            register("live/players\\.json", "", new JsonDataRequestHandler(
                    new CachedRateLimitDataSupplier(livePlayersDataSupplier,1000)
            ));
        }

        if (liveMarkerDataSupplier != null) {
            register("live/markers\\.json", "", new JsonDataRequestHandler(
                    new CachedRateLimitDataSupplier(liveMarkerDataSupplier,10000)
            ));
        }
    }

    private static @Nullable LivePlayersDataSupplier createPlayersDataSupplier(BmMap map, Server serverInterface, PluginConfig pluginConfig, Predicate<UUID> playerFilter) {
        ServerWorld world = serverInterface.getServerWorld(map.getWorld()).orElse(null);
        if (world == null) return null;
        return new LivePlayersDataSupplier(serverInterface, pluginConfig, world, playerFilter);
    }

}
