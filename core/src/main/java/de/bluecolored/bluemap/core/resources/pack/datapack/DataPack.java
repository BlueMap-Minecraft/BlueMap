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
package de.bluecolored.bluemap.core.resources.pack.datapack;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.pack.Pack;
import de.bluecolored.bluemap.core.resources.pack.datapack.biome.DatapackBiome;
import de.bluecolored.bluemap.core.resources.pack.datapack.dimension.DimensionTypeData;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.biome.Biome;
import de.bluecolored.bluemap.core.world.DimensionType;
import de.bluecolored.bluemap.core.world.mca.chunk.LegacyBiomes;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DataPack extends Pack {

    public static final Key DIMENSION_OVERWORLD = new Key("minecraft", "overworld");
    public static final Key DIMENSION_THE_NETHER = new Key("minecraft", "the_nether");
    public static final Key DIMENSION_THE_END = new Key("minecraft", "the_end");

    public static final Key DIMENSION_TYPE_OVERWORLD = new Key("minecraft", "overworld");
    public static final Key DIMENSION_TYPE_OVERWORLD_CAVES = new Key("minecraft", "overworld_caves");
    public static final Key DIMENSION_TYPE_THE_NETHER = new Key("minecraft", "the_nether");
    public static final Key DIMENSION_TYPE_THE_END = new Key("minecraft", "the_end");

    private final Map<Key, DimensionType> dimensionTypes = new HashMap<>();
    private final Map<Key, Biome> biomes = new HashMap<>();

    private LegacyBiomes legacyBiomes;

    public DataPack(int packVersion) {
        super(packVersion);
    }

    @Override
    public void loadResources(Iterable<Path> roots) throws IOException, InterruptedException {
        Logger.global.logDebug("Loading datapack...");

        for (Path root : roots) {
            Logger.global.logDebug("Loading datapack from: " + root + " ...");
            loadResources(root);
        }

        Logger.global.logDebug("Baking datapack...");
        bake();

        Logger.global.logDebug("Datapack loaded.");
    }

    private void loadResources(Path root) throws InterruptedException, IOException {
        loadResourcePath(root, this::loadPath);
    }

    private void loadPath(Path root) {
        list(root.resolve("data"))
                .map(path -> path.resolve("dimension_type"))
                .filter(Files::isDirectory)
                .flatMap(DataPack::walk)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .filter(Files::isRegularFile)
                .forEach(file -> loadResource(root, file, 1, 3, key -> {
                    try (BufferedReader reader = Files.newBufferedReader(file)) {
                        return ResourcesGson.INSTANCE.fromJson(reader, DimensionTypeData.class);
                    }
                }, dimensionTypes));

        list(root.resolve("data"))
                .map(path -> path.resolve("worldgen").resolve("biome"))
                .filter(Files::isDirectory)
                .flatMap(DataPack::walk)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .filter(Files::isRegularFile)
                .forEach(file -> loadResource(root, file, 1, 4, key -> {
                    try (BufferedReader reader = Files.newBufferedReader(file)) {
                        return new DatapackBiome(key, ResourcesGson.INSTANCE.fromJson(reader, DatapackBiome.Data.class));
                    }
                }, biomes));
    }

    public void bake() {
        dimensionTypes.putIfAbsent(DIMENSION_TYPE_OVERWORLD, DimensionType.OVERWORLD);
        dimensionTypes.putIfAbsent(DIMENSION_TYPE_OVERWORLD_CAVES, DimensionType.OVERWORLD_CAVES);
        dimensionTypes.putIfAbsent(DIMENSION_TYPE_THE_NETHER, DimensionType.NETHER);
        dimensionTypes.putIfAbsent(DIMENSION_TYPE_THE_END, DimensionType.END);

        legacyBiomes = new LegacyBiomes(this);
    }

    public @Nullable DimensionType getDimensionType(Key key) {
        return dimensionTypes.get(key);
    }

    public @Nullable Biome getBiome(Key key) {
        return biomes.get(key);
    }

    public @Nullable Biome getBiome(int legacyId) {
        return legacyBiomes.forId(legacyId);
    }

}
