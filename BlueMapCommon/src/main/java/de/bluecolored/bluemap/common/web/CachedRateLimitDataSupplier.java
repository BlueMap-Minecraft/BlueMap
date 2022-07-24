package de.bluecolored.bluemap.common.web;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class CachedRateLimitDataSupplier implements Supplier<String> {

    private final ReentrantLock lock = new ReentrantLock();

    private final Supplier<String> delegate;
    private final long rateLimitMillis;

    private long updateTime = -1;
    private String data = null;

    public CachedRateLimitDataSupplier(Supplier<String> delegate, long rateLimitMillis) {
        this.delegate = delegate;
        this.rateLimitMillis = rateLimitMillis;
    }

    @Override
    public String get() {
        update();
        return data;
    }

    protected void update() {
        if (lock.tryLock()) {
            try {
                long now = System.currentTimeMillis();
                if (data != null && now < updateTime + this.rateLimitMillis) return;
                this.data = delegate.get();
                this.updateTime = now;
            } finally {
                lock.unlock();
            }
        }
    }

}
