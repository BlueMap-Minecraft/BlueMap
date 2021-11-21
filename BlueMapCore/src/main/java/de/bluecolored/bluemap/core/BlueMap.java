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
package de.bluecolored.bluemap.core;

import com.github.benmanes.caffeine.cache.RemovalCause;
import de.bluecolored.bluemap.core.logger.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;

public class BlueMap {

    // early-loading this class, to fix a classloading issue
    private static final RemovalCause RC = null;

    public static final String VERSION, GIT_HASH, GIT_CLEAN;
    static {
        String version = "DEV", gitHash = "DEV", gitClean = "DEV";
        try {
            ConfigurationNode node = GsonConfigurationLoader.builder()
                    .url(BlueMap.class.getResource("/de/bluecolored/bluemap/version.json"))
                    .build()
                    .load();

            version = node.node("version").getString("DEV");
            gitHash = node.node("git-hash").getString("DEV");
            gitClean = node.node("git-clean").getString("DEV");
        } catch (IOException ex) {
            Logger.global.logError("Failed to load version.json from resources!", ex);
        }

        if (version.equals("${version}")) version = "DEV";
        if (gitHash.equals("${gitHash}")) version = "DEV";
        if (gitClean.equals("${gitClean}")) version = "DEV";

        VERSION = version;
        GIT_HASH = gitHash;
        GIT_CLEAN = gitClean;
    }

    public static final ForkJoinPool THREAD_POOL = new ForkJoinPool();

}
