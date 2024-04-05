package de.bluecolored.bluemap.core.storage.sql;

import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.storage.sql.commandset.CommandSet;
import de.bluecolored.bluemap.core.util.stream.OnCloseOutputStream;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
public class SQLTileStorage implements GridStorage {

    private final CommandSet sql;
    private final String mapId;
    private final int lod;
    private final Compression compression;

    @Override
    public OutputStream write(int x, int z) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        return new OnCloseOutputStream(compression.compress(bytes),
                () -> sql.writeMapTile(mapId, lod, x, z, compression, bytes.toByteArray())
        );
    }

    @Override
    public @Nullable CompressedInputStream read(int x, int z) throws IOException {
        byte[] data = sql.readMapTile(mapId, lod, x, z, compression);
        if (data == null) return null;
        return new CompressedInputStream(new ByteArrayInputStream(data), compression);
    }

    @Override
    public void delete(int x, int z) throws IOException {
        sql.deleteMapTile(mapId, lod, x, z, compression);
    }

    @Override
    public boolean exists(int x, int z) throws IOException {
        return sql.hasMapTile(mapId, lod, x, z, compression);
    }

    @Override
    public Stream<Cell> stream() throws IOException {
        return StreamSupport.stream(
                new PageSpliterator<>(page -> {
                    try {
                        return sql.listMapTiles(mapId, lod, compression, page * 1000, 1000);
                    } catch (IOException ex) { throw new RuntimeException(ex); }
                }),
                false
        ).map(tilePosition -> new GridStorageCell(this, tilePosition.x(), tilePosition.z()));
    }

    @Override
    public boolean isClosed() {
        return sql.isClosed();
    }

}
