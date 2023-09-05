package de.bluecolored.bluemap.core.mca.resourcepack;

import de.bluecolored.bluemap.api.debug.DebugDump;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@DebugDump
public class DimensionType {

    private static final DimensionType OVERWORLD = new DimensionType();
    private static final DimensionType NETHER = new DimensionType(
            true,
            false,
            8.0,
            false,
            true,
            0.1f,
            128,
            0,
            256,
            "#minecraft:infiniburn_nether",
            "minecraft:the_nether"
    );
    private static final DimensionType END = new DimensionType(
            false,
            false,
            1.0,
            false,
            false,
            0,
            256,
            0,
            256,
            "#minecraft:infiniburn_end",
            "minecraft:the_end"
    );
    private static final DimensionType OVERWORLD_CAVES = new DimensionType(
            false,
            true,
            1.0,
            true,
            true,
            0,
            256,
            -64,
            384,
            "#minecraft:infiniburn_overworld",
            "minecraft:overworld"
    );

    private boolean ultrawarm = false;
    private boolean natural = true;
    private double coordinateScale = 1.0;
    private boolean hasSkylight = true;
    private boolean hasCeiling = false;
    private float ambientLight = 0;
    private int logicalHeight = 256;
    private int minY = -64;
    private int height = 384;
    private String infiniburn = "#minecraft:infiniburn_overworld";
    private String effects = "minecraft:overworld";

}
