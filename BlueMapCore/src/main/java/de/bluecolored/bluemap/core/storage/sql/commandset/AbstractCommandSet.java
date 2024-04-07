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

    private final Database db;

    final LoadingCache<String, Integer> mapKeys = Caffeine.newBuilder()
            .build(this::findOrCreateMapKey);
    final LoadingCache<Compression, Integer> compressionKeys = Caffeine.newBuilder()
            .build(this::findOrCreateCompressionKey);

    @Language("sql")
    public abstract String createMapTableStatement();

    @Language("sql")
    public abstract String createCompressionTableStatement();

    @Language("sql")
    public abstract String createMapMetaTableStatement();

    @Language("sql")
    public abstract String createMapTileTableStatement();

    @Language("sql")
    public abstract String fixLegacyCompressionIdsStatement();

    public void initializeTables() throws IOException {
        db.run(connection -> {
            executeUpdate(connection, createMapTableStatement());
            executeUpdate(connection, createCompressionTableStatement());
            executeUpdate(connection, createMapMetaTableStatement());
            executeUpdate(connection, createMapTileTableStatement());
        });

        db.run(connection -> executeUpdate(connection, fixLegacyCompressionIdsStatement()));
    }

    @Language("sql")
    public abstract String writeMapTileStatement();

    @Override
    public int writeMapTile(
            String mapId, int lod, int x, int z, Compression compression,
            byte[] bytes
    ) throws IOException {
        int mapKey = mapKey(mapId);
        int compressionKey = compressionKey(compression);
        return db.run(connection -> {
            return executeUpdate(connection,
                    writeMapTileStatement(),
                    mapKey, lod, x, z, compressionKey,
                    bytes
            );
        });
    }

    @Language("sql")
    public abstract String readMapTileStatement();

    @Override
    public byte @Nullable [] readMapTile(String mapId, int lod, int x, int z, Compression compression) throws IOException {
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    readMapTileStatement(),
                    mapId, lod, x, z, compression.getKey().getFormatted()
            );
            if (!result.next()) return null;
            return result.getBytes(1);
        });
    }

    @Language("sql")
    public abstract String deleteMapTileStatement();

    @Override
    public int deleteMapTile(String mapId, int lod, int x, int z, Compression compression) throws IOException {
        return db.run(connection -> {
            return executeUpdate(connection,
                    deleteMapTileStatement(),
                    mapId, lod, x, z, compression.getKey().getFormatted()
            );
        });
    }

    @Language("sql")
    public abstract String hasMapTileStatement();

    @Override
    public boolean hasMapTile(String mapId, int lod, int x, int z, Compression compression) throws IOException {
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    hasMapTileStatement(),
                    mapId, lod, x, z, compression.getKey().getFormatted()
            );
            if (!result.next()) throw new IllegalStateException("Counting query returned empty result!");
            return result.getBoolean(1);
        });
    }

    @Language("sql")
    public abstract String countAllMapTilesStatement();

    @Override
    public int countAllMapTiles(String mapId) throws IOException {
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    countAllMapTilesStatement(),
                    mapId
            );
            if (!result.next()) throw new IllegalStateException("Counting query returned empty result!");
            return result.getInt(1);
        });
    }

    @Language("sql")
    public abstract String purgeMapTilesStatement();

    @Override
    public int purgeMapTiles(String mapId, int limit) throws IOException {
        return db.run(connection -> {
            return executeUpdate(connection,
                    purgeMapTilesStatement(),
                    mapId, limit
            );
        });
    }

    @Language("sql")
    public abstract String listMapTilesStatement();

    @Override
    public TilePosition[] listMapTiles(String mapId, int lod, Compression compression, int start, int count) throws IOException {
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    listMapTilesStatement(),
                    mapId, lod, compression.getKey().getFormatted(),
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
    public abstract String writeMapMetaStatement();

    @Override
    public int writeMapMeta(String mapId, String itemName, byte[] bytes) throws IOException {
        int mapKey = mapKey(mapId);
        return db.run(connection -> {
            return executeUpdate(connection,
                    writeMapMetaStatement(),
                    mapKey, itemName,
                    bytes
            );
        });
    }

    @Language("sql")
    public abstract String readMapMetaStatement();

    @Override
    public byte @Nullable [] readMapMeta(String mapId, String itemName) throws IOException {
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    readMapMetaStatement(),
                    mapId, itemName
            );
            if (!result.next()) return null;
            return result.getBytes(1);
        });
    }

    @Language("sql")
    public abstract String deleteMapMetaStatement();

    @Override
    public int deleteMapMeta(String mapId, String itemName) throws IOException {
        return db.run(connection -> {
            return executeUpdate(connection,
                    deleteMapMetaStatement(),
                    mapId, itemName
            );
        });
    }

    @Language("sql")
    public abstract String hasMapMetaStatement();

    @Override
    public boolean hasMapMeta(String mapId, String itemName) throws IOException {
        return db.run(connection -> {
            ResultSet result = executeQuery(connection,
                    hasMapMetaStatement(),
                    mapId, itemName
            );
            if (!result.next()) throw new IllegalStateException("Counting query returned empty result!");
            return result.getBoolean(1);
        });
    }

    @Language("sql")
    public abstract String purgeMapTileTableStatement();

    @Language("sql")
    public abstract String purgeMapMetaTableStatement();

    @Language("sql")
    public abstract String deleteMapStatement();

    @Override
    public void purgeMap(String mapId) throws IOException {
        synchronized (mapKeys) {
            db.run(connection -> {

                executeUpdate(connection,
                        purgeMapTileTableStatement(),
                        mapId
                );

                executeUpdate(connection,
                        purgeMapMetaTableStatement(),
                        mapId
                );

                executeUpdate(connection,
                        deleteMapStatement(),
                        mapId
                );

            });

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
