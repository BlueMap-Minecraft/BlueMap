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
package de.bluecolored.bluemap.common.live;

import com.flowpowered.math.vector.Vector2i;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.common.web.http.HttpRequest;
import de.bluecolored.bluemap.common.web.http.HttpRequestHandler;
import de.bluecolored.bluemap.common.web.http.HttpResponse;
import de.bluecolored.bluemap.common.web.http.HttpStatusCode;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.map.lowres.LowresTileManager;
import de.bluecolored.bluemap.core.map.hires.HiresModelManager;
import de.bluecolored.bluemap.core.util.Grid;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HTTP handler for the live modified-tiles endpoint.
 *
 * It accepts an optional {@code since} query parameter specifying the last
 * version the client has seen and returns all tiles with a higher version as
 * well as the current version for the map.
 */
public class LiveModifiedTilesRequestHandler implements HttpRequestHandler {

    private final String mapId;
    private final Grid hiresGrid;
    private final List<Grid> lowresGrids;

    /**
     * Creates a handler that only reports hires tiles (lod 0).
     * <p>
     * This constructor is kept for backwards compatibility with older
     * integrations that do not provide a {@link BmMap} instance.
     */
    public LiveModifiedTilesRequestHandler(String mapId) {
        this.mapId = mapId;
        this.hiresGrid = null;
        this.lowresGrids = List.of();
    }

    /**
     * Creates a handler that reports both hires tiles (lod 0) and the
     * corresponding low-res tiles for all configured LOD levels.
     */
    public LiveModifiedTilesRequestHandler(BmMap map) {
        this.mapId = map.getId();

        HiresModelManager hiresModelManager = map.getHiresModelManager();
        LowresTileManager lowresTileManager = map.getLowresTileManager();

        this.hiresGrid = hiresModelManager != null ? hiresModelManager.getTileGrid() : null;

        if (lowresTileManager != null && this.hiresGrid != null) {
            Grid baseLowresGrid = lowresTileManager.getTileGrid();
            int lodCount = lowresTileManager.getLodCount();
            int lodFactor = lowresTileManager.getLodFactor();

            if (baseLowresGrid != null && lodCount > 0 && lodFactor > 0) {
                this.lowresGrids = new ArrayList<>(lodCount);

                for (int lod = 1; lod <= lodCount; lod++) {
                    int factor = (int) Math.pow(lodFactor, lod - 1);

                    Vector2i size = baseLowresGrid.getGridSize().mul(factor);
                    Vector2i offset = baseLowresGrid.getOffset();

                    this.lowresGrids.add(new Grid(size, offset));
                }
            } else {
                this.lowresGrids = List.of();
            }
        } else {
            this.lowresGrids = List.of();
        }
    }

    @Override
    public HttpResponse handle(HttpRequest request) {
        long sinceVersion = 0L;
        try {
            Map<String, String> params = request.getGETParams();
            String sinceParam = params.get("since");
            if (sinceParam != null && !sinceParam.isEmpty()) {
                long parsed = Long.parseLong(sinceParam);
                if (parsed > 0L)
                    sinceVersion = parsed;
            }
        } catch (NumberFormatException ignored) {
            // fall back to default sinceVersion = 0
        }

        LiveModifiedTilesRegistry.Result result = LiveModifiedTilesRegistry.getSince(mapId, sinceVersion);

        HttpResponse response = new HttpResponse(HttpStatusCode.OK);
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Content-Type", "application/json");

        try (StringWriter jsonString = new StringWriter();
                JsonWriter json = new JsonWriter(jsonString)) {

            json.beginObject();
            json.name("version").value(result.version());
            json.name("tiles").beginArray();

            // Track which (lod,x,z) triplets have already been emitted so
            // we don't spam duplicates when multiple hires-tiles map to the
            // same low-res tile.
            Set<String> emitted = new HashSet<>();

            for (LiveModifiedTilesRegistry.Tile tile : result.tiles()) {
                int x = tile.x();
                int z = tile.z();

                // Always include the hires tile itself as lod 0
                writeTile(json, emitted, 0, x, z);

                // If we don't have grid information (legacy constructor), we
                // can't reliably compute low-res tiles, so stop here.
                if (hiresGrid == null || lowresGrids.isEmpty())
                    continue;

                Vector2i hiresCell = new Vector2i(x, z);

                // For each configured low-res LOD, compute which low-res
                // tiles intersect this hires tile and mark them as modified
                // as well so the webapp can refresh them.
                for (int lodIndex = 0; lodIndex < lowresGrids.size(); lodIndex++) {
                    int lod = lodIndex + 1; // lod 1..n are lowres levels
                    Grid lowresGrid = lowresGrids.get(lodIndex);

                    for (Vector2i lowresCell : hiresGrid.getIntersecting(hiresCell, lowresGrid)) {
                        writeTile(json, emitted, lod, lowresCell.getX(), lowresCell.getY());
                    }
                }
            }

            json.endArray();
            json.endObject();

            json.flush();
            response.setData(jsonString.toString());
        } catch (IOException ex) {
            Logger.global.logError("Failed to write live/modified-chunks json!", ex);
            response = new HttpResponse(HttpStatusCode.INTERNAL_SERVER_ERROR);
            response.setData("BlueMap - Exception handling this request");
        }

        return response;
    }

    private void writeTile(JsonWriter json, Set<String> emitted, int lod, int x, int z) throws IOException {
        String key = lod + ":" + x + ":" + z;
        if (!emitted.add(key))
            return;

        json.beginObject();
        json.name("lod").value(lod);
        json.name("x").value(x);
        json.name("z").value(z);
        json.endObject();
    }
}
