package de.bluecolored.bluemap.core.world.mca.data;

import de.bluecolored.bluemap.api.debug.DebugDump;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@SuppressWarnings("FieldMayBeFinal")
@DebugDump
public class LevelData {

    private Data data = new Data();

    @Getter
    @DebugDump
    public static class Data {
        private String levelName = "world";
        private int spawnX = 0, spawnY = 0, spawnZ = 0;
        private WGSettings worldGenSettings = new WGSettings();
    }

    @Getter
    @DebugDump
    public static class WGSettings {
        private Map<String, Dimension> dimensions = new HashMap<>();
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @DebugDump
    public static class Dimension {
        private String type = "minecraft:overworld";
    }

}
