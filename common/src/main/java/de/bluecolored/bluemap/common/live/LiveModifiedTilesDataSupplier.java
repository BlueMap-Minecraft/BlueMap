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
import de.bluecolored.bluemap.core.logger.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Supplier;

/**
 * Legacy supplier for a JSON document describing recently rendered tiles
 * for a given map.
 *
 * This is kept for backwards compatibility but is no longer used by the
 * default HTTP routing, which instead uses {@link LiveModifiedTilesRequestHandler}
 * and the versioned {@link LiveModifiedTilesRegistry}.
 */
public class LiveModifiedTilesDataSupplier implements Supplier<String> {

    private final String mapId;

    public LiveModifiedTilesDataSupplier(String mapId) {
        this.mapId = mapId;
    }

    @Override
    public String get() {
        try (StringWriter jsonString = new StringWriter();
                JsonWriter json = new JsonWriter(jsonString)) {

            json.beginObject();
            json.name("tiles").beginArray();

            List<LiveModifiedTilesRegistry.Tile> tiles = LiveModifiedTilesRegistry.getSince(mapId, 0L).tiles();
            for (LiveModifiedTilesRegistry.Tile tile : tiles) {
                json.beginObject();
                json.name("lod").value(0); // hires tiles are lod 0 in the webapp
                json.name("x").value(tile.x());
                json.name("z").value(tile.z());
                json.endObject();
            }

            json.endArray();
            json.endObject();

            json.flush();
            return jsonString.toString();
        } catch (IOException ex) {
            Logger.global.logError("Failed to write live/modified-chunks json!", ex);
            return "BlueMap - Exception handling this request";
        }
    }
}
