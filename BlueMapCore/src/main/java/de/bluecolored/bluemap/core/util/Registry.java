package de.bluecolored.bluemap.core.util;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Registry<T extends Keyed> {

    private final ConcurrentHashMap<Key, T> entries;

    public Registry() {
        this.entries = new ConcurrentHashMap<>();
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
        return Collections.unmodifiableSet(entries.keySet());
    }

    /**
     * Returns an unmodifiable collection of entries in this registry
     */
    public Collection<T> values() {
        return Collections.unmodifiableCollection(entries.values());
    }

}
