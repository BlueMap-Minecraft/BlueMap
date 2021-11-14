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
package de.bluecolored.bluemap.core.config.old;

import de.bluecolored.bluemap.core.config.ConfigurationException;
import de.bluecolored.bluemap.core.debug.DebugDump;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.io.IOException;

@DebugDump
public class CoreConfig {

    private boolean downloadAccepted = false;
    private int renderThreadCount = 0;
    private boolean metricsEnabled = false;
    private File dataFolder = new File("data");

    public CoreConfig(ConfigurationNode node) throws ConfigurationException {

        //accept-download
        downloadAccepted = node.node("accept-download").getBoolean(false);

        //renderThreadCount
        int processors = Runtime.getRuntime().availableProcessors();
        renderThreadCount = node.node("renderThreadCount").getInt(0);
        if (renderThreadCount <= 0) renderThreadCount = processors + renderThreadCount;
        if (renderThreadCount <= 0) renderThreadCount = 1;

        //metrics
        metricsEnabled = node.node("metrics").getBoolean(false);

        //data
        dataFolder = ConfigManager.toFolder(node.node("data").getString("data"));

    }

    public File getDataFolder() {
        return dataFolder;
    }

    public boolean isDownloadAccepted() {
        return downloadAccepted;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public int getRenderThreadCount() {
        return renderThreadCount;
    }

}
