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
import de.bluecolored.bluemap.common.rendermanager.MapUpdateTask;
import de.bluecolored.bluemap.common.rendermanager.WorldRegionRenderTask;
import de.bluecolored.bluemap.core.map.BmMap;

import java.util.function.Predicate;

public class BlueMapMapImpl implements BlueMapMap {

    private BlueMapAPIImpl api;
    private BmMap delegate;

    protected BlueMapMapImpl(BlueMapAPIImpl api, BmMap delegate) {
        this.api = api;
        this.delegate = delegate;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public BlueMapWorldImpl getWorld() {
        return api.getWorldForUuid(delegate.getWorld().getUUID());
    }

    @Override
    public Vector2i getTileSize() {
        return delegate.getHiresModelManager().getTileGrid().getGridSize();
    }

    @Override
    public Vector2i getTileOffset() {
        return delegate.getHiresModelManager().getTileGrid().getOffset();
    }

    @Override
    public void setTileFilter(Predicate<Vector2i> filter) {
        delegate.setTileFilter(filter);
    }

    @Override
    public Predicate<Vector2i> getTileFilter() {
        return delegate.getTileFilter();
    }

    @Override
    public boolean isFrozen() {
        return !api.plugin.getPluginState().getMapState(delegate).isUpdateEnabled();
    }

    @Override
    public synchronized void setFrozen(boolean frozen) {
        if (isFrozen()) unfreeze();
        else freeze();
    }

    private synchronized void unfreeze() {
        api.plugin.startWatchingMap(delegate);
        api.plugin.getPluginState().getMapState(delegate).setUpdateEnabled(true);
        api.plugin.getRenderManager().scheduleRenderTask(new MapUpdateTask(delegate));
    }

    private synchronized void freeze() {
        api.plugin.stopWatchingMap(delegate);
        api.plugin.getPluginState().getMapState(delegate).setUpdateEnabled(false);
        api.plugin.getRenderManager().removeRenderTasksIf(task -> {
            if (task instanceof MapUpdateTask)
                return ((MapUpdateTask) task).getMap().equals(delegate);

            if (task instanceof WorldRegionRenderTask)
                return ((WorldRegionRenderTask) task).getMap().equals(delegate);

            return false;
        });
    }

    public BmMap getMapType() {
        return delegate;
    }

}
