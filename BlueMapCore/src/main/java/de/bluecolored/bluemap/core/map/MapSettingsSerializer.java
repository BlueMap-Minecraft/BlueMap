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
