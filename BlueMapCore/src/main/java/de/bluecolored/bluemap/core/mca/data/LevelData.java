package de.bluecolored.bluemap.core.mca.data;

import lombok.Getter;

@Getter
@SuppressWarnings("FieldMayBeFinal")
public class LevelData {

    private Data data = new Data();

    @Getter
    public static class Data {

        private String levelName = "world";
        private int spawnX = 0, spawnY = 0, spawnZ = 0;

    }

}
