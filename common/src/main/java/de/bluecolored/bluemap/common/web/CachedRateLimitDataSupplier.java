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
