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

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.util.WatchService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Represents a World on the Server.<br>
 * This is usually one of the dimensions of a level.<br>
 * <br>
 * <i>The implementation of this class has to be thread-save!</i><br>
 */
public interface World {

    String getId();

    String getName();

    Vector3i getSpawnPoint();

    DimensionType getDimensionType();

    Grid getChunkGrid();

    Grid getRegionGrid();

    /**
     * Returns the {@link Chunk} on the specified block-position
     */
    Chunk getChunkAtBlock(int x, int z);

    /**
     * Returns the {@link Chunk} on the specified chunk-position
     */
    Chunk getChunk(int x, int z);

    /**
     * Returns the {@link Region} on the specified region-position
     */
    Region getRegion(int x, int z);

    /**
     * Returns a collection of all regions in this world.
     * <i>(Be aware that the collection is not cached and recollected each time from the world-files!)</i>
     */
    Collection<Vector2i> listRegions();

    /**
     * Creates and returns a new {@link WatchService} which watches for any changes in this worlds regions.
     * @throws IOException if an IOException occurred while creating the watch-service
     * @throws UnsupportedOperationException if watching this world is not supported
     */
    default WatchService<Vector2i> createRegionWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Loads all chunks from the specified region into the chunk cache (if there is a cache)
     */
    default void preloadRegionChunks(int x, int z) {
        preloadRegionChunks(x, z, pos -> true);
    }

    /**
     * Loads the filtered chunks from the specified region into the chunk cache (if there is a cache)
     */
    void preloadRegionChunks(int x, int z, Predicate<Vector2i> chunkFilter);

    /**
     * Invalidates the complete chunk cache (if there is a cache), so that every chunk has to be reloaded from disk
     */
    void invalidateChunkCache();

    /**
     * Invalidates the chunk from the chunk-cache (if there is a cache), so that the chunk has to be reloaded from disk
     */
    void invalidateChunkCache(int x, int z);

    /**
     * Generates a unique world-id based on a world-folder and a dimension
     */
    static String id(Path worldFolder, Key dimension) {
        worldFolder = worldFolder.toAbsolutePath().normalize();

        Path workingDir = Path.of("").toAbsolutePath().normalize();
        if (worldFolder.startsWith(workingDir))
            worldFolder = workingDir.relativize(worldFolder);

        return worldFolder + "#" + dimension.getFormatted();
    }

}
