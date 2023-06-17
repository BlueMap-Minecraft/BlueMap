package de.bluecolored.bluemap.core.storage.sql.dialect;

import org.intellij.lang.annotations.Language;

public class SqliteDialect implements Dialect {

    public static final SqliteDialect INSTANCE = new SqliteDialect();

    private SqliteDialect() {}

    @Override
    @Language("sqlite")
    public String writeMapTile() {
        return "REPLACE INTO `bluemap_map_tile` (`map`, `lod`, `x`, `z`, `compression`, `data`) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
    }

    @Override
    @Language("sqlite")
    public String readMapTile() {
        return "SELECT t.`data` " +
                "FROM `bluemap_map_tile` t " +
                " INNER JOIN `bluemap_map` m " +
                "  ON t.`map` = m.`id` " +
                " INNER JOIN `bluemap_map_tile_compression` c " +
                "  ON t.`compression` = c.`id` " +
                "WHERE m.`map_id` = ? " +
                "AND t.`lod` = ? " +
                "AND t.`x` = ? " +
                "AND t.`z` = ? " +
                "AND c.`compression` = ?";
    }

    @Override
    @Language("sqlite")
    public String readMapTileInfo() {
        return "SELECT t.`changed`, LENGTH(t.`data`) as 'size' " +
                "FROM `bluemap_map_tile` t " +
                " INNER JOIN `bluemap_map` m " +
                "  ON t.`map` = m.`id` " +
                " INNER JOIN `bluemap_map_tile_compression` c " +
                "  ON t.`compression` = c.`id` " +
                "WHERE m.`map_id` = ? " +
                "AND t.`lod` = ? " +
                "AND t.`x` = ? " +
                "AND t.`z` = ? " +
                "AND c.`compression` = ?";
    }

    @Override
    @Language("sqlite")
    public String deleteMapTile() {
        return "DELETE FROM `bluemap_map_tile` " +
                "WHERE `map` IN( " +
                " SELECT `id` " +
                " FROM `bluemap_map` " +
                " WHERE `map_id` = ? " +
                " LIMIT 1 " +
                ") " +
                "AND `lod` = ? " +
                "AND `x` = ? " +
                "AND `z` = ? ";
    }

    @Override
    @Language("sqlite")
    public String writeMeta() {
        return "REPLACE INTO `bluemap_map_meta` (`map`, `key`, `value`) " +
                "VALUES (?, ?, ?)";
    }

    @Override
    @Language("sqlite")
    public String readMeta() {
        return "SELECT t.`value` " +
                "FROM `bluemap_map_meta` t " +
                " INNER JOIN `bluemap_map` m " +
                "  ON t.`map` = m.`id` " +
                "WHERE m.`map_id` = ? " +
                "AND t.`key` = ?";
    }

    @Override
    @Language("sqlite")
    public String readMetaSize() {
        return "SELECT LENGTH(t.`value`) as 'size' " +
                "FROM `bluemap_map_meta` t " +
                " INNER JOIN `bluemap_map` m " +
                "  ON t.`map` = m.`id` " +
                "WHERE m.`map_id` = ? " +
                "AND t.`key` = ?";
    }

    @Override
    @Language("sqlite")
    public String deleteMeta() {
        return "DELETE FROM `bluemap_map_meta` " +
                "WHERE `map` IN( " +
                " SELECT `id` " +
                " FROM `bluemap_map` " +
                " WHERE `map_id` = ? " +
                " LIMIT 1 " +
                ") " +
                "AND `key` = ?";
    }

    @Override
    @Language("sqlite")
    public String purgeMapTile() {
        return "DELETE FROM `bluemap_map_tile` " +
                "WHERE `map` IN( " +
                " SELECT `id` " +
                " FROM `bluemap_map` " +
                " WHERE `map_id` = ? " +
                " LIMIT 1 " +
                ")";
    }

    @Override
    @Language("sqlite")
    public String purgeMapMeta() {
        return "DELETE FROM `bluemap_map_meta` " +
                "WHERE `map` IN( " +
                " SELECT `id` " +
                " FROM `bluemap_map` " +
                " WHERE `map_id` = ? " +
                " LIMIT 1 " +
                ")";
    }

    @Override
    @Language("sqlite")
    public String purgeMap() {
        return "DELETE " +
                "FROM `bluemap_map` " +
                "WHERE `map_id` = ?";
    }

    @Override
    @Language("sqlite")
    public String selectMapIds() {
        return "SELECT `map_id` FROM `bluemap_map`";
    }

    @Override
    @Language("sqlite")
    public String initializeStorageMeta() {
        return "CREATE TABLE IF NOT EXISTS `bluemap_storage_meta` (" +
                "`key` varchar(255) NOT NULL, " +
                "`value` varchar(255) DEFAULT NULL, " +
                "PRIMARY KEY (`key`)" +
                ")";
    }

    @Override
    @Language("sqlite")
    public String selectStorageMeta() {
        return "SELECT `value` FROM `bluemap_storage_meta` " +
                "WHERE `key` = ?";
    }

    @Override
    @Language("sqlite")
    public String insertStorageMeta() {
        return "INSERT INTO `bluemap_storage_meta` (`key`, `value`) " +
                "VALUES (?, ?)";
    }

    @Override
    @Language("sqlite")
    public String initializeMap() {
        return "CREATE TABLE `bluemap_map` (" +
                "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "`map_id` VARCHAR(255) NOT NULL," +
                "UNIQUE (`map_id`)" +
                ");";
    }

    @Override
    @Language("sqlite")
    public String initializeMapTileCompression() {
        return "CREATE TABLE `bluemap_map_tile_compression` (" +
                "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "`compression` VARCHAR(255) NOT NULL," +
                "UNIQUE (`compression`)" +
                ");";
    }

    @Override
    @Language("sqlite")
    public String initializeMapMeta() {
        return "CREATE TABLE `bluemap_map_meta` (" +
                "`map` SMALLINT UNSIGNED NOT NULL," +
                "`key` varchar(255) NOT NULL," +
                "`value` LONGBLOB NOT NULL," +
                "PRIMARY KEY (`map`, `key`)," +
                "CONSTRAINT `fk_bluemap_map_meta_map` FOREIGN KEY (`map`) REFERENCES `bluemap_map` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT" +
                ")";
    }

    @Override
    @Language("sqlite")
    public String initializeMapTile() {
        return "CREATE TABLE `bluemap_map_tile` (" +
                "`map` SMALLINT UNSIGNED NOT NULL," +
                "`lod` SMALLINT UNSIGNED NOT NULL," +
                "`x` INT NOT NULL," +
                "`z` INT NOT NULL," +
                "`compression` SMALLINT UNSIGNED NOT NULL," +
                "`changed` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "`data` LONGBLOB NOT NULL," +
                "PRIMARY KEY (`map`, `lod`, `x`, `z`)," +
                "CONSTRAINT `fk_bluemap_map_tile_map` FOREIGN KEY (`map`) REFERENCES `bluemap_map` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT," +
                "CONSTRAINT `fk_bluemap_map_tile_compression` FOREIGN KEY (`compression`) REFERENCES `bluemap_map_tile_compression` (`id`) ON UPDATE RESTRICT ON DELETE RESTRICT" +
                ");";
    }

    @Override
    @Language("sqlite")
    public String updateStorageMeta() {
        return "UPDATE `bluemap_storage_meta` " +
                "SET `value` = ? " +
                "WHERE `key` = ?";
    }

    @Override
    @Language("sqlite")
    public String deleteMapMeta() {
        return "DELETE FROM `bluemap_map_meta`" +
                "WHERE `key` IN (?, ?, ?)";
    }

    @Override
    @Language("sqlite")
    public String updateMapMeta() {
        return "UPDATE `bluemap_map_meta` " +
                "SET `key` = ? " +
                "WHERE `key` = ?";
    }

    @Override
    @Language("sqlite")
    public String lookupFK(String table, String idField, String valueField) {
        return "SELECT `" + idField + "` FROM `" + table + "` " +
                "WHERE `" + valueField + "` = ?";
    }

    @Override
    @Language("sqlite")
    public String insertFK(String table, String valueField) {
        return "INSERT INTO `" + table + "` (`" + valueField + "`) " +
                "VALUES (?)";
    }


}
