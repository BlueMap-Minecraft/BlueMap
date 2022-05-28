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
package de.bluecolored.bluemap.core.resources.biome;

import com.google.gson.stream.JsonReader;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.biome.datapack.DpBiome;
import de.bluecolored.bluemap.core.world.Biome;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@DebugDump
public class BiomeConfig {

    private final Map<String, Biome> biomes;

    public BiomeConfig() {
        biomes = new HashMap<>();
    }

    public void load(Path configFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(configFile)) {
            JsonReader json = new JsonReader(reader);
            json.setLenient(true);

            json.beginObject();

            while (json.hasNext()) {
                String formatted = json.nextName();
                BiomeConfigEntry entry = ResourcesGson.INSTANCE.fromJson(json, BiomeConfigEntry.class);
                Biome biome = entry.createBiome(formatted);

                // don't overwrite already present values, higher priority resources are loaded first
                biomes.putIfAbsent(biome.getFormatted(), biome);
            }

            json.endObject();
        }
    }

    public void loadDatapackBiome(String namespace, Path biomeFile) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(biomeFile)) {
            JsonReader json = new JsonReader(reader);
            json.setLenient(true);
            DpBiome dpBiome = ResourcesGson.INSTANCE.fromJson(json, DpBiome.class);

            String formatted = namespace + ":" + biomeFile.getFileName().toString();
            int fileEndingDot = formatted.lastIndexOf('.');
            if (fileEndingDot != -1) formatted = formatted.substring(0, fileEndingDot);

            Biome biome = dpBiome.createBiome(formatted);

            // don't overwrite already present values, higher priority resources are loaded first
            biomes.putIfAbsent(biome.getFormatted(), biome);
        }
    }

    public Biome getBiome(String formatted) {
        return biomes.getOrDefault(formatted, Biome.DEFAULT);
    }

}
