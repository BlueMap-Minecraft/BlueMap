/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
