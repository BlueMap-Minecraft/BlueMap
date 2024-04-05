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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.storage.MapStorage;
import de.bluecolored.bluemap.core.storage.SingleItemStorage;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.storage.sql.commandset.CommandSet;

import java.io.IOException;
import java.util.function.DoublePredicate;

public class SQLMapStorage implements MapStorage {

    public static final String SETTINGS_META_NAME = "settings.json";
    public static final String TEXTURES_META_NAME = "textures.json";
    public static final String RENDER_STATE_META_NAME = ".rstate";
    public static final String MARKERS_META_NAME = "live/markers.json";
    public static final String PLAYERS_META_NAME = "live/players.json";

    private final String mapId;
    private final CommandSet sql;

    private final SQLTileStorage hiresTileStorage;
    private final LoadingCache<Integer, GridStorage> lowresGridStorages;

    private final SingleItemStorage renderStateStorage;
    private final SingleItemStorage settingsStorage;
    private final SingleItemStorage texturesStorage;
    private final SingleItemStorage markersStorage;
    private final SingleItemStorage playersStorage;

    public SQLMapStorage(String mapId, CommandSet sql, Compression compression) {
        this.mapId = mapId;
        this.sql = sql;

        this.hiresTileStorage = new SQLTileStorage(
                sql,
                mapId,
                0,
                compression
        );

        this.lowresGridStorages = Caffeine.newBuilder().build(lod -> new SQLTileStorage(
                sql,
                mapId,
                lod,
                Compression.NONE
        ));

        renderStateStorage = meta(RENDER_STATE_META_NAME, Compression.NONE);
        settingsStorage = meta(SETTINGS_META_NAME, Compression.NONE);
        texturesStorage = meta(TEXTURES_META_NAME, Compression.NONE);
        markersStorage = meta(MARKERS_META_NAME, Compression.NONE);
        playersStorage = meta(PLAYERS_META_NAME, Compression.NONE);
    }

    @Override
    public GridStorage hiresTiles() {
        return hiresTileStorage;
    }

    @Override
    public GridStorage lowresTiles(int lod) {
        return lowresGridStorages.get(lod);
    }

    public String getAssetMetaName(String assetName) {
        return "assets/" + MapStorage.escapeAssetName(assetName);
    }

    @Override
    public SingleItemStorage asset(String name) {
        return meta(getAssetMetaName(name), Compression.NONE);
    }

    @Override
    public SingleItemStorage renderState() {
        return renderStateStorage;
    }

    @Override
    public SingleItemStorage settings() {
        return settingsStorage;
    }

    @Override
    public SingleItemStorage textures() {
        return texturesStorage;
    }

    @Override
    public SingleItemStorage markers() {
        return markersStorage;
    }

    @Override
    public SingleItemStorage players() {
        return playersStorage;
    }

    @Override
    public void delete(DoublePredicate onProgress) throws IOException {

        // delete tiles in 1000er steps to track progress
        int tileCount = sql.countAllMapTiles(mapId);
        if (tileCount > 0) {
            int totalDeleted = 0;
            int deleted = 0;
            do {
                deleted = sql.purgeMapTiles(mapId, 1000);
                totalDeleted += deleted;

                if (onProgress.test((double) totalDeleted / tileCount))
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

    private SingleItemStorage meta(String name, Compression compression) {
        return new SQLMetaItemStorage(sql, mapId, name, compression);
    }

    @Override
    public boolean isClosed() {
        return sql.isClosed();
    }

}
