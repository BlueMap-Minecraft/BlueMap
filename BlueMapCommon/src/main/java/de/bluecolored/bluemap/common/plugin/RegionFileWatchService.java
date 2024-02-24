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
import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.common.rendermanager.RenderManager;
import de.bluecolored.bluemap.common.rendermanager.WorldRegionRenderTask;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.util.FileHelper;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.mca.MCAWorld;
import de.bluecolored.bluemap.core.world.mca.region.RegionType;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RegionFileWatchService extends Thread {

    private final BmMap map;
    private final RenderManager renderManager;
    private final WatchService watchService;

    private volatile boolean closed;

    private Timer delayTimer;

    @DebugDump
    private final Map<Vector2i, TimerTask> scheduledUpdates;

    public RegionFileWatchService(RenderManager renderManager, BmMap map) throws IOException {
        this.renderManager = renderManager;
        this.map = map;
        this.closed = false;
        this.scheduledUpdates = new HashMap<>();

        World world = map.getWorld();
        if (!(world instanceof MCAWorld)) throw new UnsupportedOperationException("world-type is not supported");
        Path folder = ((MCAWorld) world).getRegionFolder();
        FileHelper.createDirectories(folder);

        this.watchService = folder.getFileSystem().newWatchService();
        folder.register(this.watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

        Logger.global.logDebug("Created region-file watch-service for map '" + map.getId() + "' at '" + folder + "'.");
    }

    @Override
    public void run() {
        if (delayTimer == null) delayTimer = new Timer("BlueMap-RegionFileWatchService-DelayTimer", true);

        Logger.global.logDebug("Started watching map '" + map.getId() + "' for updates...");

        try {
            while (!closed) {
                WatchKey key = this.watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                    Object fileObject = event.context();
                    if (!(fileObject instanceof Path)) continue;
                    Path file = (Path) fileObject;

                    String regionFileName = file.toFile().getName();
                    updateRegion(regionFileName);
                }

                if (!key.reset()) return;
            }
        } catch (ClosedWatchServiceException ignore) {
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

    private synchronized void updateRegion(String regionFileName) {
        if (RegionType.forFileName(regionFileName) == null) return;

        try {
            String[] filenameParts = regionFileName.split("\\.");
            if (filenameParts.length < 3) return;

            int rX = Integer.parseInt(filenameParts[1]);
            int rZ = Integer.parseInt(filenameParts[2]);
            Vector2i regionPos = new Vector2i(rX, rZ);

            // we only want to start the render when there were no changes on a file for 5 seconds
            TimerTask task = scheduledUpdates.remove(regionPos);
            if (task != null) task.cancel();

            task = new TimerTask() {
                @Override
                public void run() {
                    synchronized (RegionFileWatchService.this) {
                        WorldRegionRenderTask task = new WorldRegionRenderTask(map, regionPos);
                        scheduledUpdates.remove(regionPos);
                        renderManager.scheduleRenderTask(task);

                        Logger.global.logDebug("Scheduled update for region-file: " + regionPos + " (Map: " + map.getId() + ")");
                    }
                }
            };
            scheduledUpdates.put(regionPos, task);
            delayTimer.schedule(task, 5000);
        } catch (NumberFormatException ignore) {}
    }

    public void close() {
        this.closed = true;
        this.interrupt();

        if (this.delayTimer != null) this.delayTimer.cancel();

        try {
            this.watchService.close();
        } catch (IOException ex) {
            Logger.global.logError("Exception while trying to close WatchService!", ex);
        }
    }

}
