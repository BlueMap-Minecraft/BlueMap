package de.bluecolored.bluemap.core.map;

import de.bluecolored.bluemap.core.util.math.Color;

@FunctionalInterface
public interface TileMetaConsumer {

    void set(int x, int z, Color color, int height, int blockLight);

}
