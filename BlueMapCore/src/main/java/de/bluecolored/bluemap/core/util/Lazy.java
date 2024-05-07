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

import java.util.Objects;
import java.util.function.Supplier;

public class Lazy<T> {

    private Supplier<T> loader;

    @DebugDump
    private volatile T value;

    public Lazy(Supplier<T> loader) {
        Objects.requireNonNull(loader);

        this.loader = loader;
        this.value = null;
    }

    public Lazy(T value) {
        Objects.requireNonNull(value);

        this.loader = null;
        this.value = value;
    }

    public T getValue() {
        if (value == null) {
            synchronized (this) {
                if (value == null) {
                    this.value = loader.get();
                    this.loader = null;
                }
            }
        }

        return this.value;
    }

    public boolean isLoaded() {
        return this.value != null;
    }

}
