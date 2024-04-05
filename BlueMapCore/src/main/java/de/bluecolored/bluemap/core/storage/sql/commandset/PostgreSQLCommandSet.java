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

    public PostgreSQLCommandSet(Database db) {
        super(db);
    }

    @Override
    @Language("postgresql")
    public String createMapTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS bluemap_map (
         id SMALLSERIAL PRIMARY KEY,
         map_id VARCHAR(190) UNIQUE NOT NULL
        )
        """;
    }

    @Override
    @Language("postgresql")
    public String createCompressionTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS bluemap_map_tile_compression (
         id SMALLSERIAL PRIMARY KEY,
         compression VARCHAR(190) UNIQUE NOT NULL
        )
        """;
    }

    @Override
    @Language("postgresql")
    public String createMapMetaTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS bluemap_map_meta (
         map SMALLINT NOT NULL
          REFERENCES bluemap_map(id)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         key VARCHAR(190) NOT NULL,
         value BYTEA NOT NULL,
         PRIMARY KEY (map, key)
        )
        """;
    }

    @Override
    @Language("postgresql")
    public String createMapTileTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS bluemap_map_tile (
         map SMALLINT NOT NULL
          REFERENCES bluemap_map (id)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         lod SMALLINT NOT NULL,
         x INT NOT NULL,
         z INT NOT NULL,
         compression SMALLINT NOT NULL
          REFERENCES bluemap_map_tile_compression (id)
          ON UPDATE RESTRICT
          ON DELETE CASCADE,
         data BYTEA NOT NULL,
         PRIMARY KEY (map, lod, x, z)
        )
        """;
    }

    @Override
    @Language("postgresql")
    public String fixLegacyCompressionIdsStatement() {
        return """
        UPDATE bluemap_map_tile_compression
        SET compression = CONCAT('bluemap:', compression)
        WHERE NOT compression LIKE '%:%'
        """;
    }

    @Override
    @Language("postgresql")
    public String writeMapTileStatement() {
        return """
        INSERT
        INTO bluemap_map_tile (map, lod, x, z, compression, data)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (map, lod, x, z)
         DO UPDATE SET
          compression = excluded.compression,
          data = excluded.data
        """;
    }

    @Override
    @Language("postgresql")
    public String readMapTileStatement() {
        return """
        SELECT t.data
        FROM bluemap_map_tile t
         INNER JOIN bluemap_map m
          ON t.map = m.id
         INNER JOIN bluemap_map_tile_compression c
          ON t.compression = c.id
        WHERE m.map_id = ?
        AND t.lod = ?
        AND t.x = ?
        AND t.z = ?
        AND c.compression = ?
        """;
    }

    @Override
    @Language("postgresql")
    public String deleteMapTileStatement() {
        return """
        DELETE
        FROM bluemap_map_tile
        WHERE map IN (
         SELECT m.id
         FROM bluemap_map m
         WHERE m.map_id = ?
        )
        AND lod = ?
        AND x = ?
        AND z = ?
        AND compression IN (
         SELECT c.id
         FROM bluemap_map_tile_compression c
         WHERE c.compression = ?
        )
        """;
    }

    @Override
    @Language("postgresql")
    public String hasMapTileStatement() {
        return """
        SELECT COUNT(*) > 0
        FROM bluemap_map_tile t
         INNER JOIN bluemap_map m
          ON t.map = m.id
         INNER JOIN bluemap_map_tile_compression c
          ON t.compression = c.id
        WHERE m.map_id = ?
        AND t.lod = ?
        AND t.x = ?
        AND t.z = ?
        AND c.compression = ?
        """;
    }

    @Override
    @Language("postgresql")
    public String countAllMapTilesStatement() {
        return """
        SELECT COUNT(*)
        FROM bluemap_map_tile t
         INNER JOIN bluemap_map m
          ON t.map = m.id
        WHERE m.map_id = ?
        """;
    }

    @Override
    @Language("postgresql")
    public String purgeMapTilesStatement() {
        return """
        DELETE
        FROM bluemap_map_tile
        WHERE CTID IN (
         SELECT CTID
         FROM bluemap_map_tile t
          INNER JOIN bluemap_map m
           ON t.map = m.id
         WHERE m.map_id = ?
         LIMIT ?
        )
        """;
    }

    @Override
    @Language("postgresql")
    public String listMapTilesStatement() {
        return """
        SELECT t.x, t.z
        FROM bluemap_map_tile t
         INNER JOIN bluemap_map m
          ON t.map = m.id
         INNER JOIN bluemap_map_tile_compression c
          ON t.compression = c.id
        WHERE m.map_id = ?
        AND t.lod = ?
        AND c.compression = ?
        LIMIT ? OFFSET ?
        """;
    }

    @Override
    @Language("postgresql")
    public String writeMapMetaStatement() {
        return """
        INSERT
        INTO bluemap_map_meta (map, key, value)
        VALUES (?, ?, ?)
        ON CONFLICT (map, key)
         DO UPDATE SET
          value = excluded.value
        """;
    }

    @Override
    @Language("postgresql")
    public String readMapMetaStatement() {
        return """
        SELECT t.value
        FROM bluemap_map_meta t
         INNER JOIN bluemap_map m
          ON t.map = m.id
        WHERE m.map_id = ?
        AND t.key = ?
        """;
    }

    @Override
    @Language("postgresql")
    public String deleteMapMetaStatement() {
        return """
        DELETE
        FROM bluemap_map_meta
        WHERE map IN (
         SELECT id
         FROM bluemap_map
         WHERE map_id = ?
        )
        AND key = ?
        """;
    }

    @Override
    @Language("postgresql")
    public String hasMapMetaStatement() {
        return """
        SELECT COUNT(*) > 0
        FROM bluemap_map_meta t
         INNER JOIN bluemap_map m
          ON t.map = m.id
        WHERE m.map_id = ?
        AND t.key = ?
        """;
    }

    @Override
    @Language("postgresql")
    public String purgeMapTileTableStatement() {
        return """
        DELETE
        FROM bluemap_map_tile
        WHERE map IN (
         SELECT m.id
         FROM bluemap_map m
         WHERE m.map_id = ?
        )
        """;
    }

    @Override
    @Language("postgresql")
    public String purgeMapMetaTableStatement() {
        return """
        DELETE
        FROM bluemap_map_meta
        WHERE map IN (
         SELECT m.id
         FROM bluemap_map m
         WHERE m.map_id = ?
        )
        """;
    }

    @Override
    @Language("postgresql")
    public String deleteMapStatement() {
        return """
        DELETE
        FROM bluemap_map
        WHERE map_id = ?
        """;
    }

    @Override
    @Language("postgresql")
    public String hasMapStatement() {
        return """
        SELECT COUNT(*) > 0
        FROM bluemap_map m
        WHERE m.map_id = ?
        """;
    }

    @Override
    @Language("postgresql")
    public String listMapIdsStatement() {
        return """
        SELECT map_id
        FROM bluemap_map m
        LIMIT ? OFFSET ?
        """;
    }

    @Override
    @Language("postgresql")
    public String findMapKeyStatement() {
        return """
        SELECT id
        FROM bluemap_map
        WHERE map_id = ?
        """;
    }

    @Override
    @Language("postgresql")
    public String createMapKeyStatement() {
        return """
        INSERT
        INTO bluemap_map (map_id)
        VALUES (?)
        """;
    }

    @Override
    @Language("postgresql")
    public String findCompressionKeyStatement() {
        return """
        SELECT id
        FROM bluemap_map_tile_compression
        WHERE compression = ?
        """;
    }

    @Override
    @Language("postgresql")
    public String createCompressionKeyStatement() {
        return """
        INSERT
        INTO bluemap_map_tile_compression (compression)
        VALUES (?)
        """;
    }

}
