package de.bluecolored.bluemap.common.rendermanager;

import de.bluecolored.bluemap.core.map.BmMap;

import java.util.concurrent.atomic.AtomicBoolean;

public class MapSaveTask implements RenderTask {

    private final BmMap map;
    private final AtomicBoolean saved;

    public MapSaveTask(BmMap map) {
        this.map = map;
        this.saved = new AtomicBoolean(false);
    }

    @Override
    public void doWork() {
        if (this.saved.compareAndSet(false, true)) {
            map.save();
        }
    }

    @Override
    public boolean hasMoreWork() {
        return !this.saved.get();
    }

    @Override
    public void cancel() {
        this.saved.set(true);
    }

    @Override
    public String getDescription() {
        return "Save map '" + map.getId() + "'";
    }

    @Override
    public boolean contains(RenderTask task) {
        if (this == task) return true;
        if (task == null) return false;
        if (getClass() != task.getClass()) return false;
        MapSaveTask other = (MapSaveTask) task;
        return map.getId().equals(other.map.getId());
    }

}
