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

@DebugDump
public class Key {

    private static final String MINECRAFT_NAMESPACE = "minecraft";

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

        this.namespace = namespace.intern();
        this.value = value.intern();
        this.formatted = (this.namespace + ":" + this.value).intern();
    }

    public Key(String namespace, String value) {
        this.namespace = namespace.intern();
        this.value = value.intern();
        this.formatted = (this.namespace + ":" + this.value).intern();
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

    @SuppressWarnings("StringEquality")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Key that = (Key) o;
        return getFormatted() == that.getFormatted();
    }

    @Override
    public int hashCode() {
        return getFormatted().hashCode();
    }

    @Override
    public String toString() {
        return formatted;
    }
}
