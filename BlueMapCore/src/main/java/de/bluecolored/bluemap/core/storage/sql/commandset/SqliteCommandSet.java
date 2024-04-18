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
        CREATE TABLE IF NOT EXISTS `bluemap_map_tile_compression` (
         `id` INTEGER PRIMARY KEY AUTOINCREMENT,
         `compression` TEXT UNIQUE NOT NULL
        ) STRICT
        """;
    }

    @Override
    @Language("sqlite")
    public String createMapMetaTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS `bluemap_map_meta` (
         `map` INTEGER NOT NULL,
         `key` TEXT NOT NULL,
         `value` BLOB NOT NULL,
         PRIMARY KEY (`map`, `key`),
         CONSTRAINT `fk_bluemap_map_meta_map`
          FOREIGN KEY (`map`)
          REFERENCES `bluemap_map` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE
        ) STRICT
        """;
    }

    @Override
    @Language("sqlite")
    public String createMapTileTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS `bluemap_map_tile` (
         `map` INTEGER NOT NULL,
         `lod` INTEGER NOT NULL,
         `x` INTEGER NOT NULL,
         `z` INTEGER NOT NULL,
         `compression` INTEGER NOT NULL,
         `data` BLOB NOT NULL,
         PRIMARY KEY (`map`, `lod`, `x`, `z`),
         CONSTRAINT `fk_bluemap_map_tile_map`
          FOREIGN KEY (`map`)
          REFERENCES `bluemap_map` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         CONSTRAINT `fk_bluemap_map_tile_compression`
          FOREIGN KEY (`compression`)
          REFERENCES `bluemap_map_tile_compression` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE
        ) STRICT
        """;
    }

    @Override
    @Language("sqlite")
    public String fixLegacyCompressionIdsStatement() {
        return """
        UPDATE `bluemap_map_tile_compression`
        SET `compression` = 'bluemap:' || `compression`
        WHERE NOT `compression` LIKE '%:%'
        """;
    }

    @Override
    @Language("sqlite")
    public String writeMapTileStatement() {
        return """
        REPLACE
        INTO `bluemap_map_tile` (`map`, `lod`, `x`, `z`, `compression`, `data`)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    }

    @Override
    @Language("sqlite")
    public String readMapTileStatement() {
        return """
        SELECT t.`data`
        FROM `bluemap_map_tile` t
         INNER JOIN `bluemap_map` m
          ON t.`map` = m.`id`
         INNER JOIN `bluemap_map_tile_compression` c
          ON t.`compression` = c.`id`
        WHERE m.`map_id` = ?
        AND t.`lod` = ?
        AND t.`x` = ?
        AND t.`z` = ?
        AND c.`compression` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String deleteMapTileStatement() {
        return """
        DELETE
        FROM `bluemap_map_tile`
        WHERE `map` = ?
        AND `lod` = ?
        AND `x` = ?
        AND `z` = ?
        AND `compression` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String hasMapTileStatement() {
        return """
        SELECT COUNT(*) > 0
        FROM `bluemap_map_tile` t
         INNER JOIN `bluemap_map` m
          ON t.`map` = m.`id`
         INNER JOIN `bluemap_map_tile_compression` c
          ON t.`compression` = c.`id`
        WHERE m.`map_id` = ?
        AND t.`lod` = ?
        AND t.`x` = ?
        AND t.`z` = ?
        AND c.`compression` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String countAllMapTilesStatement() {
        return """
        SELECT COUNT(*)
        FROM `bluemap_map_tile` t
         INNER JOIN `bluemap_map` m
          ON t.`map` = m.`id`
        WHERE m.`map_id` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String purgeMapTilesStatement() {
        return """
        DELETE
        FROM bluemap_map_tile
        WHERE ROWID IN (
         SELECT t.ROWID
         FROM bluemap_map_tile t
         WHERE t.map = ?
         LIMIT ?
        )
        """;
    }

    @Override
    @Language("sqlite")
    public String listMapTilesStatement() {
        return """
        SELECT t.`x`, t.`z`
        FROM `bluemap_map_tile` t
         INNER JOIN `bluemap_map` m
          ON t.`map` = m.`id`
         INNER JOIN `bluemap_map_tile_compression` c
          ON t.`compression` = c.`id`
        WHERE m.`map_id` = ?
        AND t.`lod` = ?
        AND c.`compression` = ?
        LIMIT ? OFFSET ?
        """;
    }

    @Override
    @Language("sqlite")
    public String writeMapMetaStatement() {
        return """
        REPLACE
        INTO `bluemap_map_meta` (`map`, `key`, `value`)
        VALUES (?, ?, ?)
        """;
    }

    @Override
    @Language("sqlite")
    public String readMapMetaStatement() {
        return """
        SELECT t.`value`
        FROM `bluemap_map_meta` t
         INNER JOIN `bluemap_map` m
          ON t.`map` = m.`id`
        WHERE m.`map_id` = ?
        AND t.`key` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String deleteMapMetaStatement() {
        return """
        DELETE
        FROM `bluemap_map_meta`
        WHERE `map` = ?
        AND `key` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String hasMapMetaStatement() {
        return """
        SELECT COUNT(*) > 0
        FROM `bluemap_map_meta` t
         INNER JOIN `bluemap_map` m
          ON t.`map` = m.`id`
        WHERE m.`map_id` = ?
        AND t.`key` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String purgeMapTileTableStatement() {
        return """
        DELETE
        FROM `bluemap_map_tile`
        WHERE `map` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String purgeMapMetaTableStatement() {
        return """
        DELETE
        FROM `bluemap_map_meta`
        WHERE `map` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String deleteMapStatement() {
        return """
        DELETE
        FROM `bluemap_map`
        WHERE `map` = ?
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
        FROM `bluemap_map_tile_compression`
        WHERE `compression` = ?
        """;
    }

    @Override
    @Language("sqlite")
    public String createCompressionKeyStatement() {
        return """
        INSERT
        INTO `bluemap_map_tile_compression` (`compression`)
        VALUES (?)
        """;
    }

}
