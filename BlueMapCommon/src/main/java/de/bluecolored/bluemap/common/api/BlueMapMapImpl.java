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
package de.bluecolored.bluemap.common.api;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.AssetStorage;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.rendermanager.MapUpdateTask;
import de.bluecolored.bluemap.common.rendermanager.WorldRegionRenderTask;
import de.bluecolored.bluemap.core.map.BmMap;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public class BlueMapMapImpl implements BlueMapMap {

    private final WeakReference<Plugin> plugin;
    private final WeakReference<BmMap> map;
    private final BlueMapWorldImpl world;

    public BlueMapMapImpl(Plugin plugin, BmMap map) throws IOException {
        this.plugin = new WeakReference<>(plugin);
        this.map = new WeakReference<>(map);
        this.world = new BlueMapWorldImpl(plugin, map.getWorld());
    }

    public BlueMapMapImpl(Plugin plugin, BmMap map, BlueMapWorldImpl world) {
        this.plugin = new WeakReference<>(plugin);
        this.map = new WeakReference<>(map);
        this.world = world;
    }

    public BmMap getBmMap() {
        return unpack(map);
    }

    @Override
    public String getId() {
        return unpack(map).getId();
    }

    @Override
    public String getName() {
        return unpack(map).getName();
    }

    @Override
    public BlueMapWorld getWorld() {
        return world;
    }

    @Override
    public AssetStorage getAssetStorage() {
        return new AssetStorageImpl(unpack(map).getStorage(), getId());
    }

    @Override
    public Map<String, MarkerSet> getMarkerSets() {
        return unpack(map).getMarkerSets();
    }

    @Override
    public Vector2i getTileSize() {
        return unpack(map).getHiresModelManager().getTileGrid().getGridSize();
    }

    @Override
    public Vector2i getTileOffset() {
        return unpack(map).getHiresModelManager().getTileGrid().getOffset();
    }

    @Override
    public void setTileFilter(Predicate<Vector2i> filter) {
        unpack(map).setTileFilter(filter);
    }

    @Override
    public Predicate<Vector2i> getTileFilter() {
        return unpack(map).getTileFilter();
    }

    @Override
    public synchronized void setFrozen(boolean frozen) {
        if (isFrozen()) unfreeze();
        else freeze();
    }

    private synchronized void unfreeze() {
        Plugin plugin = unpack(this.plugin);
        BmMap map = unpack(this.map);
        plugin.startWatchingMap(map);
        plugin.getPluginState().getMapState(map).setUpdateEnabled(true);
        plugin.getRenderManager().scheduleRenderTask(new MapUpdateTask(map));
    }

    private synchronized void freeze() {
        Plugin plugin = unpack(this.plugin);
        BmMap map = unpack(this.map);
        plugin.stopWatchingMap(map);
        plugin.getPluginState().getMapState(map).setUpdateEnabled(false);
        plugin.getRenderManager().removeRenderTasksIf(task -> {
            if (task instanceof MapUpdateTask)
                return ((MapUpdateTask) task).getMap().equals(map);

            if (task instanceof WorldRegionRenderTask)
                return ((WorldRegionRenderTask) task).getMap().equals(map);

            return false;
        });
    }

    @Override
    public boolean isFrozen() {
        return !unpack(plugin).getPluginState().getMapState(unpack(map)).isUpdateEnabled();
    }

    private <T> T unpack(WeakReference<T> ref) {
        return Objects.requireNonNull(ref.get(), "Reference lost to delegate object. Most likely BlueMap got reloaded and this instance is no longer valid.");
    }

}
