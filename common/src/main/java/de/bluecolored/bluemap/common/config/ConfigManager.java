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
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ConfigManager {

    private static final String CONFIG_TEMPLATE_RESOURCE_PATH = "/de/bluecolored/bluemap/config/";

    private final Path configRoot;

    public ConfigManager(Path configRoot) {
        this.configRoot = configRoot;
    }

    public <T> T loadConfig(String name, Class<T> type) throws ConfigurationException {
        Path file = resolveConfigFile(name);
        return loadConfig(file, type);
    }

    public <T> T loadConfig(Path file, Class<T> type) throws ConfigurationException {
        ConfigurationNode configNode = loadConfigFile(file);
        try {
            return Objects.requireNonNull(configNode.get(type));
        } catch (SerializationException | NullPointerException ex) {
            throw new ConfigurationException(
                    "BlueMap failed to parse this file:\n" +
                            file + "\n" +
                            "Check if the file is correctly formatted and all values are correct!",
                    ex);
        }
    }

    public ConfigTemplate loadConfigTemplate(String name) throws IOException {
        String resource = CONFIG_TEMPLATE_RESOURCE_PATH + name + ConfigLoader.DEFAULT.getFileSuffix();
        try (InputStream in = BlueMap.class.getResourceAsStream(resource)) {
            if (in == null) throw new IOException("Resource not found: " + resource);

            StringWriter writer = new StringWriter();
            InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            reader.transferTo(writer);

            return new ConfigTemplate(writer.toString());
        }
    }

    public Path resolveConfigFile(String name) {
        for (ConfigLoader configLoader : ConfigLoader.REGISTRY.values()) {
            Path path = configRoot.resolve(name + configLoader.getFileSuffix());
            if (Files.isRegularFile(path)) return path;
        }

        return configRoot.resolve(name + ConfigLoader.DEFAULT.getFileSuffix());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isConfigFile(Path file) {
        String fileName = file.getFileName().toString();
        for (ConfigLoader configLoader : ConfigLoader.REGISTRY.values())
            if (fileName.endsWith(configLoader.getFileSuffix())) return true;
        return false;
    }

    public String getConfigName(Path file) {
        String fileName = file.getFileName().toString();
        for (ConfigLoader configLoader : ConfigLoader.REGISTRY.values()) {
            String suffix = configLoader.getFileSuffix();
            if (fileName.endsWith(suffix))
                return fileName.substring(0, fileName.length() - suffix.length());
        }
        return fileName;
    }

    public Path getConfigRoot() {
        return configRoot;
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

    private ConfigurationLoader<? extends ConfigurationNode> getLoader(Path path){
        AbstractConfigurationLoader.Builder<?, ?> builder = null;
        for (ConfigLoader loader : ConfigLoader.REGISTRY.values()) {
            if (path.getFileName().endsWith(loader.getFileSuffix())) {
                builder = loader.createLoaderBuilder();
                break;
            }
        }

        if (builder == null)
            builder = ConfigLoader.DEFAULT.createLoaderBuilder();

        return builder
                .path(path)
                .defaultOptions(o -> o.serializers(b -> {
                    b.register(Vector2i.class, new Vector2iTypeSerializer());
                    b.register(Key.class, new KeyTypeSerializer());
                }))
                .build();
    }

}
