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

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.PalettedArrayAdapter;
import de.bluecolored.bluemap.core.util.RegistryAdapter;
import de.bluecolored.bluenbt.BlueNBT;
import de.bluecolored.bluenbt.TypeToken;
import lombok.Getter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

abstract class CellStorage<T extends CellStorage.Cell> {

    private static final BlueNBT BLUE_NBT = new BlueNBT();
    static {
        BLUE_NBT.register(TypeToken.of(TileState.class), new RegistryAdapter<>(TileState.REGISTRY, Key.BLUEMAP_NAMESPACE, TileState.UNKNOWN));
        BLUE_NBT.register(TypeToken.of(TileState[].class), new PalettedArrayAdapter<>(BLUE_NBT, TileState.class));
    }

    private static final int CACHE_SIZE = 4;

    @Getter private final GridStorage storage;
    private final Class<T> type;
    private final LinkedHashMap<Vector2i, T> cells = new LinkedHashMap<>(
            8,
            0.75f,
            true
    ) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Vector2i, T> eldest) {
            if (this.size() <= CACHE_SIZE) return false;
            saveCell(eldest.getKey(), eldest.getValue());
            return true;
        }
    };

    public CellStorage(GridStorage storage, Class<T> type) {
        this.storage = storage;
        this.type = type;
    }

    public synchronized void save() {
        cells.forEach(this::saveCell);
    }

    public synchronized void reset() {
        cells.clear();
    }

    T cell(int x, int z) {
        return cell(new Vector2i(x, z));
    }

    synchronized T cell(Vector2i pos) {
        return cells.computeIfAbsent(pos, this::loadCell);
    }

    private synchronized T loadCell(Vector2i pos) {
        try (CompressedInputStream in = storage.read(pos.getX(), pos.getY())) {
            if (in != null)
                return BLUE_NBT.read(in.decompress(), type);
        } catch (IOException ex) {
            Logger.global.logError("Failed to load render-state cell " + pos, ex);
        } catch (RuntimeException ex) { // E.g. NoSuchElementException thrown by BlueNBT if there is a format error
            Logger.global.logError("Failed to load render-state cell " + pos, ex);

            // try to delete the possibly corrupted file for self-healing
            try {
                storage.delete(pos.getX(), pos.getY());
            } catch (IOException e) {
                Logger.global.logError("Failed to delete render-state cell " + pos, e);
            }
        }

        return createNewCell();
    }

    protected abstract T createNewCell();

    private synchronized void saveCell(Vector2i pos, T cell) {
        if (!cell.isModified()) return;
        try (OutputStream in = storage.write(pos.getX(), pos.getY())) {
            BLUE_NBT.write(cell, in, type);
        } catch (IOException ex) {
            Logger.global.logError("Failed to save render-state cell " + pos, ex);
        }
    }

    public interface Cell {
        boolean isModified();
    }

}
