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
package de.bluecolored.bluemap.core.storage.sql.commandset;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.storage.sql.Database;
import de.bluecolored.bluemap.core.util.Key;
import lombok.RequiredArgsConstructor;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SqlSourceToSinkFlow")
@RequiredArgsConstructor
public abstract class AbstractCommandSet implements CommandSet {

    protected final Database db;

    protected final LoadingCache<String, Integer> mapKeys = Caffeine.newBuilder()
            .build(this::findOrCreateMapKey);
    protected final LoadingCache<Compression, Integer> compressionKeys = Caffeine.newBuilder()
            .build(this::findOrCreateCompressionKey);
    protected final LoadingCache<Key, Integer> itemStorageKeys = Caffeine.newBuilder()
            .build(this::findOrCreateItemStorageKey);
    protected final LoadingCache<Key, Integer> gridStorageKeys = Caffeine.newBuilder()
            .build(this::findOrCreateGridStorageKey);

    @Language("sql")
    public abstract String createMapTableStatement();

    @Language("sql")
    public abstract String createCompressionTableStatement();

    @Language("sql")
    public abstract String createItemStorageTableStatement();

    @Language("sql")
    public abstract String createItemStorageDataTableStatement();

    @Language("sql")
    public abstract String createGridStorageTableStatement();

    @Language("sql")
    public abstract String createGridStorageDataTableStatement();

    @Override
    public void initializeTables() throws IOException {
        db.run(connection -> {
            executeUpdate(connection, createMapTableStatement());
            executeUpdate(connection, createCompressionTableStatement());
            executeUpdate(connection, createItemStorageTableStatement());
            executeUpdate(connection, createItemStorageDataTableStatement());
            executeUpdate(connection, createGridStorageTableStatement());
            executeUpdate(connection, createGridStorageDataTableStatement());
        });
    }

    @Language("sql")
    public abstract String itemStorageWriteStatement();

    @Override
    public void writeItem(String mapId, Key key, Compression compression, byte[] bytes) throws IOException {
        int mapKey = mapKey(mapId);
        int storageKey = itemStorageKey(key);
        int compressionKey = compressionKey(compression);
        db.run(connection -> executeUpdate(connection,
                itemStorageWriteStatement(),
                mapKey, storageKey, compressionKey,
                bytes
        ));
    }

    @Language("sql")
    public abstract String itemStorageReadStatement();

    @Override
    public byte @Nullable [] readItem(String mapId, Key key, Compression compression) throws IOException {
        int mapKey = mapKey(mapId);
        int storageKey = itemStorageKey(key);
        int compressionKey = compressionKey(compression);
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    itemStorageReadStatement(),
                    mapKey, storageKey, compressionKey
            );
            if (!result.next()) return null;
            return result.getBytes(1);
        });
    }

    @Language("sql")
    public abstract String itemStorageDeleteStatement();

    @Override
    public void deleteItem(String mapId, Key key) throws IOException {
        int mapKey = mapKey(mapId);
        int storageKey = itemStorageKey(key);
        db.run(connection -> executeUpdate(connection,
                itemStorageDeleteStatement(),
                mapKey, storageKey
        ));
    }

    @Language("sql")
    public abstract String itemStorageHasStatement();

    @Override
    public boolean hasItem(String mapId, Key key, Compression compression) throws IOException {
        int mapKey = mapKey(mapId);
        int storageKey = itemStorageKey(key);
        int compressionKey = compressionKey(compression);
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    itemStorageHasStatement(),
                    mapKey, storageKey, compressionKey
            );
            if (!result.next()) throw new IllegalStateException("Counting query returned empty result!");
            return result.getBoolean(1);
        });
    }

    @Language("sql")
    public abstract String gridStorageWriteStatement();

    @Override
    public void writeGridItem(
            String mapId, Key key, int x, int z, Compression compression,
            byte[] bytes
    ) throws IOException {
        int mapKey = mapKey(mapId);
        int storageKey = gridStorageKey(key);
        int compressionKey = compressionKey(compression);
        db.run(connection -> executeUpdate(connection,
                gridStorageWriteStatement(),
                mapKey, storageKey, x, z, compressionKey,
                bytes
        ));
    }

    @Language("sql")
    public abstract String gridStorageReadStatement();

    @Override
    public byte @Nullable [] readGridItem(
            String mapId, Key key, int x, int z, Compression compression
    ) throws IOException {
        int mapKey = mapKey(mapId);
        int storageKey = gridStorageKey(key);
        int compressionKey = compressionKey(compression);
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    gridStorageReadStatement(),
                    mapKey, storageKey, x, z, compressionKey
            );
            if (!result.next()) return null;
            return result.getBytes(1);
        });
    }

    @Language("sql")
    public abstract String gridStorageDeleteStatement();

    @Override
    public void deleteGridItem(
            String mapId, Key key, int x, int z
    ) throws IOException {
        int mapKey = mapKey(mapId);
        int storageKey = gridStorageKey(key);
        db.run(connection -> executeUpdate(connection,
                gridStorageDeleteStatement(),
                mapKey, storageKey, x, z
        ));
    }

    @Language("sql")
    public abstract String gridStorageHasStatement();

    @Override
    public boolean hasGridItem(
            String mapId, Key key, int x, int z, Compression compression
    ) throws IOException {
        int mapKey = mapKey(mapId);
        int storageKey = gridStorageKey(key);
        int compressionKey = compressionKey(compression);
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    gridStorageHasStatement(),
                    mapKey, storageKey, x, z, compressionKey
            );
            if (!result.next()) throw new IllegalStateException("Counting query returned empty result!");
            return result.getBoolean(1);
        });
    }

    @Language("sql")
    public abstract String gridStorageListStatement();

    @Override
    public TilePosition[] listGridItems(
            String mapId, Key key, Compression compression,
            int start, int count
    ) throws IOException {
        int mapKey = mapKey(mapId);
        int storageKey = gridStorageKey(key);
        int compressionKey = compressionKey(compression);
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    gridStorageListStatement(),
                    mapKey, storageKey, compressionKey,
                    count, start
            );

            TilePosition[] tiles = new TilePosition[count];
            int i = 0;
            while (result.next()) {
                tiles[i++] = new TilePosition(
                        result.getInt(1),
                        result.getInt(2)
                );
            }

            if (i < count) {
                TilePosition[] trimmed = new TilePosition[i];
                System.arraycopy(tiles, 0, trimmed, 0, i);
                tiles = trimmed;
            }

            return tiles;
        });
    }

    @Language("sql")
    public abstract String gridStorageCountMapItemsStatement();

    @Override
    public int countMapGridsItems(String mapId) throws IOException {
        int mapKey = mapKey(mapId);
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    gridStorageCountMapItemsStatement(),
                    mapKey
            );
            if (!result.next()) throw new IllegalStateException("Counting query returned empty result!");
            return result.getInt(1);
        });
    }

    @Language("sql")
    public abstract String gridStoragePurgeMapStatement();

    @Override
    public int purgeMapGrids(String mapId, int limit) throws IOException {
        int mapKey = mapKey(mapId);
        return db.run(connection -> {
            return executeUpdate(connection,
                    gridStoragePurgeMapStatement(),
                    mapKey, limit
            );
        });
    }

    @Language("sql")
    public abstract String purgeMapStatement();

    @Override
    public void purgeMap(String mapId) throws IOException {
        synchronized (mapKeys) {
            int mapKey = mapKey(mapId);
            db.run(connection -> executeUpdate(connection,
                    purgeMapStatement(),
                    mapKey
            ));
            mapKeys.invalidate(mapId);
        }
    }

    @Language("sql")
    public abstract String hasMapStatement();

    @Override
    public boolean hasMap(String mapId) throws IOException {
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    hasMapStatement(),
                    mapId
            );
            if (!result.next()) throw new IllegalStateException("Counting query returned empty result!");
            return result.getBoolean(1);
        });
    }

    @Language("sql")
    public abstract String listMapIdsStatement();

    @Override
    public String[] listMapIds(int start, int count) throws IOException {
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    listMapIdsStatement(),
                    count, start
            );
            List<String> mapIds = new ArrayList<>();
            while (result.next()) {
                mapIds.add(result.getString(1));
            }
            return mapIds.toArray(String[]::new);
        });
    }

    @Language("sql")
    public abstract String findMapKeyStatement();

    @Language("sql")
    public abstract String createMapKeyStatement();

    public int mapKey(String mapId) {
        synchronized (mapKeys) {
            //noinspection DataFlowIssue
            return mapKeys.get(mapId);
        }
    }

    public int findOrCreateMapKey(String mapId) throws IOException {
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    findMapKeyStatement(),
                    mapId
            );

            if (result.next())
                return result.getInt(1);

            PreparedStatement statement = connection.prepareStatement(
                    createMapKeyStatement(),
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, mapId);
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (!keys.next()) throw new IllegalStateException("No generated key returned!");
            return keys.getInt(1);
        });
    }

    @Language("sql")
    public abstract String findCompressionKeyStatement();

    @Language("sql")
    public abstract String createCompressionKeyStatement();

    public int compressionKey(Compression compression) {
        synchronized (compressionKeys) {
            //noinspection DataFlowIssue
            return compressionKeys.get(compression);
        }
    }

    public int findOrCreateCompressionKey(Compression compression) throws IOException {
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    findCompressionKeyStatement(),
                    compression.getKey().getFormatted()
            );

            if (result.next())
                return result.getInt(1);

            PreparedStatement statement = connection.prepareStatement(
                    createCompressionKeyStatement(),
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, compression.getKey().getFormatted());
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (!keys.next()) throw new IllegalStateException("No generated key returned!");
            return keys.getInt(1);
        });
    }

    @Language("sql")
    public abstract String findItemStorageKeyStatement();

    @Language("sql")
    public abstract String createItemStorageKeyStatement();

    public int itemStorageKey(Key key) {
        synchronized (itemStorageKeys) {
            //noinspection DataFlowIssue
            return itemStorageKeys.get(key);
        }
    }

    public int findOrCreateItemStorageKey(Key key) throws IOException {
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    findItemStorageKeyStatement(),
                    key.getFormatted()
            );

            if (result.next())
                return result.getInt(1);

            PreparedStatement statement = connection.prepareStatement(
                    createItemStorageKeyStatement(),
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, key.getFormatted());
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (!keys.next()) throw new IllegalStateException("No generated key returned!");
            return keys.getInt(1);
        });
    }

    @Language("sql")
    public abstract String findGridStorageKeyStatement();

    @Language("sql")
    public abstract String createGridStorageKeyStatement();

    public int gridStorageKey(Key key) {
        synchronized (gridStorageKeys) {
            //noinspection DataFlowIssue
            return gridStorageKeys.get(key);
        }
    }

    public int findOrCreateGridStorageKey(Key key) throws IOException {
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    findGridStorageKeyStatement(),
                    key.getFormatted()
            );

            if (result.next())
                return result.getInt(1);

            PreparedStatement statement = connection.prepareStatement(
                    createGridStorageKeyStatement(),
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, key.getFormatted());
            statement.executeUpdate();

            ResultSet keys = statement.getGeneratedKeys();
            if (!keys.next()) throw new IllegalStateException("No generated key returned!");
            return keys.getInt(1);
        });
    }

    @Override
    public boolean isClosed() {
        return db.isClosed();
    }

    @Override
    public void close() throws IOException {
        db.close();
    }

    protected static ResultSet executeQuery(Connection connection, @Language("sql") String sql, Object... parameters) throws SQLException {
        return prepareStatement(connection, sql, parameters).executeQuery();
    }

    @SuppressWarnings("UnusedReturnValue")
    protected static int executeUpdate(Connection connection, @Language("sql") String sql, Object... parameters) throws SQLException {
        return prepareStatement(connection, sql, parameters).executeUpdate();
    }

    private static PreparedStatement prepareStatement(Connection connection, @Language("sql") String sql, Object... parameters) throws SQLException {
        // we only use this prepared statement once, but the DB-Driver caches those and reuses them
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
        return statement;
    }

}
