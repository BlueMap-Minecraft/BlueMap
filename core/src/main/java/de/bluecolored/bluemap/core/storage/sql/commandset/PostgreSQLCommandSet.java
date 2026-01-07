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

import de.bluecolored.bluemap.core.storage.sql.Database;
import org.intellij.lang.annotations.Language;

public class PostgreSQLCommandSet extends AbstractCommandSet {

    public PostgreSQLCommandSet(Database db, String tablePrefix) {
        super(db, tablePrefix);
    }

    @Override
    @Language("postgresql")
    public String listExistingTablesStatement() {
        return """
        SELECT tablename
        FROM pg_catalog.pg_tables
        WHERE schemaname = current_schema()
        """;
    }

    @Override
    @Language("postgresql")
    public String createMapTableStatement() {
        return String.format("""
        CREATE TABLE IF NOT EXISTS %s (
         id SMALLSERIAL PRIMARY KEY,
         map_id VARCHAR(190) UNIQUE NOT NULL
        )
        """, tableName("map"));
    }

    @Override
    @Language("postgresql")
    public String createCompressionTableStatement() {
        return String.format("""
        CREATE TABLE IF NOT EXISTS %s (
         id SMALLSERIAL PRIMARY KEY,
         key VARCHAR(190) UNIQUE NOT NULL
        )
        """, tableName("compression"));
    }

    @Override
    @Language("postgresql")
    public String createItemStorageTableStatement() {
        return String.format("""
        CREATE TABLE IF NOT EXISTS %s (
         id SERIAL PRIMARY KEY,
         key VARCHAR(190) UNIQUE NOT NULL
        )
        """, tableName("item_storage"));
    }

    @Override
    @Language("postgresql")
    public String createItemStorageDataTableStatement() {
        return String.format("""
        CREATE TABLE IF NOT EXISTS %s (
         map SMALLINT NOT NULL
          REFERENCES %s (id)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         storage INT NOT NULL
          REFERENCES %s (id)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         compression SMALLINT NOT NULL
          REFERENCES %s (id)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         data BYTEA NOT NULL,
         PRIMARY KEY (map, storage)
        )
        """, tableName("item_storage_data"), tableName("map"), tableName("item_storage"), tableName("compression"));
    }

    @Override
    @Language("postgresql")
    public String createGridStorageTableStatement() {
        return String.format("""
        CREATE TABLE IF NOT EXISTS %s (
         id SMALLSERIAL PRIMARY KEY,
         key VARCHAR(190) UNIQUE NOT NULL
        )
        """, tableName("grid_storage"));
    }

    @Override
    @Language("postgresql")
    public String createGridStorageDataTableStatement() {
        return String.format("""
        CREATE TABLE IF NOT EXISTS %s (
         map SMALLINT NOT NULL
          REFERENCES %s (id)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         storage SMALLINT NOT NULL
          REFERENCES %s (id)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         x INT NOT NULL,
         z INT NOT NULL,
         compression SMALLINT NOT NULL
          REFERENCES %s (id)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         data BYTEA NOT NULL,
         PRIMARY KEY (map, storage, x, z)
        )
        """, tableName("grid_storage_data"), tableName("map"), tableName("grid_storage"), tableName("compression"));
    }

    @Override
    @Language("postgresql")
    public String itemStorageWriteStatement() {
        return String.format("""
        INSERT
        INTO %s (map, storage, compression, data)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (map, storage)
         DO UPDATE SET
          compression = excluded.compression,
          data = excluded.data
        """, tableName("item_storage_data"));
    }

    @Override
    @Language("postgresql")
    public String itemStorageReadStatement() {
        return String.format("""
        SELECT data
        FROM %s
        WHERE map = ?
        AND storage = ?
        AND compression = ?
        """, tableName("item_storage_data"));
    }

    @Override
    @Language("postgresql")
    public String itemStorageDeleteStatement() {
        return String.format("""
        DELETE
        FROM %s
        WHERE map = ?
        AND storage = ?
        """, tableName("item_storage_data"));
    }

    @Override
    @Language("postgresql")
    public String itemStorageHasStatement() {
        return String.format("""
        SELECT COUNT(*) > 0
        FROM %s
        WHERE map = ?
        AND storage = ?
        AND compression = ?
        """, tableName("item_storage_data"));
    }

    @Override
    @Language("postgresql")
    public String gridStorageWriteStatement() {
        return String.format("""
        INSERT
        INTO %s (map, storage, x, z, compression, data)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (map, storage, x, z)
         DO UPDATE SET
          compression = excluded.compression,
          data = excluded.data
        """, tableName("grid_storage_data"));
    }

    @Override
    @Language("postgresql")
    public String gridStorageReadStatement() {
        return String.format("""
        SELECT data
        FROM %s
        WHERE map = ?
        AND storage = ?
        AND x = ?
        AND z = ?
        AND compression = ?
        """, tableName("grid_storage_data"));
    }

    @Override
    @Language("postgresql")
    public String gridStorageDeleteStatement() {
        return String.format("""
        DELETE
        FROM %s
        WHERE map = ?
        AND storage = ?
        AND x = ?
        AND z = ?
        """, tableName("grid_storage_data"));
    }

    @Override
    @Language("postgresql")
    public String gridStorageHasStatement() {
        return String.format("""
        SELECT COUNT(*) > 0
        FROM %s
        WHERE map = ?
        AND storage = ?
        AND x = ?
        AND z = ?
        AND compression = ?
        """, tableName("grid_storage_data"));
    }

    @Override
    @Language("postgresql")
    public String gridStorageListStatement() {
        return String.format("""
        SELECT x, z
        FROM %s
        WHERE map = ?
        AND storage = ?
        AND compression = ?
        LIMIT ? OFFSET ?
        """, tableName("grid_storage_data"));
    }

    @Override
    @Language("postgresql")
    public String gridStorageCountMapItemsStatement() {
        return String.format("""
        SELECT COUNT(*)
        FROM %s
        WHERE map = ?
        """, tableName("grid_storage_data"));
    }

    @Override
    @Language("postgresql")
    public String gridStoragePurgeMapStatement() {
        return String.format("""
        DELETE
        FROM %s
        WHERE CTID IN (
         SELECT CTID
         FROM %s t
         WHERE t.map = ?
         LIMIT ?
        )
        """, tableName("grid_storage_data"), tableName("grid_storage_data"));
    }

    @Override
    @Language("postgresql")
    public String purgeMapStatement() {
        return String.format("""
        DELETE
        FROM %s
        WHERE id = ?
        """, tableName("map"));
    }

    @Override
    @Language("postgresql")
    public String hasMapStatement() {
        return String.format("""
        SELECT COUNT(*) > 0
        FROM %s m
        WHERE m.map_id = ?
        """, tableName("map"));
    }

    @Override
    @Language("postgresql")
    public String listMapIdsStatement() {
        return String.format("""
        SELECT map_id
        FROM %s m
        LIMIT ? OFFSET ?
        """, tableName("map"));
    }

    @Override
    @Language("postgresql")
    public String findMapKeyStatement() {
        return String.format("""
        SELECT id
        FROM %s
        WHERE map_id = ?
        """, tableName("map"));
    }

    @Override
    @Language("postgresql")
    public String createMapKeyStatement() {
        return String.format("""
        INSERT
        INTO %s (map_id)
        VALUES (?)
        """, tableName("map"));
    }

    @Override
    @Language("postgresql")
    public String findCompressionKeyStatement() {
        return String.format("""
        SELECT id
        FROM %s
        WHERE key = ?
        """, tableName("compression"));
    }

    @Override
    @Language("postgresql")
    public String createCompressionKeyStatement() {
        return String.format("""
        INSERT
        INTO %s (key)
        VALUES (?)
        """, tableName("compression"));
    }

    @Override
    @Language("postgresql")
    public String findItemStorageKeyStatement() {
        return String.format("""
        SELECT id
        FROM %s
        WHERE key = ?
        """, tableName("item_storage"));
    }

    @Override
    @Language("postgresql")
    public String createItemStorageKeyStatement() {
        return String.format("""
        INSERT
        INTO %s (key)
        VALUES (?)
        """, tableName("item_storage"));
    }

    @Override
    @Language("postgresql")
    public String findGridStorageKeyStatement() {
        return String.format("""
        SELECT id
        FROM %s
        WHERE key = ?
        """, tableName("grid_storage"));
    }

    @Override
    @Language("postgresql")
    public String createGridStorageKeyStatement() {
        return String.format("""
        INSERT
        INTO %s (key)
        VALUES (?)
        """, tableName("grid_storage"));
    }

}
