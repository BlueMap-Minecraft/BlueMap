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

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.storage.MapStorage;

import java.util.Objects;

public class StorageDeleteTask implements RenderTask {

    private final MapStorage storage;
    private final String mapId;

    private volatile double progress;
    private volatile boolean hasMoreWork;
    private volatile boolean cancelled;

    public StorageDeleteTask(MapStorage storage, String mapId) {
        this.storage = Objects.requireNonNull(storage);
        this.mapId = Objects.requireNonNull(mapId);
        this.progress = 0d;
        this.hasMoreWork = true;
        this.cancelled = false;
    }

    @Override
    public void doWork() throws Exception {
        synchronized (this) {
            if (!this.hasMoreWork) return;
            this.hasMoreWork = false;
        }
        if (this.cancelled) return;

        // purge the map
        storage.delete(progress -> {
            this.progress = progress;
            return !this.cancelled;
        });
    }

    @Override
    public boolean hasMoreWork() {
        return this.hasMoreWork && !this.cancelled;
    }

    @Override
    @DebugDump
    public double estimateProgress() {
        return this.progress;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean contains(RenderTask task) {
        if (task == this) return true;
        if (task instanceof StorageDeleteTask) {
            StorageDeleteTask sTask = (StorageDeleteTask) task;
            return storage.equals(sTask.storage) && mapId.equals(sTask.mapId);
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "Delete map " + mapId;
    }

}
