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
import de.bluecolored.bluemap.core.util.Key;

import java.io.IOException;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public class ResourcePool<T> {

    private final Map<Key, T> resources = new HashMap<>();

    public T get(Key key) {
        return resources.get(key);
    }

    public void put(Key key, T value) {
        Objects.requireNonNull(key, "key is null");
        resources.put(key, value);
    }

    public void putIfAbsent(Key key, T value) {
        Objects.requireNonNull(key, "key is null");
        resources.putIfAbsent(key, value);
    }

    public boolean containsKey(Key key) {
        return resources.containsKey(key);
    }

    public void remove(Key key) {
        resources.remove(key);
    }

    public Collection<T> values() {
        return resources.values();
    }

    public Set<Map.Entry<Key, T>> entrySet() {
        return resources.entrySet();
    }

    public Set<Key> keySet() {
        return resources.keySet();
    }

    public void load(Key path, Loader<T> loader) {
        try {
            if (this.containsKey(path)) return; // don't load already present resources

            T resource = loader.load(path);
            if (resource == null) return; // don't load missing resources

            put(path, resource);
        } catch (Exception ex) {
            Logger.global.logDebug("Failed to load resource '" + path + "': " + ex);
        }
    }

    public void load(Key path, Loader<T> loader, BinaryOperator<T> mergeFunction) {
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
        T load(Key resourcePath) throws IOException;
    }

}
