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

import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Supplier;

public class InstancePool<T> {

    private static final class AutoClearTimer {
        private static final Timer INSTANCE = new Timer("BlueMap-InstancePool-RecycleTimer", true);
    }

    private final Supplier<T> creator;
    private final Function<T, T> recycler;
    private final ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<>();
    private final @Nullable Duration autoClearTime;
    private @Nullable TimerTask autoClearTask = null;

    public InstancePool(Supplier<T> creator) {
        this.creator = creator;
        this.recycler = t -> t;
        this.autoClearTime = null;
    }

    public InstancePool(Supplier<T> creator, Function<T, T> recycler) {
        this.creator = creator;
        this.recycler = recycler;
        this.autoClearTime = null;
    }

    public InstancePool(Supplier<T> creator, Function<T, T> recycler, @Nullable Duration autoClearTime) {
        this.creator = creator;
        this.recycler = recycler;
        this.autoClearTime = autoClearTime;
        updateAutoClear();
    }

    private synchronized void updateAutoClear() {
        if (autoClearTask != null) autoClearTask.cancel();
        if (autoClearTime != null) {
            autoClearTask = new TimerTask() {
                @Override
                public void run() {
                    InstancePool.this.clear();
                }
            };
            AutoClearTimer.INSTANCE.schedule(autoClearTask, autoClearTime.toMillis());
        }
    }

    public T claimInstance() {
        T instance = pool.poll();
        if (instance == null) {
            instance = creator.get();
        }
        updateAutoClear();
        return instance;
    }

    public void recycleInstance(T instance) {
        instance = recycler.apply(instance);
        if (instance != null)
            pool.offer(instance);
        updateAutoClear();
    }

    public void clear() {
        pool.clear();
    }

}
