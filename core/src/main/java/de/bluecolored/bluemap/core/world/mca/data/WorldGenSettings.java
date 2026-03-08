package de.bluecolored.bluemap.core.world.mca.data;

import de.bluecolored.bluemap.core.world.DimensionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@SuppressWarnings("FieldMayBeFinal")
public class WorldGenSettings {

    private Data data = new Data();

    @Getter
    public static class Data {

        Map<String, DimensionSettings> dimensions = new HashMap<>();

    }

}
