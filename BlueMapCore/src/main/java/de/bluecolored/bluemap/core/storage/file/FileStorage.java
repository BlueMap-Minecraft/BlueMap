package de.bluecolored.bluemap.core.storage.file;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.storage.compression.Compression;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class FileStorage implements Storage {

    private final Path root;
    private final LoadingCache<String, FileMapStorage> mapStorages;

    public FileStorage(Path root, Compression compression) {
        this.root = root;

        mapStorages = Caffeine.newBuilder()
                .build(id -> new FileMapStorage(root.resolve(id), compression));
    }

    @Override
    public void initialize() throws IOException {}

    @Override
    public FileMapStorage map(String mapId) {
        return mapStorages.get(mapId);
    }

    @SuppressWarnings("resource")
    @Override
    public Stream<String> mapIds() throws IOException {
        return Files.list(root)
                .filter(Files::isDirectory)
                .map(Path::getFileName)
                .map(Path::toString);
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() throws IOException {}

}
