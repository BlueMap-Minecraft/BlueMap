package de.bluecolored.bluemap.core.storage.sql.dialect;

import org.intellij.lang.annotations.Language;

public class PostgresDialect implements Dialect {

    public static final PostgresDialect INSTANCE = new PostgresDialect();

    private PostgresDialect() {};

    @Override
    @Language("PostgreSQL")
    public String writeMapTile() {
        return "INSERT INTO bluemap_map_tile (map, lod, x, z, compression, data) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (map, lod, x, z) DO UPDATE SET compression = EXCLUDED.compression, data = EXCLUDED.data";
    }

    @Override
    @Language("PostgreSQL")
    public String readMapTile() {
        return "SELECT t.data " +
                "FROM bluemap_map_tile t " +
                " INNER JOIN bluemap_map m " +
                "  ON t.map = m.id " +
                " INNER JOIN bluemap_map_tile_compression c " +
                "  ON t.compression = c.id " +
                "WHERE m.map_id = ? " +
                "AND t.lod = ? " +
                "AND t.x = ? " +
                "AND t.z = ? " +
                "AND c.compression = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String readMapTileInfo() {
        return "SELECT t.changed, OCTET_LENGTH(t.data) as size " +
                "FROM bluemap_map_tile t " +
                " INNER JOIN bluemap_map m " +
                "  ON t.map = m.id " +
                " INNER JOIN bluemap_map_tile_compression c " +
                "  ON t.compression = c.id " +
                "WHERE m.map_id = ? " +
                "AND t.lod = ? " +
                "AND t.x = ? " +
                "AND t.z = ? " +
                "AND c.compression = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String deleteMapTile() {
        return "DELETE FROM bluemap_map_tile t " +
                "USING bluemap_map m " +
                "WHERE t.map = m.id " +
                "AND m.map_id = ? " +
                "AND t.lod = ? " +
                "AND t.x = ? " +
                "AND t.z = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String writeMeta() {
        return "INSERT INTO bluemap_map_meta (map, key, value) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT (map, key) DO UPDATE SET value = EXCLUDED.value";
    }

    @Override
    @Language("PostgreSQL")
    public String readMeta() {
        return "SELECT t.value " +
                "FROM bluemap_map_meta t " +
                " INNER JOIN bluemap_map m " +
                "  ON t.map = m.id " +
                "WHERE m.map_id = ? " +
                "AND t.key = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String readMetaSize() {
        return "SELECT OCTET_LENGTH(t.value) as size " +
                "FROM bluemap_map_meta t " +
                " INNER JOIN bluemap_map m " +
                "  ON t.map = m.id " +
                "WHERE m.map_id = ? " +
                "AND t.key = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String deleteMeta() {
        return "DELETE FROM bluemap_map_meta t " +
                "USING bluemap_map m " +
                "WHERE t.map = m.id " +
                "AND m.map_id = ? " +
                "AND t.key = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String purgeMapTile() {
        return "DELETE FROM bluemap_map_tile t " +
                "USING bluemap_map m " +
                "WHERE t.map = m.id " +
                "AND m.map_id = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String purgeMapMeta() {
        return "DELETE FROM bluemap_map_meta t " +
                "USING bluemap_map m " +
                "WHERE t.map = m.id " +
                "AND m.map_id = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String purgeMap() {
        return "DELETE FROM bluemap_map " +
                "WHERE map_id = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String selectMapIds() {
        return "SELECT map_id FROM bluemap_map";
    }

    @Override
    @Language("PostgreSQL")
    public String initializeStorageMeta() {
        return "CREATE TABLE IF NOT EXISTS bluemap_storage_meta (" +
                "key varchar(255) PRIMARY KEY, " +
                "value varchar(255)" +
                ")";
    }

    @Override
    @Language("PostgreSQL")
    public String selectStorageMeta() {
        return "SELECT value FROM bluemap_storage_meta " +
                "WHERE key = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String insertStorageMeta() {
        return "INSERT INTO bluemap_storage_meta (key, value) " +
                "VALUES (?, ?) " +
                "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value";
    }

    @Override
    @Language("PostgreSQL")
    public String initializeMap() {
        return "CREATE TABLE IF NOT EXISTS bluemap_map (" +
                "id SERIAL PRIMARY KEY, " +
                "map_id VARCHAR(255) UNIQUE NOT NULL" +
                ")";
    }

    @Override
    @Language("PostgreSQL")
    public String initializeMapTileCompression() {
        return "CREATE TABLE IF NOT EXISTS bluemap_map_tile_compression (" +
                "id SERIAL PRIMARY KEY, " +
                "compression VARCHAR(255) UNIQUE NOT NULL" +
                ")";
    }

    @Override
    @Language("PostgreSQL")
    public String initializeMapMeta() {
        return "CREATE TABLE IF NOT EXISTS bluemap_map_meta (" +
                "map SMALLINT REFERENCES bluemap_map(id) ON UPDATE RESTRICT ON DELETE RESTRICT, " +
                "key varchar(255) NOT NULL, " +
                "value BYTEA NOT NULL, " +
                "PRIMARY KEY (map, key)" +
                ")";
    }

    @Override
    @Language("PostgreSQL")
    public String initializeMapTile() {
        return "CREATE TABLE IF NOT EXISTS bluemap_map_tile (" +
                "map SMALLINT REFERENCES bluemap_map(id) ON UPDATE RESTRICT ON DELETE RESTRICT, " +
                "lod SMALLINT NOT NULL, " +
                "x INT NOT NULL, " +
                "z INT NOT NULL, " +
                "compression SMALLINT REFERENCES bluemap_map_tile_compression(id) ON UPDATE RESTRICT ON DELETE RESTRICT, " +
                "changed TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "data BYTEA NOT NULL, " +
                "PRIMARY KEY (map, lod, x, z)" +
                ")";
    }

    @Override
    @Language("PostgreSQL")
    public String updateStorageMeta() {
        return "UPDATE bluemap_storage_meta " +
                "SET value = ? " +
                "WHERE key = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String deleteMapMeta() {
        return "DELETE FROM bluemap_map_meta " +
                "WHERE key IN (?, ?, ?)";
    }

    @Override
    @Language("PostgreSQL")
    public String updateMapMeta() {
        return "UPDATE bluemap_map_meta " +
                "SET key = ? " +
                "WHERE key = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String lookupFK(String table, String idField, String valueField) {
        return "SELECT " + idField + " FROM " + table +
                " WHERE " + valueField + " = ?";
    }

    @Override
    @Language("PostgreSQL")
    public String insertFK(String table, String valueField) {
        return "INSERT INTO " + table + " (" + valueField + ") " +
                "VALUES (?)";
    }


}
