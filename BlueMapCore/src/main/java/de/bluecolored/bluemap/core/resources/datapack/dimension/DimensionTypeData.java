package de.bluecolored.bluemap.core.resources.datapack.dimension;

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.world.DimensionType;
import lombok.*;
import lombok.experimental.Accessors;

@Data
@DebugDump
public class DimensionTypeData implements DimensionType {

    private boolean natural;
    @Accessors(fluent = true) private boolean hasSkylight;
    @Accessors(fluent = true) private boolean hasCeiling;
    private float ambientLight;
    private int minY;
    private int height;
    private Long fixedTime;
    private double coordinateScale;

}
