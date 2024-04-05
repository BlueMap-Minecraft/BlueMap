package de.bluecolored.bluemap.core.storage.file;

import de.bluecolored.bluemap.core.storage.compression.CompressedInputStream;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.storage.SingleItemStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@RequiredArgsConstructor
class PathBasedGridStorage implements GridStorage {

    private static final Pattern ITEM_PATH_PATTERN = Pattern.compile("x(-?\\d+)z(-?\\d+)");

    private final PathBasedMapStorage storage;
    private final Path root;
    private final String suffix;
    private final Compression compression;

    @Override
    public OutputStream write(int x, int z) throws IOException {
        return item(x, z).write();
    }

    @Override
    public CompressedInputStream read(int x, int z) throws IOException {
        return item(x, z).read();
    }

    @Override
    public void delete(int x, int z) throws IOException {
        item(x, z).delete();
    }

    @Override
    public boolean exists(int x, int z) throws IOException {
        return item(x, z).exists();
    }

    @Override
    public Stream<Cell> stream() throws IOException {
        return storage.files(root)
                .<Cell>map(itemPath -> {
                    Path path = itemPath;
                    if (!path.startsWith(root)) return null;
                    path = root.relativize(path);

                    String name = path.toString();
                    name = name.replace(root.getFileSystem().getSeparator(), "");
                    if (!name.endsWith(suffix)) return null;
                    name = name.substring(name.length() - suffix.length());

                    Matcher matcher = ITEM_PATH_PATTERN.matcher(name);
                    if (!matcher.matches()) return null;
                    int x = Integer.parseInt(matcher.group(1));
                    int z = Integer.parseInt(matcher.group(2));

                    return new PathCell(x, z, itemPath);
                })
                .filter(Objects::nonNull);
    }

    @Override
    public boolean isClosed() {
        return storage.isClosed();
    }

    public SingleItemStorage item(int x, int z) {
        return storage.file(root.resolve(getGridPath(x, z)), compression);
    }

    public Path getGridPath(int x, int z) {
        StringBuilder sb = new StringBuilder()
                .append('x')
                .append(x)
                .append('z')
                .append(z);

        LinkedList<String> folders = new LinkedList<>();
        StringBuilder folder = new StringBuilder();
        sb.chars().forEach(i -> {
            char c = (char) i;
            folder.append(c);
            if (c >= '0' && c <= '9') {
                folders.add(folder.toString());
                folder.delete(0, folder.length());
            }
        });

        String fileName = folders.removeLast();
        folders.add(fileName + suffix);

        return Path.of(folders.removeFirst(), folders.toArray(String[]::new));
    }

    @RequiredArgsConstructor
    private class PathCell implements Cell {

        @Getter
        private final int x, z;

        private final Path path;
        private SingleItemStorage storage;

        @Override
        public OutputStream write() throws IOException {
            return storage().write();
        }

        @Override
        public CompressedInputStream read() throws IOException {
            return storage().read();
        }

        @Override
        public void delete() throws IOException {
            storage().delete();
        }

        @Override
        public boolean exists() throws IOException {
            return storage().exists();
        }

        @Override
        public boolean isClosed() {
            return PathBasedGridStorage.this.isClosed();
        }

        private SingleItemStorage storage() {
            if (storage == null)
                storage = PathBasedGridStorage.this.storage.file(path, compression);
            return storage;
        }

    }

}
