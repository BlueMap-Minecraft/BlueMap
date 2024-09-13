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

import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.RegistryAdapter;
import de.bluecolored.bluenbt.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;

import static de.bluecolored.bluemap.core.map.renderstate.MapTileState.SHIFT;

public class TileInfoRegion implements CellStorage.Cell {

    private static final int REGION_LENGTH = 1 << SHIFT;
    private static final int REGION_MASK = REGION_LENGTH - 1;
    private static final int TILES_PER_REGION = REGION_LENGTH * REGION_LENGTH;

    @NBTName("last-render-times")
    private int[] lastRenderTimes;

    @NBTName("tile-states")
    private TileState[] tileStates;

    @Getter
    private transient boolean modified;

    private TileInfoRegion() {}

    @NBTPostDeserialize
    public void init() {
        if (lastRenderTimes == null || lastRenderTimes.length != TILES_PER_REGION)
            lastRenderTimes = new int[TILES_PER_REGION];

        if (tileStates == null || tileStates.length != TILES_PER_REGION) {
            tileStates = new TileState[TILES_PER_REGION];
            Arrays.fill(tileStates, TileState.UNKNOWN);
        }
    }

    public TileInfo get(int x, int z) {
        int index = index(x, z);
        return new TileInfo(
                lastRenderTimes[index],
                tileStates[index]
        );
    }

    public TileInfo set(int x, int z, TileInfo info) {
        int index = index(x, z);

        TileInfo previous = new TileInfo(
                lastRenderTimes[index],
                tileStates[index]
        );

        lastRenderTimes[index] = info.getRenderTime();
        tileStates[index] = Objects.requireNonNull(info.getState());

        if (!previous.equals(info))
            this.modified = true;

        return previous;
    }

    int findLatestRenderTime() {
        if (lastRenderTimes == null) return -1;
        return Arrays.stream(lastRenderTimes)
                .max()
                .orElse(-1);
    }

    private static int index(int x, int z) {
        return (z & REGION_MASK) << SHIFT | (x & REGION_MASK);
    }

    @Data
    @AllArgsConstructor
    public static class TileInfo {

        private int renderTime;
        private TileState state;

    }

    public static TileInfoRegion create() {
        TileInfoRegion region = new TileInfoRegion();
        region.init();
        return region;
    }

    /**
     * Only loads the palette-part from a TileState-file
     */
    public static TileState[] loadPalette(InputStream in) throws IOException {
        return PaletteOnly.BLUE_NBT.read(in, PaletteOnly.class).tileStates.palette;
    }

    @Getter
    private static class PaletteOnly {

        private final static BlueNBT BLUE_NBT = new BlueNBT();
        static {
            BLUE_NBT.register(TypeToken.of(TileState.class), new RegistryAdapter<>(TileState.REGISTRY, Key.BLUEMAP_NAMESPACE, TileState.UNKNOWN));
        }

        @NBTName("tile-states")
        private TileStates tileStates;

        @Getter
        private static class TileStates {
            private TileState[] palette;
        }

    }

}
