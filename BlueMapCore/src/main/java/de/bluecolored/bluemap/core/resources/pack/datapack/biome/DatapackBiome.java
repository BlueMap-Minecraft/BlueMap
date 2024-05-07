package de.bluecolored.bluemap.core.resources.pack.datapack.biome;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.Biome;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class DatapackBiome implements Biome {

    private final Key key;
    private final Data data;

    @Override
    public float getDownfall() {
        return data.downfall;
    }

    @Override
    public float getTemperature() {
        return data.temperature;
    }

    @Override
    public Color getWaterColor() {
        return data.effects.waterColor;
    }

    @Override
    public Color getOverlayFoliageColor() {
        return data.effects.foliageColor;
    }

    @Override
    public Color getOverlayGrassColor() {
        return data.effects.grassColor;
    }

    @SuppressWarnings("FieldMayBeFinal")
    @Getter
    public static class Data {

        private Effects effects = new Effects();
        private float temperature = Biome.DEFAULT.getTemperature();
        private float downfall = Biome.DEFAULT.getDownfall();

    }

    @SuppressWarnings("FieldMayBeFinal")
    @Getter
    public static class Effects {

        private Color waterColor = Biome.DEFAULT.getWaterColor();
        private Color foliageColor = Biome.DEFAULT.getOverlayFoliageColor();
        private Color grassColor = Biome.DEFAULT.getOverlayGrassColor();

    }

}
