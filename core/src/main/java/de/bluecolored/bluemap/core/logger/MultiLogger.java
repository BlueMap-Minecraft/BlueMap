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

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MultiLogger extends AbstractLogger {

    private static final AtomicInteger LOGGER_INDEX = new AtomicInteger(0);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, Logger> logger;

    public MultiLogger(Logger... logger) {
        this.logger = new HashMap<>();
        for (Logger l : logger)
            put(l);
    }

    public void put(Logger logger) {
        put("anonymous-logger-" + LOGGER_INDEX.getAndIncrement(), () -> logger);
    }

    public void put(String name, LoggerSupplier loggerSupplier) {
        lock.writeLock().lock();
        try {
            remove(name); //remove first so logger is closed
            this.logger.put(name, loggerSupplier.get());
        } catch (Exception ex) {
            logError("Failed to close Logger!", ex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(String name) {
        lock.writeLock().lock();
        try {
            Logger removed = this.logger.remove(name);
            if (removed != null) removed.close();
        } catch (Exception ex) {
            logError("Failed to close Logger!", ex);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            String[] loggerNames = this.logger.keySet().toArray(String[]::new);
            for (String name : loggerNames)
                remove(name);
            this.logger.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void logError(String message, @Nullable Throwable throwable) {
        lock.readLock().lock();
        try {
            for (Logger l : logger.values())
                l.logError(message, throwable);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void logWarning(String message) {
        lock.readLock().lock();
        try {
            for (Logger l : logger.values())
                l.logWarning(message);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void logInfo(String message) {
        lock.readLock().lock();
        try {
            for (Logger l : logger.values())
                l.logInfo(message);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void logDebug(String message) {
        lock.readLock().lock();
        try {
            for (Logger l : logger.values())
                l.logDebug(message);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void close() throws Exception {
        lock.readLock().lock();
        try {
            for (Logger l : logger.values())
                l.close();
        } finally {
            lock.readLock().unlock();
        }
    }

    @FunctionalInterface
    public interface LoggerSupplier {

        Logger get() throws Exception;

    }

}
