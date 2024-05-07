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

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.util.Grid;
import lombok.Getter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@DebugDump
public class MapTileState extends CellStorage<TileInfoRegion> {

    static final int SHIFT = 5;
    public static final Grid GRID = new Grid(1 << SHIFT);

    @Getter private int lastRenderTime = -1;
    private final Map<TileState, Integer> chunkStateCounts = new ConcurrentHashMap<>();

    public MapTileState(GridStorage storage) {
        super(storage, TileInfoRegion.class);
    }

    public synchronized void load() {
        lastRenderTime = -1;
        chunkStateCounts.clear();

        try (Stream<GridStorage.Cell> stream = getStorage().stream()) {
            stream.forEach(cell -> {
                TileInfoRegion region = cell(cell.getX(), cell.getZ());
                lastRenderTime = Math.max(lastRenderTime, region.findLatestRenderTime());
                region.populateSummaryMap(chunkStateCounts);
            });
        } catch (IOException e) {
            Logger.global.logError("Failed to load render-state regions", e);
        }
    }

    public synchronized void reset() {
        super.reset();
        load();
    }

    public TileInfoRegion.TileInfo get(int x, int z) {
        return cell(x >> SHIFT, z >> SHIFT).get(x, z);
    }

    public synchronized TileInfoRegion.TileInfo set(int x, int z, TileInfoRegion.TileInfo info) {
        TileInfoRegion.TileInfo old = cell(x >> SHIFT, z >> SHIFT).set(x, z, info);

        if (info.getRenderTime() > lastRenderTime)
            lastRenderTime = info.getRenderTime();

        if (old.getState() != info.getState()) {
            chunkStateCounts.merge(old.getState(), -1, Integer::sum);
            chunkStateCounts.merge(info.getState(), 1, Integer::sum);
        }

        return old;
    }

    public Map<TileState, Integer> getChunkStateCounts() {
        return Collections.unmodifiableMap(chunkStateCounts);
    }

    @Override
    protected synchronized TileInfoRegion createNewCell() {
        TileInfoRegion region = TileInfoRegion.create();
        region.populateSummaryMap(chunkStateCounts);
        return region;
    }

}
