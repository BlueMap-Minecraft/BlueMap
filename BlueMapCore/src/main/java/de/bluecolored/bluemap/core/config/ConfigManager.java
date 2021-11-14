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
package de.bluecolored.bluemap.core.config;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final String[] CONFIG_FILE_ENDINGS = new String[] {
            ".conf",
            ".json"
    };

    private final Path configRoot;

    public ConfigManager(Path configRoot) {
        this.configRoot = configRoot;
    }

    public ConfigurationNode loadConfig(Path rawPath) throws ConfigurationException {
        Path path = findConfigPath(configRoot.resolve(rawPath));

        if (!Files.exists(path)) {
            throw new ConfigurationException(
                    "BlueMap tried to find this file, but it does not exist:\n" +
                    path);
        }

        if (!Files.isReadable(path)) {
            throw new ConfigurationException(
                    "BlueMap tried to read this file, but can not access it:\n" +
                    path + "\n" +
                    "Check if BlueMap has the permission to read this file.");
        }

        try {
            return getLoader(path).load();
        } catch (ConfigurateException ex) {
            throw new ConfigurationException(
                    "BlueMap failed to parse this file:\n" +
                    path + "\n" +
                    "Check if the file is correctly formatted.\n" +
                    "(for example there might be a } or ] or , missing somewhere)",
                    ex);
        }
    }

    public Path getConfigRoot() {
        return configRoot;
    }

    private Path findConfigPath(Path rawPath) {
        for (String fileEnding : CONFIG_FILE_ENDINGS) {
            if (rawPath.getFileName().endsWith(fileEnding)) return rawPath;
        }

        for (String fileEnding : CONFIG_FILE_ENDINGS) {
            Path path = rawPath.getParent().resolve(rawPath.getFileName() + fileEnding);
            if (Files.exists(path)) return path;
        }

        return rawPath.getParent().resolve(rawPath.getFileName() + CONFIG_FILE_ENDINGS[0]);
    }

    private ConfigurationLoader<? extends ConfigurationNode> getLoader(Path path){
        if (path.getFileName().endsWith(".json")) return GsonConfigurationLoader.builder().path(path).build();
        else return HoconConfigurationLoader.builder().path(path).build();
    }

}
