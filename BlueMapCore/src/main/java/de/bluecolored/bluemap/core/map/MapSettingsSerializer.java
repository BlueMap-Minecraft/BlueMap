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
package de.bluecolored.bluemap.core.map;

import com.flowpowered.math.vector.Vector2i;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.bluecolored.bluemap.core.map.lowres.LowresTileManager;
import de.bluecolored.bluemap.core.util.math.Color;

import java.lang.reflect.Type;

public class MapSettingsSerializer implements JsonSerializer<BmMap> {

    @Override
    public JsonElement serialize(BmMap map, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject root = new JsonObject();

        // name
        root.addProperty("name", map.getName());

        // sorting
        root.addProperty("sorting", map.getMapSettings().getSorting());

        // hires
        Vector2i hiresTileSize = map.getHiresModelManager().getTileGrid().getGridSize();
        Vector2i gridOrigin = map.getHiresModelManager().getTileGrid().getOffset();

        JsonObject hires = new JsonObject();
        hires.add("tileSize", context.serialize(hiresTileSize));
        hires.add("scale", context.serialize(Vector2i.ONE));
        hires.add("translate", context.serialize(gridOrigin));
        root.add("hires", hires);

        // lowres
        LowresTileManager lowresTileManager = map.getLowresTileManager();

        JsonObject lowres = new JsonObject();
        lowres.add("tileSize", context.serialize(lowresTileManager.getTileGrid().getGridSize()));
        lowres.add("lodFactor", context.serialize(lowresTileManager.getLodFactor()));
        lowres.add("lodCount", context.serialize(lowresTileManager.getLodCount()));
        root.add("lowres", lowres);

        // startPos
        Vector2i startPos = map.getMapSettings().getStartPos()
                .orElse(map.getWorld().getSpawnPoint().toVector2(true));
        root.add("startPos", context.serialize(startPos));

        // skyColor
        Color skyColor = new Color().parse(map.getMapSettings().getSkyColor());
        root.add("skyColor", context.serialize(skyColor));

        // ambientLight
        root.addProperty("ambientLight", map.getMapSettings().getAmbientLight());

        return root;
    }

}
