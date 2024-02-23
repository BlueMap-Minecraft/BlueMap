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
package de.bluecolored.bluemap.core.resources.datapack;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.adapter.ResourcesGson;
import de.bluecolored.bluemap.core.resources.datapack.dimension.DimensionTypeData;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

public class DataPack {

    public static final Key DIMENSION_OVERWORLD = new Key("minecraft", "overworld");
    public static final Key DIMENSION_THE_NETHER = new Key("minecraft", "the_nether");
    public static final Key DIMENSION_THE_END = new Key("minecraft", "the_end");

    public static final Key DIMENSION_TYPE_OVERWORLD = new Key("minecraft", "overworld");
    public static final Key DIMENSION_TYPE_OVERWORLD_CAVES = new Key("minecraft", "overworld_caves");
    public static final Key DIMENSION_TYPE_THE_NETHER = new Key("minecraft", "the_nether");
    public static final Key DIMENSION_TYPE_THE_END = new Key("minecraft", "the_end");

    private final Map<Key, DimensionType> dimensionTypes = new HashMap<>();

    @Nullable
    public DimensionType getDimensionType(Key key) {
        return dimensionTypes.get(key);
    }

    public void load(Path root) throws InterruptedException {
        Logger.global.logDebug("Loading datapack from: " + root + " ...");
        loadPath(root);
    }

    private void loadPath(Path root) throws InterruptedException {
        if (Thread.interrupted()) throw new InterruptedException();
        if (!Files.isDirectory(root)) {
            try (FileSystem fileSystem = FileSystems.newFileSystem(root, (ClassLoader) null)) {
                for (Path fsRoot : fileSystem.getRootDirectories()) {
                    if (!Files.isDirectory(fsRoot)) continue;
                    loadPath(fsRoot);
                }
            } catch (Exception ex) {
                Logger.global.logDebug("Failed to read '" + root + "': " + ex);
            }
            return;
        }

        list(root.resolve("data"))
                .map(path -> path.resolve("dimension_type"))
                .filter(Files::isDirectory)
                .flatMap(DataPack::walk)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .filter(Files::isRegularFile)
                .forEach(file -> loadResource(root, file, () -> {
                    try (BufferedReader reader = Files.newBufferedReader(file)) {
                        return ResourcesGson.INSTANCE.fromJson(reader, DimensionTypeData.class);
                    }
                }, dimensionTypes));
    }

    private <T> void loadResource(Path root, Path file, Loader<T> loader, Map<Key, T> resultMap) {
        try {
            ResourcePath<T> resourcePath = new ResourcePath<>(root.relativize(file));
            if (resultMap.containsKey(resourcePath)) return; // don't load already present resources

            T resource = loader.load();
            if (resource == null) return; // don't load missing resources

            resourcePath.setResource(resource);
            resultMap.put(resourcePath, resource);
        } catch (Exception ex) {
            Logger.global.logDebug("Failed to parse resource-file '" + file + "': " + ex);
        }
    }

    public void bake() {
        dimensionTypes.putIfAbsent(DIMENSION_TYPE_OVERWORLD, DimensionType.OVERWORLD);
        dimensionTypes.putIfAbsent(DIMENSION_TYPE_OVERWORLD_CAVES, DimensionType.OVERWORLD_CAVES);
        dimensionTypes.putIfAbsent(DIMENSION_TYPE_THE_NETHER, DimensionType.NETHER);
        dimensionTypes.putIfAbsent(DIMENSION_TYPE_THE_END, DimensionType.END);
    }

    private static Stream<Path> list(Path root) {
        if (!Files.isDirectory(root)) return Stream.empty();
        try {
            return Files.list(root);
        } catch (IOException ex) {
            throw new CompletionException(ex);
        }
    }

    private static Stream<Path> walk(Path root) {
        if (!Files.exists(root)) return Stream.empty();
        if (Files.isRegularFile(root)) return Stream.of(root);
        try {
            return Files.walk(root);
        } catch (IOException ex) {
            throw new CompletionException(ex);
        }
    }

    private interface Loader<T> {
        T load() throws IOException;
    }

}
