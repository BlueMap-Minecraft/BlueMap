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
package de.bluecolored.bluemap.common.plugin;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.common.rendermanager.RenderManager;
import de.bluecolored.bluemap.common.rendermanager.WorldRegionRenderTask;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.util.WatchService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MapUpdateService extends Thread {

    private final BmMap map;
    private final RenderManager renderManager;
    private final WatchService<Vector2i> watchService;

    private volatile boolean closed;

    private Timer delayTimer;

    private final Map<Vector2i, TimerTask> scheduledUpdates;

    public MapUpdateService(RenderManager renderManager, BmMap map) throws IOException {
        this.renderManager = renderManager;
        this.map = map;
        this.closed = false;
        this.scheduledUpdates = new HashMap<>();
        this.watchService = map.getWorld().createRegionWatchService();
    }

    @Override
    public void run() {
        if (delayTimer == null) delayTimer = new Timer("BlueMap-RegionFileWatchService-DelayTimer", true);

        Logger.global.logDebug("Started watching map '" + map.getId() + "' for updates...");

        try {
            while (!closed)
                this.watchService.take().forEach(this::updateRegion);
        } catch (WatchService.ClosedException ignore) {
        } catch (IOException e) {
            Logger.global.logError("Exception trying to watch map '" + map.getId() + "' for updates.", e);
        } catch (InterruptedException iex) {
            Thread.currentThread().interrupt();
        } finally {
            Logger.global.logDebug("Stopped watching map '" + map.getId() + "' for updates.");
            if (!closed) {
                Logger.global.logWarning("Region-file watch-service for map '" + map.getId() +
                        "' stopped unexpectedly! (This map might not update automatically from now on)");
            }
        }
    }

    private synchronized void updateRegion(Vector2i regionPos) {
        if (closed) return;

        // we only want to start the render when there were no changes on a file for 5 seconds
        TimerTask task = scheduledUpdates.remove(regionPos);
        if (task != null) task.cancel();

        task = new TimerTask() {
            @Override
            public void run() {
                synchronized (MapUpdateService.this) {
                    WorldRegionRenderTask task = new WorldRegionRenderTask(map, regionPos);
                    scheduledUpdates.remove(regionPos);
                    renderManager.scheduleRenderTask(task);

                    Logger.global.logDebug("Scheduled update for region-file: " + regionPos + " (Map: " + map.getId() + ")");
                }
            }
        };
        scheduledUpdates.put(regionPos, task);
        delayTimer.schedule(task, 5000);
    }

    public synchronized void close() {
        this.closed = true;
        this.interrupt();

        if (this.delayTimer != null) this.delayTimer.cancel();

        try {
            this.watchService.close();
        } catch (Exception ex) {
            Logger.global.logError("Exception while trying to close WatchService!", ex);
        }
    }

}
