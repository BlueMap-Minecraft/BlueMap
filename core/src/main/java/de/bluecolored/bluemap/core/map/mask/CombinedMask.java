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
package de.bluecolored.bluemap.core.map.mask;

import de.bluecolored.bluemap.core.util.Tristate;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CombinedMask implements Mask {

    private final List<MaskLayer> layers = new ArrayList<>();

    public void add(Mask mask, boolean value) {
        if (!value && layers.isEmpty())
            layers.add(new MaskLayer(ALL, true));
        layers.add(new MaskLayer(mask, value));
    }

    @Override
    public boolean test(int x, int y, int z) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            MaskLayer layer = layers.get(i);
            if (!layer.mask.test(x, y, z)) continue;
            return layer.value;
        }
        return layers.isEmpty();
    }

    @Override
    public Tristate test(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            MaskLayer layer = layers.get(i);
            Tristate result = layer.mask.test(minX, minY, minZ, maxX, maxY, maxZ);
            if (result == Tristate.FALSE) continue;
            if (result == Tristate.UNDEFINED) return Tristate.UNDEFINED;
            return Tristate.valueOf(layer.value);
        }
        return Tristate.valueOf(layers.isEmpty());
    }

    @Override
    public Mask submask(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        Tristate test = test(minX, minY, minZ, maxX, maxY, maxZ);
        if (test == Tristate.TRUE) return ALL;
        if (test == Tristate.FALSE) return NONE;

        CombinedMask optimized = new CombinedMask();
        for (MaskLayer layer : layers) {
            if (!optimized.layers.isEmpty() && layer.mask.test(minX, minY, minZ, maxX, maxY, maxZ) == Tristate.FALSE) continue;
            optimized.add(layer.mask.submask(minX, minY, minZ, maxX, maxY, maxZ), layer.value);
        }
        return optimized;
    }

    @Override
    public boolean isEdge(int minX, int minZ, int maxX, int maxZ) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            MaskLayer layer = layers.get(i);
            if (layer.mask.isEdge(minX, minZ, maxX, maxZ)) return true;
        }
        return false;
    }

    public int size() {
        return layers.size();
    }

    private record MaskLayer(Mask mask, boolean value) {}

}
