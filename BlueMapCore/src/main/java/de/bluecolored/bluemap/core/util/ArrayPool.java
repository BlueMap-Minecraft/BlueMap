package de.bluecolored.bluemap.core.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * A pool of objects that (lazily) maintains a specific size of objects.
 * that threads can take, use and return.
 * It discards excessive objects and creates new ones when needed.
 */
public class ArrayPool<T> {

    private final Object[] objects;
    private final int capacity, maxConcurrency;
    private final Supplier<T> supplier;

    private final AtomicInteger head, tail; // head is excluded, tail included
    private int size;

    public ArrayPool(int capacity, int maxConcurrency, Supplier<T> supplier) {
        this.capacity = capacity;
        this.objects = new Object[capacity + maxConcurrency * 2];
        this.maxConcurrency = maxConcurrency;
        this.supplier = supplier;

        this.head = new AtomicInteger(0);
        this.tail = new AtomicInteger(0);
        this.size = 0;
    }

    @SuppressWarnings("unchecked")
    public T take() {
        while (size < maxConcurrency) add(supplier.get());

        size --;
        return (T) objects[tail.getAndUpdate(this::nextPointer)];
    }


    public void add(T resource) {
        while (size > capacity + maxConcurrency) take();

        objects[head.getAndUpdate(this::nextPointer)] = resource;
        size ++;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return capacity;
    }

    private int nextPointer(int prev) {
        return (prev + 1) % objects.length;
    }

}
