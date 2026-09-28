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
import com.github.benmanes.caffeine.cache.Cache;
import de.bluecolored.bluemap.common.rendermanager.MapUpdatePreparationTask;
import de.bluecolored.bluemap.common.rendermanager.RenderManager;
import de.bluecolored.bluemap.common.rendermanager.WorldRegionUpdateTask;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.util.Caches;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.util.WatchService;
import de.bluecolored.bluemap.core.world.World;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MapUpdateService extends Thread {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);

    private final BmMap map;
    private final RenderManager renderManager;
    private final Instant lastFullUpdate;
    private final Duration fullUpdateInterval;
    private final Duration regionUpdateCooldown;
    private final Duration regionCheckInterval;
    private final WatchService<Vector2i> watchService;

    private volatile boolean closed;

    private final Map<Vector2i, ScheduledFuture<?>> scheduledUpdates;
    private final Cache<Vector2i, Long> lastUpdateTimes;
    private @Nullable ScheduledFuture<?> fullUpdateTask;

    private final Map<Vector2i, Long> regionFingerprints;
    private @Nullable ScheduledFuture<?> regionCheckTask;

    private final Consumer<String> verboseLog;
    private final Consumer<Instant> onFullUpdate;

    @lombok.Builder
    public MapUpdateService(
            @NonNull RenderManager renderManager,
            @NonNull BmMap map,
            @NonNull Instant lastFullUpdate,
            @NonNull Duration fullUpdateInterval,
            @NonNull Duration regionUpdateCooldown,
            @Nullable Duration regionCheckInterval,
            boolean verbose,
            @Nullable Consumer<Instant> onFullUpdate
    ) throws IOException {
        super("BlueMap-MapUpdateService-" + NEXT_ID.getAndIncrement());
        this.renderManager = renderManager;
        this.map = map;
        this.lastFullUpdate = lastFullUpdate;
        this.fullUpdateInterval = fullUpdateInterval;
        this.regionUpdateCooldown = regionUpdateCooldown;
        this.regionCheckInterval = regionCheckInterval != null ? regionCheckInterval : Duration.ZERO;
        this.closed = false;
        this.scheduledUpdates = new HashMap<>();
        this.lastUpdateTimes = Caches.with()
                .expireAfterWrite(regionUpdateCooldown)
                .build();
        this.regionFingerprints = new HashMap<>();
        this.watchService = map.getWorld().createRegionWatchService();
        this.verboseLog = verbose ? Logger.global::logInfo : Logger.global::logDebug;
        this.onFullUpdate = onFullUpdate != null ? onFullUpdate : _ -> {};
    }

    @Override
    public void run() {
        verboseLog.accept("Started watching map '" + map.getId() + "' for updates...");

        synchronized (this) {
            if (!closed && fullUpdateInterval.isPositive()) {
                Duration delay = Instant.now().until(lastFullUpdate.plus(fullUpdateInterval));
                if (delay.isNegative()) delay = Duration.ZERO;
                fullUpdateTask = BlueMap.SCHEDULER.scheduleAtFixedRate(
                        this::fullUpdate,
                        delay.toMillis(), fullUpdateInterval.toMillis(), TimeUnit.MILLISECONDS
                );
            }

            if (!closed && regionCheckInterval.isPositive()) {
                long interval = regionCheckInterval.toMillis();
                regionCheckTask = BlueMap.SCHEDULER.scheduleWithFixedDelay(
                        this::checkRegions,
                        0, interval, TimeUnit.MILLISECONDS
                );
            }
        }

        try {
            while (!closed)
                this.watchService.take().forEach(this::updateRegion);
        } catch (WatchService.ClosedException ignore) {
        } catch (IOException e) {
            Logger.global.logError("Exception trying to watch map '" + map.getId() + "' for updates.", e);
        } catch (InterruptedException iex) {
            Thread.currentThread().interrupt();
        } finally {
            verboseLog.accept("Stopped watching map '" + map.getId() + "' for updates.");
            if (!closed) {
                Logger.global.logWarning("Region-file watch-service for map '" + map.getId() +
                        "' stopped unexpectedly! (This map might not update automatically from now on)");
            }
        }
    }

    private synchronized void updateRegion(Vector2i regionPos) {
        if (closed) return;

        // we only want to start the render when there were no changes on a file for at least 5 seconds
        ScheduledFuture<?> task = scheduledUpdates.remove(regionPos);
        if (task != null) task.cancel(false);

        Long lastUpdateTime = lastUpdateTimes.getIfPresent(regionPos);
        if (lastUpdateTime == null) lastUpdateTime = 0L;
        long timeSinceLastUpdate = System.currentTimeMillis() - lastUpdateTime;
        long delay = Math.max(regionUpdateCooldown.toMillis() - timeSinceLastUpdate, 5000);

        task = BlueMap.SCHEDULER.schedule(() -> scheduleRegionUpdate(regionPos), delay, TimeUnit.MILLISECONDS);
        scheduledUpdates.put(regionPos, task);
    }

    private synchronized void scheduleRegionUpdate(Vector2i regionPos) {
        if (closed) return;

        try {
            WorldRegionUpdateTask task = new WorldRegionUpdateTask(map, regionPos);
            scheduledUpdates.remove(regionPos);
            renderManager.scheduleRenderTask(task);
            lastUpdateTimes.put(regionPos, System.currentTimeMillis());

            verboseLog.accept("Scheduled update for region-file: " + regionPos + " (Map: " + map.getId() + ")");
        } catch (Exception ex) {
            Logger.global.logError("Exception trying to schedule update for region " + regionPos + " of map '" + map.getId() + "'.", ex);
        }
    }

    private synchronized void fullUpdate() {
        if (closed) return;

        try {
            verboseLog.accept("Start updating map '" + map.getId() + "'...");
            onFullUpdate.accept(Instant.now());
            renderManager.scheduleRenderTaskNext(MapUpdatePreparationTask.updateMap(map, renderManager));
        } catch (Exception ex) {
            Logger.global.logError("Exception trying to start full-update of map '" + map.getId() + "'.", ex);
        }
    }

    private void checkRegions() {
        try {
            World world = map.getWorld();
            Grid regionGrid = world.getRegionGrid();
            Predicate<Vector2i> regionBoundsFilter = map.getMapSettings().getCellRenderBoundariesFilter(regionGrid, true);

            Set<Vector2i> removedRegions = new HashSet<>(regionFingerprints.keySet());
            for (Vector2i regionPos : world.listRegions()) {
                if (closed) return;
                if (!regionBoundsFilter.test(regionPos)) continue;
                removedRegions.remove(regionPos);

                long fingerprint;
                try {
                    fingerprint = world.getRegion(regionPos.getX(), regionPos.getY()).fingerprint();
                } catch (IOException ex) {
                    Logger.global.logDebug("Failed to check region-file " + regionPos + " for changes (Map: " + map.getId() + "): " + ex);
                    continue;
                }

                Long lastFingerprint = regionFingerprints.put(regionPos, fingerprint);
                if (lastFingerprint != null && lastFingerprint != fingerprint)
                    updateRegion(regionPos);
            }

            for (Vector2i regionPos : removedRegions) {
                if (closed) return;
                regionFingerprints.remove(regionPos);
                updateRegion(regionPos);
            }
        } catch (Exception ex) {
            Logger.global.logError("Exception trying to check region-files of map '" + map.getId() + "' for changes.", ex);
        }
    }

    public synchronized void close() {
        this.closed = true;
        this.interrupt();

        if (this.fullUpdateTask != null) this.fullUpdateTask.cancel(false);
        if (this.regionCheckTask != null) this.regionCheckTask.cancel(false);

        this.scheduledUpdates.values().forEach(task -> task.cancel(false));
        this.scheduledUpdates.clear();

        try {
            this.watchService.close();
        } catch (Exception ex) {
            Logger.global.logError("Exception while trying to close WatchService!", ex);
        }
    }

}
