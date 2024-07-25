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
package de.bluecolored.bluemap.core.storage.sql;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class PageSpliterator<T> implements Spliterator<T> {

    private final IntFunction<T[]> pageSupplier;
    private T[] lastBatch;
    private int pos;
    private int page;

    public PageSpliterator(IntFunction<T[]> pageSupplier) {
        this.pageSupplier = pageSupplier;
    }

    @Override
    public synchronized boolean tryAdvance(Consumer<? super T> action) {
        if (!refill()) return false;
        action.accept(lastBatch[pos++]);
        return true;
    }

    @Override
    public synchronized Spliterator<T> trySplit() {
        if (!refill()) return null;
        int from = pos;
        pos = lastBatch.length;
        return Spliterators.spliterator(
                lastBatch, from, pos,
                characteristics()
        );
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return 0;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private synchronized boolean refill() {
        if (lastBatch != null && pos < lastBatch.length) return true;
        pos = 0;
        lastBatch = pageSupplier.apply(page++);
        return lastBatch != null && lastBatch.length > 0;
    }

}
