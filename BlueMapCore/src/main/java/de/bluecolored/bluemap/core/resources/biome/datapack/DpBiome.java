package de.bluecolored.bluemap.core.resources.biome.datapack;

import de.bluecolored.bluemap.core.world.Biome;

@SuppressWarnings("FieldMayBeFinal")
public class DpBiome {

    private DpBiomeEffects effects = new DpBiomeEffects();
    private float temperature = Biome.DEFAULT.getTemp();
    private float downfall = Biome.DEFAULT.getHumidity();

    public Biome createBiome(String formatted) {
        return new Biome(
                formatted,
                downfall,
                temperature,
                effects.getWaterColor(),
                effects.getFoliageColor(),
                effects.getGrassColor()
        );
    }

    public DpBiomeEffects getEffects() {
        return effects;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getDownfall() {
        return downfall;
    }

}
