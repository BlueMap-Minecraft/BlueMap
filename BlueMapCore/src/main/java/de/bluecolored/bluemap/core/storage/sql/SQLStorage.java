package de.bluecolored.bluemap.core.storage.sql;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.storage.MapStorage;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.storage.sql.commandset.CommandSet;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
public class SQLStorage implements Storage {

    private final CommandSet sql;
    private final Compression compression;
    private final LoadingCache<String, SQLMapStorage> mapStorages = Caffeine.newBuilder()
            .build(this::create);

    @Override
    public void initialize() throws IOException {
        sql.initializeTables();
    }

    private SQLMapStorage create(String mapId) {
        return new SQLMapStorage(mapId, sql, compression);
    }

    @Override
    public MapStorage map(String mapId) {
        return mapStorages.get(mapId);
    }

    @Override
    public Stream<String> mapIds() {
        return StreamSupport.stream(
                new PageSpliterator<>(page -> {
                    try {
                        return sql.listMapIds(page * 1000, 1000);
                    } catch (IOException ex) { throw new RuntimeException(ex); }
                }),
                false
        );
    }

    @Override
    public boolean isClosed() {
        return sql.isClosed();
    }

    @Override
    public void close() throws IOException {
        sql.close();
    }

}
