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
package de.bluecolored.bluemap.core.map.hires;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.TextureGallery;
import de.bluecolored.bluemap.core.map.TileMetaConsumer;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.util.math.Color;
import de.bluecolored.bluemap.core.world.World;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;

public class HiresModelManager {

    private final GridStorage storage;
    private final HiresModelRenderer renderer;

    @Getter
    private final Grid tileGrid;

    public HiresModelManager(GridStorage storage, ResourcePack resourcePack, TextureGallery textureGallery, RenderSettings renderSettings, Grid tileGrid) {
        this(storage, new HiresModelRenderer(resourcePack, textureGallery, renderSettings), tileGrid);
    }

    public HiresModelManager(GridStorage storage, HiresModelRenderer renderer, Grid tileGrid) {
        this.storage = storage;
        this.renderer = renderer;

        this.tileGrid = tileGrid;
    }

    /**
     * Renders the given world tile with the provided render-settings
     */
    public void render(World world, Vector2i tile, TileMetaConsumer tileMetaConsumer, boolean save) {
        Vector2i tileMin = tileGrid.getCellMin(tile);
        Vector2i tileMax = tileGrid.getCellMax(tile);

        Vector3i modelMin = new Vector3i(tileMin.getX(), Integer.MIN_VALUE, tileMin.getY());
        Vector3i modelMax = new Vector3i(tileMax.getX(), Integer.MAX_VALUE, tileMax.getY());

        if (save) {
            ArrayTileModel model = ArrayTileModel.instancePool().claimInstance();

            renderer.render(world, modelMin, modelMax, model, tileMetaConsumer);

            model.sort();
            save(model, tile);

            ArrayTileModel.instancePool().recycleInstance(model);
        } else {
            renderer.render(world, modelMin, modelMax, VoidTileModel.INSTANCE, tileMetaConsumer);
        }

    }

    /**
     * Un-renders a tile.
     * The hires tile is deleted and the tileMetaConsumer (lowres) is updated with default values in the tiles area.
     */
    public void unrender(Vector2i tile, TileMetaConsumer tileMetaConsumer) {
        try {
            storage.delete(tile.getX(), tile.getY());
        } catch (IOException ex) {
            Logger.global.logError("Failed to delete hires model: " + tile, ex);
        }

        Color color = new Color();
        tileGrid.forEachIntersecting(tile, Grid.UNIT, (x, z) ->
                tileMetaConsumer.set(x, z, color, 0, 0)
        );
    }

    private void save(final ArrayTileModel model, Vector2i tile) {
        try (
                OutputStream out = storage.write(tile.getX(), tile.getY());
                PRBMWriter modelWriter = new PRBMWriter(out)
        ) {
            modelWriter.write(model);
        } catch (IOException e){
            Logger.global.logError("Failed to save hires model: " + tile, e);
        }
    }

}
