package de.bluecolored.bluemap.core.map.lowres;

import de.bluecolored.bluemap.core.map.TileMetaConsumer;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.Grid;

public class LowresTileManager implements TileMetaConsumer {

    private final Grid tileGrid;
    private final int lodFactor, lodCount;

    private final LowresLayer[] layers;

    public LowresTileManager(Storage.MapStorage mapStorage, Grid tileGrid, int lodCount, int lodFactor) {
        this.tileGrid = tileGrid;
        this.lodFactor = lodFactor;
        this.lodCount = lodCount;

        this.layers = new LowresLayer[lodCount];
        for (int i = lodCount - 1; i >= 0; i--) {
            this.layers[i] = new LowresLayer(mapStorage, tileGrid, lodCount, lodFactor, i + 1,
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
