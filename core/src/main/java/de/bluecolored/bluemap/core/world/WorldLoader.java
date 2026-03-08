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
package de.bluecolored.bluemap.core.world;

import de.bluecolored.bluemap.core.resources.pack.datapack.DataPack;
import de.bluecolored.bluemap.core.util.Key;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public interface WorldLoader {

    /**
     * Loads the world (one dimension of a level) from a Path, a dimension-key and a DataPack.<br>
     * The Path is deserialized directly from the map-config and could either be directly the location of the world-data (world-folder)
     * or it could be the path to another config-file that is providing more information on how to load this world for the WorldLoader.<br>
     * It is up to the implementation of the WorldLoader how to interpret the path.
     */
    World loadWorld(Path path, Key dimension, DataPack dataPack) throws IOException, InterruptedException;

    /**
     * Returns a list of DataPacks that should be loaded additionally when loading the provided Path / dimension.
     */
    default List<Path> worldDataPacks(Path path, Key dimension) throws IOException, InterruptedException {
        Path worldPacksFolder = path.resolve("datapacks");
        if (Files.isDirectory(worldPacksFolder)) {
            try (Stream<Path> worldPacksStream = Files.list(worldPacksFolder)) {
                return worldPacksStream.toList();
            }
        }
        return List.of();
    }

}
