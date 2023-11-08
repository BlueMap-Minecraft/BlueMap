package de.bluecolored.bluemap.core.world.mca.data;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@SuppressWarnings("FieldMayBeFinal")
public class LevelData {

    private Data data = new Data();

    @Getter
    public static class Data {
        private String levelName = "world";
        private int spawnX = 0, spawnY = 0, spawnZ = 0;
        private WGSettings worldGenSettings = new WGSettings();
    }

    @Getter
    public static class WGSettings {
        private Map<String, Dimension> dimensions = new HashMap<>();
    }

    @Getter
    public static class Dimension {
        private String type = "minecraft:overworld";
    }

}
