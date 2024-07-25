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
package de.bluecolored.bluemap.core.world.mca;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.util.FileHelper;
import de.bluecolored.bluemap.core.util.WatchService;
import de.bluecolored.bluemap.core.world.mca.region.RegionType;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MCAWorldRegionWatchService implements WatchService<Vector2i> {

    private final Path regionFolder;
    private final java.nio.file.WatchService watchService;
    private boolean initialized;

    public MCAWorldRegionWatchService(Path regionFolder) throws IOException {
        this.regionFolder = regionFolder;
        this.watchService = regionFolder.getFileSystem().newWatchService();
    }

    @Override
    public @Nullable List<Vector2i> poll() throws IOException {
        if (!ensureInitialization()) return null;
        try {
            WatchKey key = watchService.poll();
            if (key == null) return null;
            return processWatchKey(key);
        } catch (ClosedWatchServiceException e) {
            throw new ClosedException(e);
        }
    }

    @Override
    public @Nullable List<Vector2i> poll(long timeout, TimeUnit unit) throws IOException, InterruptedException {
        long endTime = System.currentTimeMillis() + unit.toMillis(timeout);

        FileHelper.awaitExistence(regionFolder, timeout, unit);
        if (!ensureInitialization()) return null;

        long now = System.currentTimeMillis();
        if (now >= endTime) return null;

        try {
            WatchKey key = watchService.poll(endTime - now, TimeUnit.MILLISECONDS);
            if (key == null) return null;
            return processWatchKey(key);
        } catch (ClosedWatchServiceException e) {
            throw new ClosedException(e);
        }
    }

    @Override
    public List<Vector2i> take() throws IOException, InterruptedException {
        while (!ensureInitialization())
            FileHelper.awaitExistence(regionFolder, 1, TimeUnit.HOURS);

        try {
            WatchKey key = watchService.take();
            return processWatchKey(key);
        } catch (ClosedWatchServiceException e) {
            throw new ClosedException(e);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private synchronized boolean ensureInitialization() throws IOException {
        if (initialized) return true;
        if (!Files.exists(regionFolder)) return false;

        regionFolder.register(this.watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
        );
        initialized = true;
        return true;
    }

    @Override
    public void close() throws IOException {
        watchService.close();
    }

    private List<Vector2i> processWatchKey(WatchKey key) {
        try {
            return key.pollEvents().stream()
                    .map(event -> {
                        if (event.context() instanceof Path path) {
                            return RegionType.regionForFileName(path.getFileName().toString());
                        } else {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } finally {
            key.reset();
        }
    }

}
