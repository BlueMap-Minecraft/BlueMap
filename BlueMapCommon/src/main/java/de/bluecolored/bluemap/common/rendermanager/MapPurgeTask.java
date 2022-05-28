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
package de.bluecolored.bluemap.common.rendermanager;

import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.storage.file.FileStorage;
import de.bluecolored.bluemap.core.util.DeletingPathVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class MapPurgeTask implements RenderTask {

    public static MapPurgeTask create(BmMap map) throws IOException {
        Storage storage = map.getStorage();
        if (storage instanceof FileStorage) {
            return new MapFilePurgeTask(map, (FileStorage) storage);
        } else {
            return new MapStoragePurgeTask(map);
        }
    }

    public static MapPurgeTask create(Path mapDirectory) throws IOException {
        return new MapFilePurgeTask(mapDirectory);
    }

    @DebugDump
    private static class MapFilePurgeTask extends MapPurgeTask {

        private final BmMap map;
        private final Path directory;
        private final int subFilesCount;
        private final LinkedList<Path> subFiles;

        private volatile boolean hasMoreWork;
        private volatile boolean cancelled;

        public MapFilePurgeTask(Path mapDirectory) throws IOException {
            this(null, mapDirectory);
        }

        public MapFilePurgeTask(BmMap map, FileStorage fileStorage) throws IOException {
            this(map, fileStorage.getFilePath(map.getId()));
        }

        private MapFilePurgeTask(BmMap map, Path directory) throws IOException {
            this.map = map;
            this.directory = directory;
            try (Stream<Path> pathStream = Files.walk(directory, 3)) {
                this.subFiles = pathStream.collect(Collectors.toCollection(LinkedList::new));
            }
            this.subFilesCount = subFiles.size();
            this.hasMoreWork = true;
            this.cancelled = false;
        }

        @Override
        public void doWork() throws Exception {
            synchronized (this) {
                if (!this.hasMoreWork) return;
                this.hasMoreWork = false;
            }

            try {
                // delete subFiles first to be able to track the progress and cancel
                while (!subFiles.isEmpty()) {
                    Path subFile = subFiles.getLast();
                    Files.walkFileTree(subFile, DeletingPathVisitor.INSTANCE);
                    subFiles.removeLast();
                    if (this.cancelled) return;
                }

                // make sure everything is deleted
                Files.walkFileTree(directory, DeletingPathVisitor.INSTANCE);
            } finally {
                // reset map render state
                if (this.map != null) {
                    this.map.getRenderState().reset();
                }
            }
        }

        @Override
        public boolean hasMoreWork() {
            return this.hasMoreWork;
        }

        @Override
        @DebugDump
        public double estimateProgress() {
            return 1d - (subFiles.size() / (double) subFilesCount);
        }

        @Override
        public void cancel() {
            this.cancelled = true;
        }

        @Override
        public boolean contains(RenderTask task) {
            if (task == this) return true;
            if (task instanceof MapFilePurgeTask) {
                return ((MapFilePurgeTask) task).directory.toAbsolutePath().normalize()
                        .startsWith(this.directory.toAbsolutePath().normalize());
            }

            return false;
        }

        @Override
        public String getDescription() {
            return "Purge Map " + directory.getFileName();
        }

    }

    @DebugDump
    private static class MapStoragePurgeTask extends MapPurgeTask {

        private final BmMap map;

        private volatile boolean hasMoreWork;

        public MapStoragePurgeTask(BmMap map) {
            this.map = Objects.requireNonNull(map);
            this.hasMoreWork = true;
        }

        @Override
        public void doWork() throws Exception {
            synchronized (this) {
                if (!this.hasMoreWork) return;
                this.hasMoreWork = false;
            }

            try {
                map.getStorage().purgeMap(map.getId());
            } finally {
                // reset map render state
                map.getRenderState().reset();
            }
        }

        @Override
        public boolean hasMoreWork() {
            return this.hasMoreWork;
        }

        @Override
        @DebugDump
        public double estimateProgress() {
            return 0d;
        }

        @Override
        public void cancel() {
            this.hasMoreWork = false;
        }

        @Override
        public boolean contains(RenderTask task) {
            if (task == this) return true;
            if (task instanceof MapStoragePurgeTask) {
                return map.equals(((MapStoragePurgeTask) task).map);
            }

            return false;
        }

        @Override
        public String getDescription() {
            return "Purge Map " + map.getId();
        }

    }

}