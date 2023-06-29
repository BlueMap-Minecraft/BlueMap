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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.stream.StreamSupport;

public abstract class Logger {

    public static Logger global = stdOut();

    public void logError(Throwable throwable) {
        logError(throwable.getMessage(), throwable);
    }

    public abstract void logError(String message, Throwable throwable);

    public abstract void logWarning(String message);

    public abstract void logInfo(String message);

    public abstract void logDebug(String message);

    /**
     * Only log the error if no message has been logged before with the same key.
     */
    public abstract void noFloodError(String key, String message, Throwable throwable);

    /**
     * Only log the warning if no message has been logged before with the same key.
     */
    public abstract void noFloodWarning(String key, String message);

    /**
     * Only log the info if no message has been logged before with the same key.
     */
    public abstract void noFloodInfo(String key, String message);

    /**
     * Only log the debug-message if no message has been logged before with the same key.
     */
    public abstract void noFloodDebug(String key, String message);

    /**
     * Only log the error if no message has been logged before with the same content.
     */
    public void noFloodError(Throwable throwable){
        noFloodError(throwable.getMessage(), throwable);
    }

    /**
     * Only log the error if no message has been logged before with the same content.
     */
    public void noFloodError(String message, Throwable throwable){
        noFloodError(message, message, throwable);
    }

    /**
     * Only log the warning if no message has been logged before with the same content.
     */
    public void noFloodWarning(String message){
        noFloodWarning(message, message);
    }

    /**
     * Only log the info if no message has been logged before with the same content.
     */
    public void noFloodInfo(String message){
        noFloodInfo(message, message);
    }

    /**
     * Only log the debug-message if no message has been logged before with the same content.
     */
    public void noFloodDebug(String message){
        noFloodDebug(message, message);
    }

    public abstract void clearNoFloodLog();

    public abstract void removeNoFloodKey(String key);

    public void removeNoFloodMessage(String message){
        removeNoFloodKey(message);
    }

    public static Logger stdOut(){
        return new PrintStreamLogger(System.out, System.err);
    }


    public static Logger file(Path path) throws IOException {
        return file(path, null);
    }

    public static Logger file(Path path, boolean append) throws IOException {
        return file(path, null, append);
    }

    public static Logger file(Path path, String format) throws IOException {
        return file(path, format, true);
    }

    public static Logger file(Path path, @Nullable String format, boolean append) throws IOException {
        Files.createDirectories(path.getParent());

        FileHandler fileHandler = new FileHandler(path.toString(), append);
        fileHandler.setFormatter(format == null ? new LogFormatter() : new LogFormatter(format));

        java.util.logging.Logger javaLogger = java.util.logging.Logger.getAnonymousLogger();
        javaLogger.setUseParentHandlers(false);
        javaLogger.addHandler(fileHandler);

        return new JavaLogger(javaLogger);
    }

    public static Logger combine(Iterable<Logger> logger) {
        return combine(StreamSupport.stream(logger.spliterator(), false)
                .toArray(Logger[]::new));
    }

    public static Logger combine(Logger... logger) {
        if (logger.length == 0) return new VoidLogger();
        if (logger.length == 1) return logger[0];
        return new MultiLogger(logger);
    }

}
