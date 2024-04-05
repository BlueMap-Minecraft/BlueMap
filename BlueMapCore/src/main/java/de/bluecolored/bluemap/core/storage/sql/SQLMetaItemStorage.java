package de.bluecolored.bluemap.core.storage.sql;

import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.storage.SingleItemStorage;
import de.bluecolored.bluemap.core.storage.sql.commandset.CommandSet;
import de.bluecolored.bluemap.core.util.stream.OnCloseOutputStream;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.*;

@RequiredArgsConstructor
public class SQLMetaItemStorage implements SingleItemStorage {

    private final CommandSet sql;
    private final String mapId;
    private final String itemName;
    private final Compression compression;

    @Override
    public OutputStream write() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        return new OnCloseOutputStream(compression.compress(bytes),
                () -> sql.writeMapMeta(mapId, itemName, bytes.toByteArray())
        );
    }

    @Override
    public @Nullable CompressedInputStream read() throws IOException {
        byte[] data = sql.readMapMeta(mapId, itemName);
        if (data == null) return null;
        return new CompressedInputStream(new ByteArrayInputStream(data), compression);
    }

    @Override
    public void delete() throws IOException {
        sql.deleteMapMeta(mapId, itemName);
    }

    @Override
    public boolean exists() throws IOException {
        return sql.hasMapMeta(mapId, itemName);
    }

    @Override
    public boolean isClosed() {
        return sql.isClosed();
    }

}
