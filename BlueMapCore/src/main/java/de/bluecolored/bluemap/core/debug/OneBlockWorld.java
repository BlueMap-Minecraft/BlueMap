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
package de.bluecolored.bluemap.core.debug;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.Grid;
import de.bluecolored.bluemap.core.world.Region;
import de.bluecolored.bluemap.core.world.World;

import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

public class OneBlockWorld implements World {

    private final World delegate;

    public OneBlockWorld(World delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public UUID getUUID() {
        return delegate.getUUID();
    }

    @Override
    public Path getSaveFolder() {
        return delegate.getSaveFolder();
    }

    @Override
    public int getSeaLevel() {
        return 64;
    }

    @Override
    public Vector3i getSpawnPoint() {
        return new Vector3i(0, 70, 0);
    }

    @Override
    public int getMaxY(int x, int z) {
        return 255;
    }

    @Override
    public int getMinY(int x, int z) {
        return 0;
    }

    @Override
    public Grid getChunkGrid() {
        return delegate.getChunkGrid();
    }

    @Override
    public Grid getRegionGrid() {
        return delegate.getRegionGrid();
    }

    @Override
    public Chunk getChunkAtBlock(int x, int y, int z) {
        return delegate.getChunkAtBlock(x, y, z);
    }

    @Override
    public Chunk getChunk(int x, int z) {
        return delegate.getChunk(x, z);
    }

    @Override
    public Region getRegion(int x, int z) {
        return delegate.getRegion(x, z);
    }

    @Override
    public Collection<Vector2i> listRegions() {
        return delegate.listRegions();
    }

    @Override
    public void invalidateChunkCache() {
        delegate.invalidateChunkCache();
    }

    @Override
    public void invalidateChunkCache(int x, int z) {
        delegate.invalidateChunkCache(x, z);
    }

    @Override
    public void cleanUpChunkCache() {
        delegate.cleanUpChunkCache();
    }

}
