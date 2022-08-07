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
