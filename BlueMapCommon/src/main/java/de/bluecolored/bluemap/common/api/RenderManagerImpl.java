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
import de.bluecolored.bluemap.api.RenderManager;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.rendermanager.MapPurgeTask;
import de.bluecolored.bluemap.common.rendermanager.MapUpdateTask;
import de.bluecolored.bluemap.common.rendermanager.TileUpdateStrategy;

import java.util.Collection;

public class RenderManagerImpl implements RenderManager {

    private final BlueMapAPIImpl api;
    private final Plugin plugin;
    private final de.bluecolored.bluemap.common.rendermanager.RenderManager renderManager;

    public RenderManagerImpl(BlueMapAPIImpl api, Plugin plugin) {
        this.api = api;
        this.plugin = plugin;
        this.renderManager = plugin.getRenderManager();
    }

    @Override
    public boolean scheduleMapUpdateTask(BlueMapMap map, boolean force) {
        BlueMapMapImpl cmap = castMap(map);
        return renderManager.scheduleRenderTask(new MapUpdateTask(cmap.map(), TileUpdateStrategy.fixed(force)));
    }

    @Override
    public boolean scheduleMapUpdateTask(BlueMapMap map, Collection<Vector2i> regions, boolean force) {
        BlueMapMapImpl cmap = castMap(map);
        return renderManager.scheduleRenderTask(new MapUpdateTask(cmap.map(), regions, TileUpdateStrategy.fixed(force)));
    }

    @Override
    public boolean scheduleMapPurgeTask(BlueMapMap map) {
        BlueMapMapImpl cmap = castMap(map);
        return renderManager.scheduleRenderTask(new MapPurgeTask(cmap.map()));
    }

    @Override
    public int renderQueueSize() {
        return renderManager.getScheduledRenderTaskCount();
    }

    @Override
    public int renderThreadCount() {
        return renderManager.getWorkerThreadCount();
    }

    @Override
    public boolean isRunning() {
        return renderManager.isRunning();
    }

    @Override
    public void start() {
        if (!isRunning()){
            renderManager.start(plugin.getBlueMap().getConfig().getCoreConfig().getRenderThreadCount());
        }
        plugin.getPluginState().setRenderThreadsEnabled(true);
    }

    @Override
    public void start(int threadCount) {
        if (!isRunning()){
            renderManager.start(threadCount);
        }
        plugin.getPluginState().setRenderThreadsEnabled(true);
    }

    @Override
    public void stop() {
        renderManager.stop();
        plugin.getPluginState().setRenderThreadsEnabled(false);
    }

    private BlueMapMapImpl castMap(BlueMapMap map) {
        BlueMapMapImpl cmap;
        if (map instanceof BlueMapMapImpl) {
            cmap = (BlueMapMapImpl) map;
        } else {
            cmap = (BlueMapMapImpl) api.getMap(map.getId())
                    .orElseThrow(() -> new IllegalStateException("Failed to get BlueMapMapImpl for map " + map.getId()));
        }
        return cmap;
    }

}
