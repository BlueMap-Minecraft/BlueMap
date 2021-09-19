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
package de.bluecolored.bluemap.core.logger;

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import de.bluecolored.bluemap.core.BlueMap;

public abstract class AbstractLogger extends Logger {

    private static final Object DUMMY = new Object();

    private Cache<String, Object> noFloodCache;

    public AbstractLogger() {
        noFloodCache = Caffeine.newBuilder()
            .executor(BlueMap.THREAD_POOL)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();
    }

    @Override
    public void noFloodError(String key, String message, Throwable throwable){
        if (check(key)) logError(message, throwable);
    }

    @Override
    public void noFloodWarning(String key, String message){
        if (check(key)) logWarning(message);
    }

    @Override
    public void noFloodInfo(String key, String message){
        if (check(key)) logInfo(message);
    }

    @Override
    public void noFloodDebug(String key, String message){
        if (check(key)) logDebug(message);
    }

    @Override
    public void clearNoFloodLog() {
        noFloodCache.invalidateAll();
        noFloodCache.cleanUp();
    }

    @Override
    public void removeNoFloodKey(String key) {
        noFloodCache.invalidate(key);
    }

    private boolean check(String key) {
        if (noFloodCache.getIfPresent(key) == null) {
            noFloodCache.put(key, DUMMY);
            return true;
        }

        return false;
    }

}
