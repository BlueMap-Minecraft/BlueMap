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

import de.bluecolored.bluemap.api.debug.DebugDump;

import java.util.concurrent.ConcurrentHashMap;

@DebugDump
public class Key implements Keyed {

    private static final ConcurrentHashMap<String, String> STRING_INTERN_POOL = new ConcurrentHashMap<>();

    public static final String MINECRAFT_NAMESPACE = "minecraft";
    public static final String BLUEMAP_NAMESPACE = "bluemap";

    private final String namespace;
    private final String value;
    private final String formatted;

    public Key(String formatted) {
        String namespace = MINECRAFT_NAMESPACE;
        String value = formatted;
        int namespaceSeparator = formatted.indexOf(':');
        if (namespaceSeparator > 0) {
            namespace = formatted.substring(0, namespaceSeparator);
            value = formatted.substring(namespaceSeparator + 1);
        }

        this.namespace = intern(namespace);
        this.value = intern(value);
        this.formatted = intern(this.namespace + ":" + this.value);
    }

    public Key(String namespace, String value) {
        this.namespace = intern(namespace);
        this.value = intern(value);
        this.formatted = intern(this.namespace + ":" + this.value);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getValue() {
        return value;
    }

    public String getFormatted() {
        return formatted;
    }

    @Override
    public Key getKey() {
        return this;
    }

    @SuppressWarnings("StringEquality")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Key that)) return false;
        if (!that.canEqual(this)) return false;
        return formatted == that.formatted;
    }

    protected boolean canEqual(Object o) {
        return o instanceof Key;
    }

    @Override
    public int hashCode() {
        return formatted.hashCode();
    }

    @Override
    public String toString() {
        return formatted;
    }

    public static Key parse(String formatted) {
        return new Key(formatted);
    }

    public static Key parse(String formatted, String defaultNamespace) {
        String namespace = defaultNamespace;
        String value = formatted;
        int namespaceSeparator = formatted.indexOf(':');
        if (namespaceSeparator > 0) {
            namespace = formatted.substring(0, namespaceSeparator);
            value = formatted.substring(namespaceSeparator + 1);
        }

        return new Key(namespace, value);
    }

    public static Key minecraft(String value) {
        return new Key(MINECRAFT_NAMESPACE, value);
    }

    public static Key bluemap(String value) {
        return new Key(BLUEMAP_NAMESPACE, value);
    }

    /**
     * Using our own function instead of {@link String#intern()} since the ConcurrentHashMap is much faster.
     */
    protected static String intern(String string) {
        String interned = STRING_INTERN_POOL.putIfAbsent(string, string);
        return interned != null ? interned : string;
    }

}
