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
package de.bluecolored.bluemap.core.util;

import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
public class Registry<T extends Keyed> {

    private final ConcurrentHashMap<Key, T> entries = new ConcurrentHashMap<>();

    private final Set<Key> keys = Collections.unmodifiableSet(entries.keySet());
    private final Collection<T> values = Collections.unmodifiableCollection(entries.values());

    @SafeVarargs
    public Registry(T... defaultEntries) {
        for (T entry : defaultEntries)
            register(entry);
    }

    /**
     * Registers a new entry, only if there is no entry with the same key registered already.
     * Does nothing otherwise.
     * @param entry The new entry to be added to this registry
     * @return true if the entry has been added, false if there is already an entry with the same key registered
     */
    public boolean register(T entry) {
        Objects.requireNonNull(entry, "registry entry can not be null");
        return entries.putIfAbsent(entry.getKey(), entry) != null;
    }

    /**
     * Gets an entry from this registry for a key.
     * @param key The key to search for
     * @return The entry with the key, or null if there is no entry for this key
     */
    public @Nullable T get(Key key) {
        return entries.get(key);
    }

    /**
     * Returns an unmodifiable set of all keys this registry contains entries for
     */
    public Set<Key> keys() {
        return keys;
    }

    /**
     * Returns an unmodifiable collection of entries in this registry
     */
    public Collection<T> values() {
        return values;
    }

}
