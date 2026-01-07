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

public class SqliteCommandSet extends AbstractCommandSet {

    public SqliteCommandSet(Database db, String tablePrefix) {
        super(db, tablePrefix);
    }

    @Override
    @Language("sqlite")
    public String listExistingTablesStatement() {
        return """
        SELECT `name`
        FROM `sqlite_master`
        WHERE `type` = 'table'
        """;
    }

    @Override
    @Language("sqlite")
    public String createMapTableStatement() {
        return String.format("""
        CREATE TABLE IF NOT EXISTS `%s` (
         `id` INTEGER PRIMARY KEY AUTOINCREMENT,
         `map_id` TEXT UNIQUE NOT NULL
        ) STRICT
        """, tableName("map"));
    }

    @Override
    @Language("sqlite")
    public String createCompressionTableStatement() {
        return String.format("""
        CREATE TABLE IF NOT EXISTS `%s` (
         `id` INTEGER PRIMARY KEY AUTOINCREMENT,
         `key` TEXT UNIQUE NOT NULL
        ) STRICT
        """, tableName("compression"));
    }

    @Override
    @Language("sqlite")
    public String createItemStorageTableStatement() {
        return String.format("""
        CREATE TABLE IF NOT EXISTS `%s` (
         `id` INTEGER PRIMARY KEY AUTOINCREMENT,
         `key` TEXT UNIQUE NOT NULL
        ) STRICT
        """, tableName("item_storage"));
    }

    @Override
    @Language("sqlite")
    public String createItemStorageDataTableStatement() {
        return String.format("""
        CREATE TABLE IF NOT EXISTS `%s` (
         `map` INTEGER NOT NULL,
         `storage` INTEGER NOT NULL,
         `compression` INTEGER NOT NULL,
         `data` BLOB NOT NULL,
         PRIMARY KEY (`map`, `storage`),
         CONSTRAINT `fk_%s_item_map`
          FOREIGN KEY (`map`)
          REFERENCES `%s` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         CONSTRAINT `fk_%s_item`
          FOREIGN KEY (`storage`)
          REFERENCES `%s` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         CONSTRAINT `fk_%s_item_compression`
          FOREIGN KEY (`compression`)
          REFERENCES `%s` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE
        ) STRICT
        """, tableName("item_storage_data"), tablePrefix, tableName("map"), tablePrefix, tableName("item_storage"), tablePrefix, tableName("compression"));
    }

    @Override
    @Language("sqlite")
    public String createGridStorageTableStatement() {
        return String.format("""
        CREATE TABLE IF NOT EXISTS `%s` (
         `id` INTEGER PRIMARY KEY AUTOINCREMENT,
         `key` TEXT UNIQUE NOT NULL
        ) STRICT
        """, tableName("grid_storage"));
    }

    @Override
    @Language("sqlite")
    public String createGridStorageDataTableStatement() {
        return String.format("""
        CREATE TABLE IF NOT EXISTS `%s` (
         `map` INTEGER NOT NULL,
         `storage` INTEGER NOT NULL,
         `x` INTEGER NOT NULL,
         `z` INTEGER NOT NULL,
         `compression` INTEGER NOT NULL,
         `data` BLOB NOT NULL,
         PRIMARY KEY (`map`, `storage`, `x`, `z`),
         CONSTRAINT `fk_%s_grid_map`
          FOREIGN KEY (`map`)
          REFERENCES `%s` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         CONSTRAINT `fk_%s_grid`
          FOREIGN KEY (`storage`)
          REFERENCES `%s` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         CONSTRAINT `fk_%s_grid_compression`
          FOREIGN KEY (`compression`)
          REFERENCES `%s` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE
        ) STRICT
        """, tableName("grid_storage_data"), tablePrefix, tableName("map"), tablePrefix, tableName("grid_storage"), tablePrefix, tableName("compression"));
    }

    @Override
    @Language("sqlite")
    public String itemStorageWriteStatement() {
        return String.format("""
        REPLACE
        INTO `%s` (`map`, `storage`, `compression`, `data`)
        VALUES (?, ?, ?, ?)
        """, tableName("item_storage_data"));
    }

    @Override
    @Language("sqlite")
    public String itemStorageReadStatement() {
        return String.format("""
        SELECT `data`
        FROM `%s`
        WHERE `map` = ?
        AND `storage` = ?
        AND `compression` = ?
        """, tableName("item_storage_data"));
    }

    @Override
    @Language("sqlite")
    public String itemStorageDeleteStatement() {
        return String.format("""
        DELETE
        FROM `%s`
        WHERE `map` = ?
        AND `storage` = ?
        """, tableName("item_storage_data"));
    }

    @Override
    @Language("sqlite")
    public String itemStorageHasStatement() {
        return String.format("""
        SELECT COUNT(*) > 0
        FROM `%s`
        WHERE `map` = ?
        AND `storage` = ?
        AND `compression` = ?
        """, tableName("item_storage_data"));
    }


    @Override
    @Language("sqlite")
    public String gridStorageWriteStatement() {
        return String.format("""
        REPLACE
        INTO `%s` (`map`, `storage`, `x`, `z`, `compression`, `data`)
        VALUES (?, ?, ?, ?, ?, ?)
        """, tableName("grid_storage_data"));
    }

    @Override
    @Language("sqlite")
    public String gridStorageReadStatement() {
        return String.format("""
        SELECT `data`
        FROM `%s`
        WHERE `map` = ?
        AND `storage` = ?
        AND `x` = ?
        AND `z` = ?
        AND `compression` = ?
        """, tableName("grid_storage_data"));
    }

    @Override
    @Language("sqlite")
    public String gridStorageDeleteStatement() {
        return String.format("""
        DELETE
        FROM `%s`
        WHERE `map` = ?
        AND `storage` = ?
        AND `x` = ?
        AND `z` = ?
        """, tableName("grid_storage_data"));
    }

    @Override
    @Language("sqlite")
    public String gridStorageHasStatement() {
        return String.format("""
        SELECT COUNT(*) > 0
        FROM `%s`
        WHERE `map` = ?
        AND `storage` = ?
        AND `x` = ?
        AND `z` = ?
        AND `compression` = ?
        """, tableName("grid_storage_data"));
    }

    @Override
    @Language("sqlite")
    public String gridStorageListStatement() {
        return String.format("""
        SELECT `x`, `z`
        FROM `%s`
        WHERE `map` = ?
        AND `storage` = ?
        AND `compression` = ?
        LIMIT ? OFFSET ?
        """, tableName("grid_storage_data"));
    }

    @Override
    @Language("sqlite")
    public String gridStorageCountMapItemsStatement() {
        return String.format("""
        SELECT COUNT(*)
        FROM `%s`
        WHERE `map` = ?
        """, tableName("grid_storage_data"));
    }

    @Override
    @Language("sqlite")
    public String gridStoragePurgeMapStatement() {
        return String.format("""
        DELETE
        FROM `%s`
        WHERE ROWID IN (
         SELECT t.ROWID
         FROM `%s` t
         WHERE t.`map` = ?
         LIMIT ?
        )
        """, tableName("grid_storage_data"), tableName("grid_storage_data"));
    }

    @Override
    @Language("sqlite")
    public String purgeMapStatement() {
        return String.format("""
        DELETE
        FROM `%s`
        WHERE `id` = ?
        """, tableName("map"));
    }

    @Override
    @Language("sqlite")
    public String hasMapStatement() {
        return String.format("""
        SELECT COUNT(*) > 0
        FROM `%s` m
        WHERE m.`map_id` = ?
        """, tableName("map"));
    }

    @Override
    @Language("sqlite")
    public String listMapIdsStatement() {
        return String.format("""
        SELECT `map_id`
        FROM `%s` m
        LIMIT ? OFFSET ?
        """, tableName("map"));
    }

    @Override
    @Language("sqlite")
    public String findMapKeyStatement() {
        return String.format("""
        SELECT `id`
        FROM `%s`
        WHERE map_id = ?
        """, tableName("map"));
    }

    @Override
    @Language("sqlite")
    public String createMapKeyStatement() {
        return String.format("""
        INSERT
        INTO `%s` (`map_id`)
        VALUES (?)
        """, tableName("map"));
    }

    @Override
    @Language("sqlite")
    public String findCompressionKeyStatement() {
        return String.format("""
        SELECT `id`
        FROM `%s`
        WHERE `key` = ?
        """, tableName("compression"));
    }

    @Override
    @Language("sqlite")
    public String createCompressionKeyStatement() {
        return String.format("""
        INSERT
        INTO `%s` (`key`)
        VALUES (?)
        """, tableName("compression"));
    }

    @Override
    @Language("sqlite")
    public String findItemStorageKeyStatement() {
        return String.format("""
        SELECT `id`
        FROM `%s`
        WHERE `key` = ?
        """, tableName("item_storage"));
    }

    @Override
    @Language("sqlite")
    public String createItemStorageKeyStatement() {
        return String.format("""
        INSERT
        INTO `%s` (`key`)
        VALUES (?)
        """, tableName("item_storage"));
    }

    @Override
    @Language("sqlite")
    public String findGridStorageKeyStatement() {
        return String.format("""
        SELECT `id`
        FROM `%s`
        WHERE `key` = ?
        """, tableName("grid_storage"));
    }

    @Override
    @Language("sqlite")
    public String createGridStorageKeyStatement() {
        return String.format("""
        INSERT
        INTO `%s` (`key`)
        VALUES (?)
        """, tableName("grid_storage"));
    }

}
