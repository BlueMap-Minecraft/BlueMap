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

import de.bluecolored.bluemap.common.debug.DebugDump;
import de.bluecolored.bluemap.core.map.BmMap;

import java.util.Objects;

public class MapPurgeTask implements RenderTask {

    private final BmMap map;

    private volatile double progress;
    private volatile boolean hasMoreWork;
    private volatile boolean cancelled;

    public MapPurgeTask(BmMap map) {
        this.map = Objects.requireNonNull(map);
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

        // discard any pending lowres changes
        this.map.getLowresTileManager().discard();

        // purge the map
        map.getStorage().delete(progress -> {
            this.progress = progress;
            return !this.cancelled;
        });

        map.resetTextureGallery();
        map.getMapTileState().reset();
        map.getMapChunkState().reset();
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
        if (task instanceof MapPurgeTask) {
            return map.equals(((MapPurgeTask) task).map);
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "Purge map " + map.getId();
    }

}
