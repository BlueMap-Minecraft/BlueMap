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
package de.bluecolored.bluemap.core.map.lowres;

import de.bluecolored.bluemap.core.map.TileMetaConsumer;
import de.bluecolored.bluemap.core.storage.MapStorage;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.util.math.Color;

public class LowresTileManager implements TileMetaConsumer {

    private final Grid tileGrid;
    private final int lodFactor, lodCount;

    private final LowresLayer[] layers;

    public LowresTileManager(MapStorage storage, Grid tileGrid, int lodCount, int lodFactor) {
        this.tileGrid = tileGrid;
        this.lodFactor = lodFactor;
        this.lodCount = lodCount;

        this.layers = new LowresLayer[lodCount];
        for (int i = lodCount - 1; i >= 0; i--) {
            this.layers[i] = new LowresLayer(storage.lowresTiles(i + 1), tileGrid, lodFactor, i + 1,
                    (i == lodCount - 1) ? null : layers[i + 1]);
        }
    }

    public synchronized void save() {
        for (LowresLayer layer : this.layers) {
            layer.save();
        }
    }

    public Grid getTileGrid() {
        return tileGrid;
    }

    public int getLodCount() {
        return lodCount;
    }

    public int getLodFactor() {
        return lodFactor;
    }

    @Override
    public void set(int x, int z, Color color, int height, int blockLight) {
        int cellX = tileGrid.getCellX(x);
        int cellZ = tileGrid.getCellY(z);
        int localX = tileGrid.getLocalX(x);
        int localZ = tileGrid.getLocalY(z);
        layers[0].set(cellX, cellZ, localX, localZ, color, height, blockLight);
    }

}
