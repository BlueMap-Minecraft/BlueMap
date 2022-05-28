package de.bluecolored.bluemap.core.resources.biome;

import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.Biome;

@SuppressWarnings("FieldMayBeFinal")
public class BiomeConfigEntry {

    private float humidity = Biome.DEFAULT.getHumidity();
    private float temperature = Biome.DEFAULT.getTemp();
    private Color watercolor = Biome.DEFAULT.getWaterColor();
    private Color grasscolor = new Color();
    private Color foliagecolor = new Color();

    public Biome createBiome(String formatted) {
        return new Biome(
                formatted,
                humidity,
                temperature,
                watercolor.premultiplied(),
                foliagecolor.premultiplied(),
                grasscolor.premultiplied()
        );
    }

    public float getHumidity() {
        return humidity;
    }

    public float getTemperature() {
        return temperature;
    }

    public Color getWatercolor() {
        return watercolor;
    }

    public Color getGrasscolor() {
        return grasscolor;
    }

    public Color getFoliagecolor() {
        return foliagecolor;
    }

}
