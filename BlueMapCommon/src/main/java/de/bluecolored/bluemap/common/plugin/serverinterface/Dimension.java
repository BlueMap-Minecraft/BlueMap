package de.bluecolored.bluemap.common.plugin.serverinterface;

import java.nio.file.Path;

public enum Dimension {

    OVERWORLD ("Overworld", Path.of("")),
    NETHER ("Nether", Path.of("DIM-1")),
    END ("End", Path.of("DIM1"));

    private final String name;
    private final Path dimensionSubPath;

    Dimension(String name, Path dimensionSubPath) {
        this.name = name;
        this.dimensionSubPath = dimensionSubPath;
    }

    public String getName() {
        return name;
    }

    public Path getDimensionSubPath() {
        return dimensionSubPath;
    }

}
