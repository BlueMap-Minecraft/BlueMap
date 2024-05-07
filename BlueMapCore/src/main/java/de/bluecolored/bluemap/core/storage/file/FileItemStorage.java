package de.bluecolored.bluemap.core.storage.file;

import de.bluecolored.bluemap.core.storage.ItemStorage;
import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@RequiredArgsConstructor
public class FileItemStorage implements ItemStorage {

    private final Path file;
    private final Compression compression;
    private final boolean atomic;

    @Override
    public OutputStream write() throws IOException {
        if (atomic)
            return compression.compress(FileHelper.createFilepartOutputStream(file));

        Path folder = file.toAbsolutePath().normalize().getParent();
        FileHelper.createDirectories(folder);
        return compression.compress(Files.newOutputStream(file,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE));
    }

    @Override
    public @Nullable CompressedInputStream read() throws IOException {
        if (!Files.exists(file)) return null;
        try {
            return new CompressedInputStream(Files.newInputStream(file), compression);
        } catch (FileNotFoundException | NoSuchFileException ex) {
            return null;
        }
    }

    @Override
    public void delete() throws IOException {
        if (Files.exists(file)) Files.delete(file);
    }

    @Override
    public boolean exists() {
        return Files.exists(file);
    }

    @Override
    public boolean isClosed() {
        return false;
    }

}
