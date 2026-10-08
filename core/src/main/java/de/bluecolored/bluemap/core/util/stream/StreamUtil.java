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
package de.bluecolored.bluemap.core.util.stream;

import java.io.Closeable;
import java.io.IOException;

public final class StreamUtil {

    private StreamUtil() {}

    /**
     * Applies the action to the resource and returns its result.<br>
     * If the action fails, the resource gets closed (a failure while closing is added as suppressed exception)
     * and the original exception is rethrown.<br>
     * Use this to hand over ownership of a resource (e.g. wrapping a stream) without leaking it if the hand-over fails.
     */
    public static <T extends Closeable, R> R closeOnError(T resource, IOFunction<? super T, R> action) throws IOException {
        return onError(resource, action, Closeable::close);
    }

    /**
     * Same as {@link #closeOnError(Closeable, IOFunction)}, but performs the given cleanup instead of
     * {@link Closeable#close()} if the action fails.
     */
    public static <T, R> R onError(T resource, IOFunction<? super T, R> action, IOConsumer<? super T> onError) throws IOException {
        try {
            return action.apply(resource);
        } catch (IOException | RuntimeException | Error ex) {
            try {
                onError.accept(resource);
            } catch (IOException | RuntimeException cleanupEx) {
                ex.addSuppressed(cleanupEx);
            }
            throw ex;
        }
    }

    @FunctionalInterface
    public interface IOFunction<T, R> {
        R apply(T t) throws IOException;
    }

    @FunctionalInterface
    public interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }

}
