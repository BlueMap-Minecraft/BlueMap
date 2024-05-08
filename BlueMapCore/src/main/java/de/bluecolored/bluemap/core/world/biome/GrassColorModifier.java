package de.bluecolored.bluemap.core.world.biome;

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.Keyed;
import de.bluecolored.bluemap.core.util.Registry;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.block.Block;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public interface GrassColorModifier extends Keyed, ColorModifier {

    GrassColorModifier NONE = new Impl(Key.minecraft("none"), (Block<?> block, Color color) -> {});
    GrassColorModifier DARK_FOREST = new Impl(Key.minecraft("dark_forest"), (Block<?> block, Color color) ->
            color.set(((color.getInt() & 0xfefefe) + 0x28340a >> 1) | 0xff000000, true)
    );
    GrassColorModifier SWAMP = new Impl(Key.minecraft("swamp"), (Block<?> block, Color color) -> {
        color.set(0xff6a7039, true);

        /* Vanilla code with noise:
        double f = FOLIAGE_NOISE.sample(block.getX() * 0.0225, block.getZ() * 0.0225, false);

        if (f < -0.1) color.set(5011004)
        else color.set(6975545);
        */
    });

    Registry<GrassColorModifier> REGISTRY = new Registry<>(
            NONE,
            DARK_FOREST,
            SWAMP
    );

    @RequiredArgsConstructor
    @Getter
    class Impl implements GrassColorModifier {

        private final Key key;
        private final ColorModifier modifier;

        @Override
        public void apply(Block<?> block, Color color) {
            modifier.apply(block, color);
        }

    }

}
