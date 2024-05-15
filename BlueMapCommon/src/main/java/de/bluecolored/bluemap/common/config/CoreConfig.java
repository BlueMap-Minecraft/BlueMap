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
package de.bluecolored.bluemap.common.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.nio.file.Path;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@ConfigSerializable
public class CoreConfig {

    private boolean acceptDownload = false;

    private int renderThreadCount = 1;

    private boolean metrics = true;

    private Path data = Path.of("bluemap");

    private boolean scanForModResources = true;

    private LogConfig log = new LogConfig();

    public boolean isAcceptDownload() {
        return acceptDownload;
    }

    public int getRenderThreadCount() {
        return renderThreadCount;
    }

    public int resolveRenderThreadCount() {
        if (renderThreadCount > 0) return renderThreadCount;
        return Math.max(Runtime.getRuntime().availableProcessors() + renderThreadCount, 1);
    }

    public boolean isMetrics() {
        return metrics;
    }

    public Path getData() {
        return data;
    }

    public boolean isScanForModResources() {
        return scanForModResources;
    }

    public LogConfig getLog() {
        return log;
    }

    @ConfigSerializable
    public static class LogConfig {

        private String file = null;
        private boolean append = false;

        public String getFile() {
            return file;
        }

        public boolean isAppend() {
            return append;
        }

    }

}
