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

public class VoidLogger extends Logger {

    @Override
    public void logError(String message, @Nullable Throwable throwable) {}

    @Override
    public void logWarning(String message) {}

    @Override
    public void logInfo(String message) {}

    @Override
    public void logDebug(String message) {}

    @Override
    public void noFloodError(String key, String message, @Nullable Throwable throwable) {}

    @Override
    public void noFloodWarning(String key, String message) {}

    @Override
    public void noFloodInfo(String key, String message) {}

    @Override
    public void noFloodDebug(String key, String message) {}

    @Override
    public void clearNoFloodLog() {}

    @Override
    public void removeNoFloodKey(String key) {}

}
