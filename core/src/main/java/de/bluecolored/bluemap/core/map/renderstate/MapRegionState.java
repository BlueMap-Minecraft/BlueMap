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
package de.bluecolored.bluemap.core.map.renderstate;

import de.bluecolored.bluemap.core.storage.GridStorage;

import java.io.IOException;

public class MapRegionState extends CellStorage<RegionInfoRegion> {

    static final int SHIFT = 6;

    public MapRegionState(GridStorage storage) {
        super(storage, RegionInfoRegion.class);
    }

    public int get(int x, int z) {
        return cell(x >> SHIFT, z >> SHIFT).get(x, z);
    }

    public synchronized int set(int x, int z, int lastUpdateTime) {
        return cell(x >> SHIFT, z >> SHIFT).set(x, z, lastUpdateTime);
    }

    public synchronized int delete(int x, int z) {
        return cell(x >> SHIFT, z >> SHIFT).set(x, z, 0);
    }

    public void forEach(RegionStateConsumer consumer) throws IOException {
        forEach((CellConsumer<RegionInfoRegion>) (cellPos, region) -> {
            for (int x = 0; x < RegionInfoRegion.REGION_LENGTH; x++) {
                for (int z = 0; z < RegionInfoRegion.REGION_LENGTH; z++) {
                    int lastUpdateTime = region.get(x, z);
                    if (lastUpdateTime != 0) {
                        consumer.accept((cellPos.getX() << SHIFT) + x, (cellPos.getY() << SHIFT) + z, lastUpdateTime);
                    }
                }
            }
        });
    }

    @Override
    protected RegionInfoRegion createNewCell() {
        return RegionInfoRegion.create();
    }

    @FunctionalInterface
    public interface RegionStateConsumer {
        void accept(int x, int z, int lastUpdateTime);
    }

}
