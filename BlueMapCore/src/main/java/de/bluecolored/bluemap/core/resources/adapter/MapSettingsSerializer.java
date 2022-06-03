package de.bluecolored.bluemap.core.resources.adapter;

import com.flowpowered.math.vector.Vector2i;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.util.ConfigUtils;
import de.bluecolored.bluemap.core.util.math.Color;

import java.lang.reflect.Type;

public class MapSettingsSerializer implements JsonSerializer<BmMap> {

    @Override
    public JsonElement serialize(BmMap map, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject root = new JsonObject();

        // name
        root.addProperty("name", map.getName());

        // hires
        Vector2i hiresTileSize = map.getHiresModelManager().getTileGrid().getGridSize();
        Vector2i gridOrigin = map.getHiresModelManager().getTileGrid().getOffset();
        Vector2i lowresTileSize = map.getLowresModelManager().getTileSize();
        Vector2i lowresPointsPerHiresTile = map.getLowresModelManager().getPointsPerHiresTile();

        JsonObject hires = new JsonObject();
        hires.add("tileSize", context.serialize(hiresTileSize));
        hires.add("scale", context.serialize(Vector2i.ONE));
        hires.add("translate", context.serialize(gridOrigin));
        root.add("hires", hires);

        // lowres
        Vector2i pointSize = hiresTileSize.div(lowresPointsPerHiresTile);
        Vector2i tileSize = pointSize.mul(lowresTileSize);

        JsonObject lowres = new JsonObject();
        lowres.add("tileSize", context.serialize(tileSize));
        lowres.add("scale", context.serialize(pointSize));
        lowres.add("translate", context.serialize(pointSize.div(2)));
        root.add("lowres", lowres);

        // startPos
        Vector2i startPos = map.getMapSettings().getStartPos()
                .orElse(map.getWorld().getSpawnPoint().toVector2(true));
        root.add("startPos", context.serialize(startPos));

        // skyColor
        Color skyColor = new Color().set(ConfigUtils.parseColorFromString(map.getMapSettings().getSkyColor()));
        root.add("skyColor", context.serialize(skyColor));

        // ambientLight
        root.addProperty("ambientLight", map.getMapSettings().getAmbientLight());

        return root;
    }

}
