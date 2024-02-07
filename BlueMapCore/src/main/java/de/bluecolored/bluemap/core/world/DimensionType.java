package de.bluecolored.bluemap.core.world;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

public interface DimensionType {

    DimensionType OVERWORLD = new Builtin(
            true,
            true,
            false,
            0f,
            -64,
            384,
            null,
            1.0
    );
    DimensionType OVERWORLD_CAVES = new Builtin(
            true,
            true,
            true,
            0,
            -64,
            384,
            null,
            1.0
    );
    DimensionType NETHER = new Builtin(
            false,
            false,
            true,
            0.1f,
            0,
            256,
            6000L,
            8.0
    );
    DimensionType END = new Builtin(
            false,
            false,
            false,
            0,
            0,
            256,
            18000L,
            1.0
    );

    boolean isNatural();

    boolean hasSkylight();

    boolean hasCeiling();

    float getAmbientLight();

    int getMinY();

    int getHeight();

    Long getFixedTime();

    double getCoordinateScale();

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    class Builtin implements DimensionType {

        private final boolean natural;
        @Accessors(fluent = true) private final boolean hasSkylight;
        @Accessors(fluent = true) private final boolean hasCeiling;
        private final float ambientLight;
        private final int minY;
        private final int height;
        private final Long fixedTime;
        private final double coordinateScale;

    }

}
