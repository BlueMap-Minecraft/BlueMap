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
