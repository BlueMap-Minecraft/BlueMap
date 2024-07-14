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

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Supplier;

public class InstancePool<T> {

    private final Supplier<T> creator;
    private final Function<T, T> recycler;
    private final ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<>();

    public InstancePool(Supplier<T> creator) {
        this.creator = creator;
        this.recycler = t -> t;
    }

    public InstancePool(Supplier<T> creator, Function<T, T> recycler) {
        this.creator = creator;
        this.recycler = recycler;
    }

    public T claimInstance() {
        T instance = pool.poll();
        if (instance == null) {
            instance = creator.get();
        }
        return instance;
    }

    public void recycleInstance(T instance) {
        instance = recycler.apply(instance);
        if (instance != null)
            pool.offer(instance);
    }

}
