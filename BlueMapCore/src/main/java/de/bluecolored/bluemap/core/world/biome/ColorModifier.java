package de.bluecolored.bluemap.core.world.biome;

import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.block.Block;

public interface ColorModifier {

    void apply(Block<?> block, Color color);

}
