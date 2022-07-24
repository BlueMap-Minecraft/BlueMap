package de.bluecolored.bluemap.common.api;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.RenderManager;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.rendermanager.MapPurgeTask;
import de.bluecolored.bluemap.common.rendermanager.MapUpdateTask;

import java.io.IOException;
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
        return renderManager.scheduleRenderTask(new MapUpdateTask(cmap.getBmMap(), force));
    }

    @Override
    public boolean scheduleMapUpdateTask(BlueMapMap map, Collection<Vector2i> regions, boolean force) {
        BlueMapMapImpl cmap = castMap(map);
        return renderManager.scheduleRenderTask(new MapUpdateTask(cmap.getBmMap(), regions, force));
    }

    @Override
    public boolean scheduleMapPurgeTask(BlueMapMap map) throws IOException {
        BlueMapMapImpl cmap = castMap(map);
        return renderManager.scheduleRenderTask(MapPurgeTask.create(cmap.getBmMap()));
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
            renderManager.start(plugin.getConfigs().getCoreConfig().getRenderThreadCount());
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
