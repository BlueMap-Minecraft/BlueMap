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

import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.common.web.http.HttpRequest;
import de.bluecolored.bluemap.common.web.http.HttpRequestHandler;
import de.bluecolored.bluemap.common.web.http.HttpResponse;
import de.bluecolored.bluemap.common.web.http.HttpStatusCode;
import de.bluecolored.bluemap.core.logger.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * HTTP handler for the live modified-tiles endpoint.
 *
 * It accepts an optional {@code since} query parameter specifying the last
 * version the client has seen and returns all tiles with a higher version as
 * well as the current version for the map.
 */
public class LiveModifiedTilesRequestHandler implements HttpRequestHandler {

    private final String mapId;

    public LiveModifiedTilesRequestHandler(String mapId) {
        this.mapId = mapId;
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

            for (LiveModifiedTilesRegistry.Tile tile : result.tiles()) {
                json.beginObject();
                json.name("lod").value(0); // hires tiles are lod 0 in the webapp
                json.name("x").value(tile.x());
                json.name("z").value(tile.z());
                json.endObject();
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
}
