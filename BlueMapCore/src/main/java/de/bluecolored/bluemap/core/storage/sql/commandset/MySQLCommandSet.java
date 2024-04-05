package de.bluecolored.bluemap.core.storage.sql.commandset;

import de.bluecolored.bluemap.core.storage.sql.Database;
import org.intellij.lang.annotations.Language;

public class MySQLCommandSet extends AbstractCommandSet {

    public MySQLCommandSet(Database db) {
        super(db);
    }

    @Override
    @Language("mysql")
    public String createMapTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS `bluemap_map` (
         `id` SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
         `map_id` VARCHAR(190) NOT NULL,
         PRIMARY KEY (`id`),
         UNIQUE INDEX `map_id` (`map_id`)
        ) COLLATE 'utf8mb4_bin'
        """;
    }

    @Override
    @Language("mysql")
    public String createCompressionTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS `bluemap_map_tile_compression` (
         `id` SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
         `compression` VARCHAR(190) NOT NULL,
         PRIMARY KEY (`id`),
         UNIQUE INDEX `compression` (`compression`)
        ) COLLATE 'utf8mb4_bin'
        """;
    }

    @Override
    @Language("mysql")
    public String createMapMetaTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS `bluemap_map_meta` (
         `map` SMALLINT UNSIGNED NOT NULL,
         `key` varchar(190) NOT NULL,
         `value` LONGBLOB NOT NULL,
         PRIMARY KEY (`map`, `key`),
         CONSTRAINT `fk_bluemap_map_meta_map`
          FOREIGN KEY (`map`)
          REFERENCES `bluemap_map` (`id`)
          ON UPDATE RESTRICT
          ON DELETE CASCADE
        ) COLLATE 'utf8mb4_bin'
        """;
    }

    @Override
    @Language("mysql")
    public String createMapTileTableStatement() {
        return """
        CREATE TABLE IF NOT EXISTS `bluemap_map_tile` (
         `map` SMALLINT UNSIGNED NOT NULL,
         `lod` SMALLINT UNSIGNED NOT NULL,
         `x` INT NOT NULL,
         `z` INT NOT NULL,
         `compression` SMALLINT UNSIGNED NOT NULL,
         `data` LONGBLOB NOT NULL,
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
        ) COLLATE 'utf8mb4_bin'
        """;
    }

    @Override
    @Language("mysql")
    public String fixLegacyCompressionIdsStatement() {
        return """
        UPDATE IGNORE `bluemap_map_tile_compression`
        SET `compression` = CONCAT('bluemap:', `compression`)
        WHERE NOT `compression` LIKE '%:%'
        """;
    }

    @Override
    @Language("mysql")
    public String writeMapTileStatement() {
        return """
        REPLACE
        INTO `bluemap_map_tile` (`map`, `lod`, `x`, `z`, `compression`, `data`)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    }

    @Override
    @Language("mysql")
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
    @Language("mysql")
    public String deleteMapTileStatement() {
        return """
        DELETE
        FROM `bluemap_map_tile`
        WHERE `map` IN (
         SELECT m.`id`
         FROM `bluemap_map` m
         WHERE m.`map_id` = ?
        )
        AND `lod` = ?
        AND `x` = ?
        AND `z` = ?
        AND `compression` IN (
         SELECT c.`id`
         FROM `bluemap_map_tile_compression` c
         WHERE c.`compression` = ?
        )
        """;
    }

    @Override
    @Language("mysql")
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
    @Language("mysql")
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
    @Language("mysql")
    public String purgeMapTilesStatement() {
        return """
        DELETE
        FROM `bluemap_map_tile`
        WHERE `map` IN (
         SELECT `id`
         FROM `bluemap_map`
         WHERE `map_id` = ?
        )
        LIMIT ?
        """;
    }

    @Override
    @Language("mysql")
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
    @Language("mysql")
    public String writeMapMetaStatement() {
        return """
        REPLACE
        INTO `bluemap_map_meta` (`map`, `key`, `value`)
        VALUES (?, ?, ?)
        """;
    }

    @Override
    @Language("mysql")
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
    @Language("mysql")
    public String deleteMapMetaStatement() {
        return """
        DELETE
        FROM `bluemap_map_meta`
        WHERE `map` IN (
         SELECT `id`
         FROM `bluemap_map`
         WHERE `map_id` = ?
        )
        AND `key` = ?
        """;
    }

    @Override
    @Language("mysql")
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
    @Language("mysql")
    public String purgeMapTileTableStatement() {
        return """
        DELETE
        FROM `bluemap_map_tile`
        WHERE `map` IN (
         SELECT m.`id`
         FROM `bluemap_map` m
         WHERE m.`map_id` = ?
        )
        """;
    }

    @Override
    @Language("mysql")
    public String purgeMapMetaTableStatement() {
        return """
        DELETE
        FROM `bluemap_map_meta`
        WHERE `map` IN (
         SELECT m.`id`
         FROM `bluemap_map` m
         WHERE m.`map_id` = ?
        )
        """;
    }

    @Override
    @Language("mysql")
    public String deleteMapStatement() {
        return """
        DELETE
        FROM `bluemap_map`
        WHERE `map_id` = ?
        """;
    }

    @Override
    @Language("mysql")
    public String hasMapStatement() {
        return """
        SELECT COUNT(*) > 0
        FROM `bluemap_map` m
        WHERE m.`map_id` = ?
        """;
    }

    @Override
    @Language("mysql")
    public String listMapIdsStatement() {
        return """
        SELECT `map_id`
        FROM `bluemap_map` m
        LIMIT ? OFFSET ?
        """;
    }

    @Override
    @Language("mysql")
    public String findMapKeyStatement() {
        return """
        SELECT `id`
        FROM `bluemap_map`
        WHERE map_id = ?
        """;
    }

    @Override
    @Language("mysql")
    public String createMapKeyStatement() {
        return """
        INSERT
        INTO `bluemap_map` (`map_id`)
        VALUES (?)
        """;
    }

    @Override
    @Language("mysql")
    public String findCompressionKeyStatement() {
        return """
        SELECT `id`
        FROM `bluemap_map_tile_compression`
        WHERE `compression` = ?
        """;
    }

    @Override
    @Language("mysql")
    public String createCompressionKeyStatement() {
        return """
        INSERT
        INTO `bluemap_map_tile_compression` (`compression`)
        VALUES (?)
        """;
    }

}
