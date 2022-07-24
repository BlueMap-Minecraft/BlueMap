package de.bluecolored.bluemap.common.api;

import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.core.world.World;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public class BlueMapWorldImpl implements BlueMapWorld {

    private final WeakReference<Plugin> plugin;
    private final String id;
    private final WeakReference<World> world;

    public BlueMapWorldImpl(Plugin plugin, World world) throws IOException {
        this.plugin = new WeakReference<>(plugin);
        this.id = plugin.getBlueMap().getWorldId(world.getSaveFolder());
        this.world = new WeakReference<>(world);
    }

    public World getWorld() {
        return unpack(world);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Path getSaveFolder() {
        return unpack(world).getSaveFolder();
    }

    @Override
    public Collection<BlueMapMap> getMaps() {
        return unpack(plugin).getMaps().values().stream()
                .filter(map -> map.getWorld().equals(unpack(world)))
                .map(map -> new BlueMapMapImpl(unpack(plugin), map, this))
                .collect(Collectors.toUnmodifiableSet());
    }

    private <T> T unpack(WeakReference<T> ref) {
        return Objects.requireNonNull(ref.get(), "Reference lost to delegate object. Most likely BlueMap got reloaded and this instance is no longer valid.");
    }

}
