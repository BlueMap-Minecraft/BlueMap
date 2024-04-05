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
import de.bluecolored.bluemap.core.storage.SingleItemStorage;
import de.bluecolored.bluemap.core.util.DeletingPathVisitor;
import de.bluecolored.bluemap.core.util.FileHelper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.function.DoublePredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class FileMapStorage extends PathBasedMapStorage {

    private final Path root;

    public FileMapStorage(Path root, Compression compression) {
        super(
                compression,
                ".prbm",
                ".png"
        );
        this.root = root;
    }

    @Override
    public SingleItemStorage file(Path file, Compression compression) {
        return new FileItemStorage(root.resolve(file), compression);
    }

    @Override
    @SuppressWarnings("resource")
    public Stream<Path> files(Path path) throws IOException {
        return Files.walk(root.resolve(path))
                .filter(Files::isRegularFile);
    }

    @Override
    public void delete(DoublePredicate onProgress) throws IOException {
        if (!Files.exists(root)) return;

        final int subFilesCount;
        final LinkedList<Path> subFiles;

        // collect sub-files to be able to provide progress-updates
        try (Stream<Path> pathStream = Files.walk(root, 3)) {
            subFiles = pathStream.collect(Collectors.toCollection(LinkedList::new));
        }
        subFilesCount = subFiles.size();

        // delete subFiles first to be able to track the progress and cancel
        while (!subFiles.isEmpty()) {
            Path subFile = subFiles.getLast();
            Files.walkFileTree(subFile, DeletingPathVisitor.INSTANCE);
            subFiles.removeLast();

            if (!onProgress.test(1d - (subFiles.size() / (double) subFilesCount)))
                return;
        }

        // make sure everything is deleted
        if (Files.exists(root))
            Files.walkFileTree(root, DeletingPathVisitor.INSTANCE);
    }

    @Override
    public boolean exists() throws IOException {
        return Files.exists(root);
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @RequiredArgsConstructor
    private static class FileItemStorage implements SingleItemStorage {

        private final Path file;
        private final Compression compression;

        @Override
        public OutputStream write() throws IOException {
            return compression.compress(FileHelper.createFilepartOutputStream(file));
        }

        @Override
        public CompressedInputStream read() throws IOException {
            if (!Files.exists(file)) return null;
            try {
                return new CompressedInputStream(Files.newInputStream(file), compression);
            } catch (FileNotFoundException | NoSuchFileException ex) {
                return null;
            }
        }

        @Override
        public void delete() throws IOException {
            Files.delete(file);
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

}
