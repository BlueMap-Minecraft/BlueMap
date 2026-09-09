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
package de.bluecolored.bluemap.core.resources;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockState;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultBlockstatesConfig {

    private final Map<Key, BlockState> mappings = new ConcurrentHashMap<>();

    public void load(Path configFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(configFile)) {
            JsonReader json = new JsonReader(reader);
            json.setLenient(true);

            json.beginObject();
            while (json.hasNext()) {
                Key id = Key.parse(json.nextName(), Key.MINECRAFT_NAMESPACE);
                String formatted = json.nextString();

                try {
                    BlockState blockState = BlockState.fromString(formatted);

                    // don't overwrite already present values, higher priority resources are loaded first
                    mappings.putIfAbsent(blockState.getId(), blockState);
                } catch (IllegalArgumentException e) {
                    Logger.global.logDebug("Failed to parse the blockstate for id '" + id + "': " + e);
                }
            }
            json.endObject();
        }
    }

    public void load(Map<Key, BlockState> defaultBlockStates) {
        defaultBlockStates.forEach(mappings::putIfAbsent);
    }

    public void save(Path configFile) throws IOException {
        // sort entries for a stable output
        Map<Key, BlockState> sorted = new TreeMap<>(Comparator.comparing(Key::getFormatted));
        sorted.putAll(mappings);

        Path folder = configFile.getParent();
        if (folder != null) Files.createDirectories(folder);

        try (BufferedWriter writer = Files.newBufferedWriter(configFile)) {
            JsonWriter json = new JsonWriter(writer);
            json.setIndent("  ");

            json.beginObject();
            for (Map.Entry<Key, BlockState> entry : sorted.entrySet()) {
                json.name(entry.getKey().getFormatted());
                json.value(entry.getValue().toString());
            }
            json.endObject();

            json.flush();
        }
    }

    public BlockState get(Key key) {
        return mappings.get(key);
    }

}
