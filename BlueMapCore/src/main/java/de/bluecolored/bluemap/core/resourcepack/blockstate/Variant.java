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
package de.bluecolored.bluemap.core.resourcepack.blockstate;

import de.bluecolored.bluemap.core.resourcepack.ParseResourceException;
import de.bluecolored.bluemap.core.resourcepack.blockmodel.TransformedBlockModelResource;

import java.util.ArrayList;
import java.util.Collection;

public class Variant {

    PropertyCondition condition = PropertyCondition.all();
    Collection<Weighted<TransformedBlockModelResource>> models = new ArrayList<>(0);

    private double totalWeight;

    Variant() {}

    public TransformedBlockModelResource getModel(int x, int y, int z) {
        if (models.isEmpty()) throw new IllegalStateException("A variant must have at least one model!");

        double selection = hashToFloat(x, y, z) * totalWeight; // random based on position
        for (Weighted<TransformedBlockModelResource> w : models) {
            selection -= w.getWeight();
            if (selection <= 0) return w.getValue();
        }

        throw new RuntimeException("This line should never be reached!");
    }

    public void checkValid() throws ParseResourceException {
        if (models.isEmpty()) throw new ParseResourceException("A variant must have at least one model!");
    }

    public void updateTotalWeight() {
        totalWeight = 0d;
        for (Weighted<?> w : models) {
            totalWeight += w.getWeight();
        }
    }

    private static float hashToFloat(int x, int y, int z) {
        final long hash = x * 73438747 ^ y * 9357269 ^ z * 4335792;
        return (hash * (hash + 456149) & 0x00ffffff) / (float) 0x01000000;
    }

}
