package de.bluecolored.bluemap.core.storage.sql.commandset;

import de.bluecolored.bluemap.core.storage.compression.Compression;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;

public interface CommandSet extends Closeable {

    void initializeTables() throws IOException;

    int writeMapTile(
            String mapId, int lod, int x, int z, Compression compression,
            byte[] bytes
    ) throws IOException;

    byte @Nullable [] readMapTile(
            String mapId, int lod, int x, int z, Compression compression
    ) throws IOException;

    int deleteMapTile(
            String mapId, int lod, int x, int z, Compression compression
    ) throws IOException;

    boolean hasMapTile(
            String mapId, int lod, int x, int z, Compression compression
    ) throws IOException;

    TilePosition[] listMapTiles(
            String mapId, int lod, Compression compression,
            int start, int count
    ) throws IOException;

    int countAllMapTiles(String mapId) throws IOException;

    int purgeMapTiles(String mapId, int limit) throws IOException;

    int writeMapMeta(String mapId, String itemName, byte[] bytes) throws IOException;

    byte @Nullable [] readMapMeta(String mapId, String itemName) throws IOException;

    int deleteMapMeta(String mapId, String itemName) throws IOException;

    boolean hasMapMeta(String mapId, String itemName) throws IOException;

    void purgeMap(String mapId) throws IOException;

    boolean hasMap(String mapId) throws IOException;

    String[] listMapIds(int start, int count) throws IOException;

    boolean isClosed();

    record TilePosition (int x, int z) {}

}
