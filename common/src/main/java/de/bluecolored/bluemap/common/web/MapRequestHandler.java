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

import de.bluecolored.bluemap.common.web.http.HttpResponse;
import de.bluecolored.bluemap.common.web.http.HttpStatusCode;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.storage.MapStorage;
import org.jetbrains.annotations.Nullable;

import com.flowpowered.math.vector.Vector2i;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MapRequestHandler extends RoutingRequestHandler {

    private final SseConnectionManager sseConnections = new SseConnectionManager();

    public MapRequestHandler(
            BmMap map,
            @Nullable Supplier<String> livePlayersDataSupplier,
            @Nullable Supplier<String> liveMarkerDataSupplier,
            boolean useSSE
    ) {
        this(map.getStorage(), livePlayersDataSupplier, liveMarkerDataSupplier, useSSE);

        if (useSSE) {
            map.getHiresModelManager().addTileUpdateListener(tile -> onTileUpdate(tile, 0));
            map.getLowresTileManager().addTileUpdateListener(this::onTileUpdate);
        }
    }

    public MapRequestHandler(MapStorage mapStorage) {
        this(mapStorage, null, null, false);
    }

    public MapRequestHandler(
            MapStorage mapStorage,
            @Nullable Supplier<String> livePlayersDataSupplier,
            @Nullable Supplier<String> liveMarkerDataSupplier,
            boolean useSSE
    ) {
        register(".*", new MapStorageRequestHandler(mapStorage));

        if (useSSE) {
            register("live/sse", "", _ -> {
                HttpResponse response = new HttpResponse(HttpStatusCode.OK);
                response.addHeader("Content-Type", "text/event-stream");
                response.addHeader("Cache-Control", "no-cache");

                // attempt to turn off buffering in upstream proxy
                response.addHeader("X-Accel-Buffering", "no");

                try {
                    response.setBody(sseConnections.openConnection());
                } catch (IOException e) {
                    return new HttpResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
                }
                return response;
            });
        }

        if (livePlayersDataSupplier != null) {
            LiveDataSupplierBroadcaster<String> playerDataBroadcaster = new LiveDataSupplierBroadcaster<>(livePlayersDataSupplier, 1000);
            if (useSSE) registerSseCallback(playerDataBroadcaster, this::onPlayerUpdate);
            register("live/players\\.json", "", new JsonDataRequestHandler(playerDataBroadcaster));
        }

        if (liveMarkerDataSupplier != null) {
            LiveDataSupplierBroadcaster<String>markerDataBroadcaster = new LiveDataSupplierBroadcaster<>(liveMarkerDataSupplier, 10000);
            if (useSSE) registerSseCallback(markerDataBroadcaster, this::onMarkerUpdate);
            register("live/markers\\.json", "", new JsonDataRequestHandler(markerDataBroadcaster));
        }
    }

    /**
     * Helper function to subscribe to updates from a broadcaster (forcing it to auto-refresh)
     * only if the SSE manager has a connection.
     */
    private void registerSseCallback(LiveDataSupplierBroadcaster<String> broadcaster, Consumer<String> callback){
        sseConnections.addHasConnectionsListener(hasConnections -> {
            if (hasConnections) {
                broadcaster.addUpdateListener(callback);
            } else {
                broadcaster.removeUpdateListener(callback);
            }
        });
    }

    private void onTileUpdate(Vector2i tile, int lod) {
        // since the data is all ints there's no escaping issues so just build the JSON the hacky fast way
        sseConnections.broadcast("tile", "{\"x\":" + tile.getX() + ",\"y\":" + tile.getY() + ",\"lod\":" + lod + "}");
    }

    private void onPlayerUpdate(String data) {
        sseConnections.broadcast("player", data);
    }

    private void onMarkerUpdate(String data) {
        sseConnections.broadcast("marker", data);
    }

}
