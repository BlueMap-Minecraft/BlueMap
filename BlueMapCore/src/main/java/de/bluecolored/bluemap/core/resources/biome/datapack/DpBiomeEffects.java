package de.bluecolored.bluemap.core.resources.biome.datapack;

import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.Biome;

@SuppressWarnings("FieldMayBeFinal")
public class DpBiomeEffects {

    private Color water_color = Biome.DEFAULT.getWaterColor();
    private Color foliage_color = Biome.DEFAULT.getOverlayFoliageColor();
    private Color grass_color = Biome.DEFAULT.getOverlayGrassColor();

    public Color getWaterColor() {
        return water_color;
    }

    public Color getFoliageColor() {
        return foliage_color;
    }

    public Color getGrassColor() {
        return grass_color;
    }

}
