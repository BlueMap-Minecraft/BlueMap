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

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.common.config.typeserializer.KeyTypeSerializer;
import de.bluecolored.bluemap.common.config.typeserializer.Vector2iTypeSerializer;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.util.Key;
import org.apache.commons.io.IOUtils;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigManager {

    private static final String[] CONFIG_FILE_ENDINGS = new String[] {
            ".conf",
            ".json"
    };

    private final Path configRoot;

    public ConfigManager(Path configRoot) {
        this.configRoot = configRoot;
    }

    public <T> T loadConfig(Path rawPath, Class<T> type) throws ConfigurationException {
        Path path = findConfigPath(rawPath);
        ConfigurationNode configNode = loadConfigFile(path);
        try {
            return Objects.requireNonNull(configNode.get(type));
        } catch (SerializationException | NullPointerException ex) {
            throw new ConfigurationException(
                    "BlueMap failed to parse this file:\n" +
                            path + "\n" +
                            "Check if the file is correctly formatted and all values are correct!",
                    ex);
        }
    }

    public ConfigurationNode loadConfig(Path rawPath) throws ConfigurationException {
        Path path = findConfigPath(rawPath);
        return loadConfigFile(path);
    }

    public ConfigTemplate loadConfigTemplate(String resource) throws IOException {
        InputStream in = BlueMap.class.getResourceAsStream(resource);
        if (in == null) throw new IOException("Resource not found: " + resource);
        String configTemplate = IOUtils.toString(in, StandardCharsets.UTF_8);
        return new ConfigTemplate(configTemplate);
    }

    private ConfigurationNode loadConfigFile(Path path) throws ConfigurationException {
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

    public Path findConfigPath(Path rawPath) {
        if (!rawPath.startsWith(configRoot))
            rawPath = configRoot.resolve(rawPath);

        for (String fileEnding : CONFIG_FILE_ENDINGS) {
            if (rawPath.getFileName().endsWith(fileEnding)) return rawPath;
        }

        for (String fileEnding : CONFIG_FILE_ENDINGS) {
            Path path = rawPath.getParent().resolve(rawPath.getFileName() + fileEnding);
            if (Files.exists(path)) return path;
        }

        return rawPath.getParent().resolve(rawPath.getFileName() + CONFIG_FILE_ENDINGS[0]);
    }

    public boolean isConfigFile(Path path) {
        if (!Files.isRegularFile(path)) return false;

        String fileName = path.getFileName().toString();
        for (String fileEnding : CONFIG_FILE_ENDINGS) {
            if (fileName.endsWith(fileEnding)) return true;
        }

        return false;
    }

    public Path getRaw(Path path) {
        String fileName = path.getFileName().toString();
        String rawName = null;

        for (String fileEnding : CONFIG_FILE_ENDINGS) {
            if (fileName.endsWith(fileEnding)) {
                rawName = fileName.substring(0, fileName.length() - fileEnding.length());
                break;
            }
        }

        if (rawName == null) return path;
        return path.getParent().resolve(rawName);
    }

    private ConfigurationLoader<? extends ConfigurationNode> getLoader(Path path){
        AbstractConfigurationLoader.Builder<?, ?> builder;
        if (path.getFileName().endsWith(".json"))
            builder = GsonConfigurationLoader.builder();
        else
            builder = HoconConfigurationLoader.builder();

        return builder
                .path(path)
                .defaultOptions(o -> o.serializers(b -> {
                    b.register(Vector2i.class, new Vector2iTypeSerializer());
                    b.register(Key.class, new KeyTypeSerializer());
                }))
                .build();
    }

}
