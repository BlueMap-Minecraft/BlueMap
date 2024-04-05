/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
