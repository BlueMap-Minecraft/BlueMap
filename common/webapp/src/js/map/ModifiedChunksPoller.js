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
import {RevalidatingFileLoader} from "../util/RevalidatingFileLoader";
import {alert, hashTile} from "../util/Utils";

/**
 * Periodically polls a live JSON endpoint that lists recently modified
 * tiles/chunks for the current map and triggers selective tile reloads.
 *
 * Expected JSON format (example):
 * {
 *   "tiles": [
 *     { "lod": 0, "x": 10, "z": -5 },   // hires tile (lod 0)
 *     { "lod": 1, "x": 3, "z": 7 }     // lowres tile, lod 1
 *   ]
 * }
 */
export class ModifiedChunksPoller {

    /**
     * @param mapViewer {import("../MapViewer").MapViewer}
     * @param events {EventTarget}
     * @param intervalMs {number}
     */
    constructor(mapViewer, events, intervalMs = 10000) {
        this.mapViewer = mapViewer;
        this.events = events;
        this.intervalMs = intervalMs;

        this._timer = null;
        this._version = 0;
        this._loader = new RevalidatingFileLoader();
        this._loader.setRevalidatedUrls(new Set()); // always no-cache for this JSON
        this._loader.setResponseType("json");
    }

    start() {
        this.stop();

        // Ensure that tile and texture loaders are in revalidation mode so
        // that reloaded tiles bypass any long-lived HTTP caches.
        if (!this.mapViewer.revalidatedUrls) {
            this.mapViewer.clearTileCache();
        }

        this._timer = setInterval(() => this._pollOnce(), this.intervalMs);
    }

    stop() {
        if (this._timer) {
            clearInterval(this._timer);
            this._timer = null;
        }
    }

    _pollOnce() {
        const map = this.mapViewer.map;
        if (!map || !map.data || !map.data.liveDataRoot) return;

        const url = map.data.liveDataRoot + "/live/modified-chunks.json?since=" + encodeURIComponent(this._version);

        this._loader.load(
            url,
            data => {
                if (!data) return;
                try {
                    this._applyUpdates(data);
                } catch (e) {
                    alert(this.events, e, "warning");
                }
            },
            () => {},
            () => {
                // If the endpoint does not exist or errors, silently ignore.
            }
        );
    }

    _applyUpdates(data) {
        if (data && typeof data.version === "number") {
            this._version = data.version;
        }

        const tiles = Array.isArray(data.tiles) ? data.tiles : [];
        if (tiles.length === 0) return;

        const map = this.mapViewer.map;
        if (!map) return;

        const revalidatedUrls = this.mapViewer.revalidatedUrls;

        for (const entry of tiles) {
            if (!entry) continue;

            const lod = typeof entry.lod === "number" ? entry.lod : 0;
            const x = typeof entry.x === "number" ? entry.x : NaN;
            const z = typeof entry.z === "number" ? entry.z : NaN;
            if (!Number.isFinite(x) || !Number.isFinite(z)) continue;

            const tileManager = lod === 0
                ? map.hiresTileManager
                : (map.lowresTileManager && map.lowresTileManager[lod - 1]);

            if (!tileManager) continue;

            const tileHash = hashTile(x, z);
            const tile = tileManager.tiles.get(tileHash);

            // If the tile is not currently loaded, we still want future loads
            // to revalidate against the server, so evict this URL from the
            // revalidated set if we can compute it.
            if (tile && tile.model && tile.model.userData && tile.model.userData.tileUrl) {
                const tileUrl = tile.model.userData.tileUrl;
                if (revalidatedUrls && typeof revalidatedUrls.delete === "function") {
                    revalidatedUrls.delete(tileUrl);
                }

                tileManager.reloadTile(x, z);
            }
        }
    }
}
