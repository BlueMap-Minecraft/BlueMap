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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.core.storage.GridStorage;
import de.bluecolored.bluemap.core.storage.ItemStorage;
import de.bluecolored.bluemap.core.storage.MapStorage;
import de.bluecolored.bluemap.core.storage.compression.Compression;
import de.bluecolored.bluemap.core.util.DeletingPathVisitor;
import de.bluecolored.bluemap.core.util.FileHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.function.DoublePredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileMapStorage implements MapStorage {

    private static final String TILES_PATH = "tiles";
    private static final String RENDER_STATE_PATH = "rstate";
    private static final String LIVE_PATH = "live";

    private final Path root;
    private final Compression compression;
    private final boolean atomic;

    private final GridStorage hiresGridStorage;
    private final LoadingCache<Integer, GridStorage> lowresGridStorages;
    private final GridStorage tileStateStorage;
    private final GridStorage chunkStateStorage;

    public FileMapStorage(Path root, Compression compression, boolean atomic) {
        this.root = root;
        this.compression = compression;
        this.atomic = atomic;

        this.hiresGridStorage = new FileGridStorage(
                root.resolve(TILES_PATH).resolve("0"),
                ".prbm" + compression.getFileSuffix(),
                compression,
                atomic
        );

        this.lowresGridStorages = Caffeine.newBuilder().build(lod -> new FileGridStorage(
                root.resolve(TILES_PATH).resolve(String.valueOf(lod)),
                ".png",
                Compression.NONE,
                atomic
        ));

        this.tileStateStorage = new FileGridStorage(
                root.resolve(RENDER_STATE_PATH),
                ".tiles.dat",
                Compression.GZIP,
                atomic
        );

        this.chunkStateStorage = new FileGridStorage(
                root.resolve(RENDER_STATE_PATH).resolve(""),
                ".chunks.dat",
                Compression.GZIP,
                atomic
        );

    }

    @Override
    public GridStorage hiresTiles() {
        return hiresGridStorage;
    }

    @Override
    public GridStorage lowresTiles(int lod) {
        return lowresGridStorages.get(lod);
    }

    @Override
    public GridStorage tileState() {
        return tileStateStorage;
    }

    @Override
    public GridStorage chunkState() {
        return chunkStateStorage;
    }

    public Path getAssetPath(String name) {
        String[] parts = MapStorage.escapeAssetName(name)
                .split("/");

        Path assetPath = root.resolve("assets");
        for (String part : parts)
            assetPath = assetPath.resolve(part);

        return assetPath;
    }

    @Override
    public ItemStorage asset(String name) {
        return new FileItemStorage(getAssetPath(name), Compression.NONE, atomic);
    }

    @Override
    public ItemStorage settings() {
        return new FileItemStorage(root.resolve("settings.json"), Compression.NONE, atomic);
    }

    @Override
    public ItemStorage textures() {
        return new FileItemStorage(root.resolve("textures.json" + compression.getFileSuffix()), compression, atomic);
    }

    @Override
    public ItemStorage markers() {
        return new FileItemStorage(root.resolve(LIVE_PATH).resolve("markers.json"), Compression.NONE, atomic);
    }

    @Override
    public ItemStorage players() {
        return new FileItemStorage(root.resolve(LIVE_PATH).resolve("players.json"), Compression.NONE, atomic);
    }

    @Override
    public void delete(DoublePredicate onProgress) throws IOException {
        if (!Files.exists(root)) return;

        final int subFilesCount;
        final LinkedList<Path> subFiles;

        // collect sub-files to be able to provide progress-updates
        try (Stream<Path> pathStream = FileHelper.walk(root, 3)) {
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

}
