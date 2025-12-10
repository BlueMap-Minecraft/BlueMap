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
package de.bluecolored.bluemap.core.storage.sql;

import com.github.benmanes.caffeine.cache.Cache;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.storage.ItemStorage;
import de.bluecolored.bluemap.core.storage.KeyedMapStorage;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.storage.sql.commandset.CommandSet;
import de.bluecolored.bluemap.core.util.Caches;
import de.bluecolored.bluemap.core.util.Key;

import java.io.IOException;
import java.util.function.DoublePredicate;

public class SQLMapStorage extends KeyedMapStorage {

    private final String mapId;
    private final CommandSet sql;

    private final Cache<Key, ItemStorage> itemStorages = Caches.build();
    private final Cache<Key, GridStorage> gridStorages = Caches.build();

    public SQLMapStorage(String mapId, CommandSet sql, Compression compression) {
        super(compression);

        this.mapId = mapId;
        this.sql = sql;
    }

    @Override
    public ItemStorage item(Key key, Compression compression) {
        ItemStorage item = itemStorages.getIfPresent(key);
        if (item != null) return item;

        return itemStorages.get(key, k -> new SQLItemStorage(sql, mapId, key, compression));
    }

    @Override
    public GridStorage grid(Key key, Compression compression) {
        GridStorage grid = gridStorages.getIfPresent(key);
        if (grid != null) return grid;

        return gridStorages.get(key, k -> new SQLGridStorage(sql, mapId, key, compression));
    }

    @Override
    public void delete(DoublePredicate onProgress) throws IOException {

        // delete tiles in 1000er steps to track progress
        int tileCount = sql.countMapGridsItems(mapId);
        if (tileCount > 0) {
            int totalDeleted = 0;
            int deleted = 0;
            do {
                deleted = sql.purgeMapGrids(mapId, 1000);
                totalDeleted += deleted;

                if (!onProgress.test((double) totalDeleted / tileCount))
                    return;

            } while (deleted > 0 && totalDeleted < tileCount);
        }

        // finally purge the map
        sql.purgeMap(mapId);

    }

    @Override
    public boolean exists() throws IOException {
        return sql.hasMap(mapId);
    }

    @Override
    public boolean isClosed() {
        return sql.isClosed();
    }

}
