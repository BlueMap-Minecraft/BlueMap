package de.bluecolored.bluemap.core.resources.pack;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

public class ResourcePool<T> {

    private final Map<ResourcePath<T>, T> pool = new HashMap<>();
    private final Map<Key, ResourcePath<T>> paths = new HashMap<>();

    public void put(Key path, T value) {
        put(new ResourcePath<>(path), value);
    }

    public synchronized void put(ResourcePath<T> path, T value) {
        pool.put(path, value);
        paths.put(path, path);
        path.setResource(value);
    }

    public void putIfAbsent(Key path, T value) {
        putIfAbsent(new ResourcePath<>(path), value);
    }

    public synchronized void putIfAbsent(ResourcePath<T> path, T value) {
        if (pool.putIfAbsent(path, value) == null) {
            paths.put(path, path);
            path.setResource(value);
        }
    }

    public Set<ResourcePath<T>> paths() {
        return pool.keySet();
    }

    public Collection<T> values() {
        return pool.values();
    }

    public boolean contains(Key path) {
        return paths.containsKey(path);
    }

    public @Nullable T get(ResourcePath<T> path) {
        return pool.get(path);
    }

    public @Nullable T get(Key path) {
        ResourcePath<T> rp = paths.get(path);
        return rp == null ? null : rp.getResource(this::get);
    }

    public synchronized void remove(Key path) {
        ResourcePath<T> removed = paths.remove(path);
        if (removed != null) pool.remove(removed);
    }

    public @Nullable ResourcePath<T> getPath(Key path) {
        return paths.get(path);
    }

    public void load(ResourcePath<T> path, Loader<T> loader) {
        try {
            if (contains(path)) return; // don't load already present resources

            T resource = loader.load(path);
            if (resource == null) return; // don't load missing resources

            put(path, resource);
        } catch (Exception ex) {
            Logger.global.logDebug("Failed to load resource '" + path + "': " + ex);
        }
    }

    public void load(ResourcePath<T> path, Loader<T> loader, BinaryOperator<T> mergeFunction) {
        try {
            T resource = loader.load(path);
            if (resource == null) return; // don't load missing resources

            T previous = get(path);
            if (previous != null)
                resource = mergeFunction.apply(previous, resource);

            put(path, resource);
        } catch (Exception ex) {
            Logger.global.logDebug("Failed to parse resource '" + path + "': " + ex);
        }
    }

    public interface Loader<T> {
        T load(ResourcePath<T> resourcePath) throws IOException;
    }

}
