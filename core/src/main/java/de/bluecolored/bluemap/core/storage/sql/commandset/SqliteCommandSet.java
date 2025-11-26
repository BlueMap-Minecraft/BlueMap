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

    public SqliteCommandSet(Database db) {
        super(db);
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
        return """
        CREATE TABLE IF NOT EXISTS `bluemap_map` (
         `id` INTEGER PRIMARY KEY AUTOINCREMENT,
         `map_id` TEXT UNIQUE NOT NULL
        ) STRICT
        """;
    }

    @Override
    @Language("sqlite")
    public String createCompressionTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS `bluemap_compression` (
         `id` INTEGER PRIMARY KEY AUTOINCREMENT,
         `key` TEXT UNIQUE NOT NULL
        ) STRICT
        """;
    }

    @Override
    @Language("sqlite")
    public String createItemStorageTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS `bluemap_item_storage` (
         `id` INTEGER PRIMARY KEY AUTOINCREMENT,
         `key` TEXT UNIQUE NOT NULL
        ) STRICT
        """;
    }

    @Override
    @Language("sqlite")
    public String createItemStorageDataTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS `bluemap_item_storage_data` (
         `map` INTEGER NOT NULL,
         `storage` INTEGER NOT NULL,
         `compression` INTEGER NOT NULL,
         `data` BLOB NOT NULL,
         PRIMARY KEY (`map`, `storage`),
         CONSTRAINT `fk_bluemap_item_map`
          FOREIGN KEY (`map`)
          REFERENCES `bluemap_map` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         CONSTRAINT `fk_bluemap_item`
          FOREIGN KEY (`storage`)
          REFERENCES `bluemap_item_storage` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         CONSTRAINT `fk_bluemap_item_compression`
          FOREIGN KEY (`compression`)
          REFERENCES `bluemap_compression` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE
        ) STRICT
        """;
    }

    @Override
    @Language("sqlite")
    public String createGridStorageTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS `bluemap_grid_storage` (
         `id` INTEGER PRIMARY KEY AUTOINCREMENT,
         `key` TEXT UNIQUE NOT NULL
        ) STRICT
        """;
    }

    @Override
    @Language("sqlite")
    public String createGridStorageDataTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS `bluemap_grid_storage_data` (
         `map` INTEGER NOT NULL,
         `storage` INTEGER NOT NULL,
         `x` INTEGER NOT NULL,
         `z` INTEGER NOT NULL,
         `compression` INTEGER NOT NULL,
         `data` BLOB NOT NULL,
         PRIMARY KEY (`map`, `storage`, `x`, `z`),
         CONSTRAINT `fk_bluemap_grid_map`
          FOREIGN KEY (`map`)
          REFERENCES `bluemap_map` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         CONSTRAINT `fk_bluemap_grid`
          FOREIGN KEY (`storage`)
          REFERENCES `bluemap_grid_storage` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         CONSTRAINT `fk_bluemap_grid_compression`
          FOREIGN KEY (`compression`)
          REFERENCES `bluemap_compression` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE
        ) STRICT
        """;
    }

    @Override
    @Language("sqlite")
    public String itemStorageWriteStatement() {
        return """
        REPLACE
        INTO `bluemap_item_storage_data` (`map`, `storage`, `compression`, `data`)
        VALUES (?, ?, ?, ?)
        """;
    }

    @Override
    @Language("sqlite")
    public String itemStorageReadStatement() {
        return """
        SELECT `data`
        FROM `bluemap_item_storage_data`
        WHERE `map` = ?
        AND `storage` = ?
        AND `compression` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String itemStorageDeleteStatement() {
        return """
        DELETE
        FROM `bluemap_item_storage_data`
        WHERE `map` = ?
        AND `storage` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String itemStorageHasStatement() {
        return """
        SELECT COUNT(*) > 0
        FROM `bluemap_item_storage_data`
        WHERE `map` = ?
        AND `storage` = ?
        AND `compression` = ?
        """;
    }


    @Override
    @Language("sqlite")
    public String gridStorageWriteStatement() {
        return """
        REPLACE
        INTO `bluemap_grid_storage_data` (`map`, `storage`, `x`, `z`, `compression`, `data`)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    }

    @Override
    @Language("sqlite")
    public String gridStorageReadStatement() {
        return """
        SELECT `data`
        FROM `bluemap_grid_storage_data`
        WHERE `map` = ?
        AND `storage` = ?
        AND `x` = ?
        AND `z` = ?
        AND `compression` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String gridStorageDeleteStatement() {
        return """
        DELETE
        FROM `bluemap_grid_storage_data`
        WHERE `map` = ?
        AND `storage` = ?
        AND `x` = ?
        AND `z` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String gridStorageHasStatement() {
        return """
        SELECT COUNT(*) > 0
        FROM `bluemap_grid_storage_data`
        WHERE `map` = ?
        AND `storage` = ?
        AND `x` = ?
        AND `z` = ?
        AND `compression` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String gridStorageListStatement() {
        return """
        SELECT `x`, `z`
        FROM `bluemap_grid_storage_data`
        WHERE `map` = ?
        AND `storage` = ?
        AND `compression` = ?
        LIMIT ? OFFSET ?
        """;
    }

    @Override
    @Language("sqlite")
    public String gridStorageCountMapItemsStatement() {
        return """
        SELECT COUNT(*)
        FROM `bluemap_grid_storage_data`
        WHERE `map` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String gridStoragePurgeMapStatement() {
        return """
        DELETE
        FROM `bluemap_grid_storage_data`
        WHERE ROWID IN (
         SELECT t.ROWID
         FROM `bluemap_grid_storage_data` t
         WHERE t.`map` = ?
         LIMIT ?
        )
        """;
    }

    @Override
    @Language("sqlite")
    public String purgeMapStatement() {
        return """
        DELETE
        FROM `bluemap_map`
        WHERE `id` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String hasMapStatement() {
        return """
        SELECT COUNT(*) > 0
        FROM `bluemap_map` m
        WHERE m.`map_id` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String listMapIdsStatement() {
        return """
        SELECT `map_id`
        FROM `bluemap_map` m
        LIMIT ? OFFSET ?
        """;
    }

    @Override
    @Language("sqlite")
    public String findMapKeyStatement() {
        return """
        SELECT `id`
        FROM `bluemap_map`
        WHERE map_id = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String createMapKeyStatement() {
        return """
        INSERT
        INTO `bluemap_map` (`map_id`)
        VALUES (?)
        """;
    }

    @Override
    @Language("sqlite")
    public String findCompressionKeyStatement() {
        return """
        SELECT `id`
        FROM `bluemap_compression`
        WHERE `key` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String createCompressionKeyStatement() {
        return """
        INSERT
        INTO `bluemap_compression` (`key`)
        VALUES (?)
        """;
    }

    @Override
    @Language("sqlite")
    public String findItemStorageKeyStatement() {
        return """
        SELECT `id`
        FROM `bluemap_item_storage`
        WHERE `key` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String createItemStorageKeyStatement() {
        return """
        INSERT
        INTO `bluemap_item_storage` (`key`)
        VALUES (?)
        """;
    }

    @Override
    @Language("sqlite")
    public String findGridStorageKeyStatement() {
        return """
        SELECT `id`
        FROM `bluemap_grid_storage`
        WHERE `key` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String createGridStorageKeyStatement() {
        return """
        INSERT
        INTO `bluemap_grid_storage` (`key`)
        VALUES (?)
        """;
    }

}
