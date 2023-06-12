package de.bluecolored.bluemap.core.storage.sql.dialect;

import org.intellij.lang.annotations.Language;

public interface SQLQueryDialect {
    @Language("sql")
    String writeMapTile();

    @Language("sql")
    String readMapTile();

    @Language("sql")
    String readMapTileInfo();

    @Language("sql")
    String deleteMapTile();

    @Language("sql")
    String writeMeta();

    @Language("sql")
    String readMeta();

    @Language("sql")
    String readMetaSize();

    @Language("sql")
    String purgeMeta();

    @Language("sql")
    String purgeMapTile();

    @Language("sql")
    String purgeMapMeta();

    @Language("sql")
    String purgeMap();

    @Language("sql")
    String selectMapIds();

    @Language("sql")
    String initializeStorageMeta();

    @Language("sql")
    String selectStorageMeta();

    @Language("sql")
    String insertStorageMeta();

    @Language("sql")
    String initializeMap();

    @Language("sql")
    String initializeMapTileCompression();

    @Language("sql")
    String initializeMapMeta();

    @Language("sql")
    String initializeMapTile();

    @Language("sql")
    String updateStorageMeta(); // can be use twice in init

    @Language("sql")
    String deleteMapMeta();

    @Language("sql")
    String updateMapMeta(); // can be used twice in init

    @Language("sql")
    String lookupFK(String table, String idField, String valueField);

    @Language("sql")
    String insertFK(String table, String valueField);

}
