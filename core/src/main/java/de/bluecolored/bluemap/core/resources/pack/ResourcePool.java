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
package de.bluecolored.bluemap.core.resources.pack;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

public class ResourcePool<T> {

    private final Map<Key, T> pool = new HashMap<>();
    private final Map<Key, ResourcePath<T>> paths = new HashMap<>();
    private final ResourcePoolMapper mapper;
    private final Class<T> type;

    public ResourcePool() {
        this.type = null;
        this.mapper = null;
    }

    public ResourcePool(Class<T> type, ResourcePoolMapper mapper) {
        this.type = type;
        this.mapper = mapper;
    }

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

    public Collection<ResourcePath<T>> paths() {
        return paths.values();
    }

    public Collection<T> values() {
        return pool.values();
    }

    public boolean contains(Key path) {
        if (mapper != null) {
            path = mapper.remapResource(type, path);
        }
        return paths.containsKey(path);
    }

    public @Nullable T get(ResourcePath<T> path) {
        return pool.get(path);
    }

    public @Nullable T get(Key path) {
        ResourcePath<T> rp = getPath(path);
        return rp == null ? null : rp.getResource(this::get);
    }

    public synchronized void remove(Key path) {
        ResourcePath<T> removed = paths.remove(path);
        if (removed != null) pool.remove(removed);
    }

    public @Nullable ResourcePath<T> getPath(Key path) {
        if (mapper != null) {
            path = mapper.remapResource(type, path);
        }
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
