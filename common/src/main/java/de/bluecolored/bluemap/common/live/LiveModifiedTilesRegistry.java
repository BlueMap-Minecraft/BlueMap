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
package de.bluecolored.bluemap.common.live;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple in-memory registry of recently modified tiles per map.
 *
 * The render pipeline calls {@link #record(String, int, int)} whenever a tile
 * has finished processing. The live web-endpoint can then query tiles that
 * have changed since a given version. Updates are retained for a short,
 * limited window so that late or infrequent clients can still catch up.
 */
public final class LiveModifiedTilesRegistry {

    private static final long RETENTION_MILLIS = 20_000L; // 20 seconds

    // Single, monotonically increasing version-counter shared across maps
    private static final AtomicLong CURRENT_VERSION = new AtomicLong(0L);

    private static final ConcurrentMap<String, MapState> UPDATES = new ConcurrentHashMap<>();

    private LiveModifiedTilesRegistry() {
    }

    public static void record(String mapId, int x, int z) {
        Objects.requireNonNull(mapId, "mapId");

        MapState state = UPDATES.computeIfAbsent(mapId, k -> new MapState());
        long now = System.currentTimeMillis();

        synchronized (state) {
            long version = CURRENT_VERSION.incrementAndGet();
            state.currentVersion = version;
            state.updates.addLast(new VersionedTile(version, now, x, z));
            pruneOld(state, now);
        }
    }

    /**
     * Returns all tiles for the given map that have a version greater than the
     * supplied {@code sinceVersion}. The returned {@link Result} also
     * contains the current version for the map which clients should store and
     * use as the {@code sinceVersion} for the next query.
     *
     * Only updates from approximately the last {@value RETENTION_MILLIS}
     * milliseconds are retained. It is therefore expected that very old
     * updates might be discarded and never reported to late clients.
     */
    public static Result getSince(String mapId, long sinceVersion) {
        MapState state = UPDATES.get(mapId);
        if (state == null) {
            return new Result(0L, Collections.emptyList());
        }

        long now = System.currentTimeMillis();

        synchronized (state) {
            pruneOld(state, now);

            long currentVersion = state.currentVersion;
            if (currentVersion <= sinceVersion || state.updates.isEmpty()) {
                return new Result(currentVersion, Collections.emptyList());
            }

            List<Tile> tiles = new ArrayList<>();
            Set<Tile> seen = new LinkedHashSet<>();
            for (VersionedTile update : state.updates) {
                if (update.version <= sinceVersion)
                    continue;

                Tile tile = new Tile(update.x, update.z);
                if (seen.add(tile)) {
                    tiles.add(tile);
                }
            }

            return new Result(currentVersion, Collections.unmodifiableList(tiles));
        }
    }

    /**
     * Helper to prune entries that are older than the retention window.
     * It is acceptable for outdated entries to remain until the next
     * record/query operation triggers a cleanup.
     */
    private static void pruneOld(MapState state, long now) {
        long cutoff = now - RETENTION_MILLIS;
        while (!state.updates.isEmpty()) {
            VersionedTile oldest = state.updates.peekFirst();
            if (oldest == null || oldest.timestamp >= cutoff)
                break;
            state.updates.removeFirst();
        }
    }

    private static final class MapState {
        private long currentVersion = 0L;
        private final Deque<VersionedTile> updates = new ArrayDeque<>();
    }

    private record VersionedTile(long version, long timestamp, int x, int z) {
    }

    public record Tile(int x, int z) {
    }

    public record Result(long version, List<Tile> tiles) {
    }

}
