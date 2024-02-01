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
package de.bluecolored.bluemap.bukkit;

import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.resources.datapack.DataPack;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.mca.MCAWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

public class BukkitWorld implements ServerWorld {

    private final WeakReference<World> delegate;
    private final Path worldFolder;
    private final Key dimension;

    public BukkitWorld(World delegate) {
        this.delegate = new WeakReference<>(delegate);
        this.dimension = resolveDimension(delegate);
        Path worldFolder = delegate.getWorldFolder().toPath();

        // fix for hybrids
        Path dimensionFolder = MCAWorld.resolveDimensionFolder(worldFolder, dimension);
        if (!Files.exists(worldFolder)) {
            Path dimensionSubPath = worldFolder.relativize(dimensionFolder);

            if (Files.exists(worldFolder) && worldFolder.endsWith(dimensionSubPath))
                worldFolder = worldFolder.subpath(0, worldFolder.getNameCount() - dimensionSubPath.getNameCount());
        }

        this.worldFolder = worldFolder;
    }

    private Key resolveDimension(World world) {
        if (world.getEnvironment().equals(World.Environment.NETHER)) return DataPack.DIMENSION_THE_NETHER;
        if (world.getEnvironment().equals(World.Environment.THE_END)) return DataPack.DIMENSION_THE_END;
        return DataPack.DIMENSION_OVERWORLD;
    }

    @Override
    public boolean persistWorldChanges() throws IOException {
        World delegateWorld = delegate.get();
        if (delegateWorld == null) return false;

        try {
            return Bukkit.getScheduler().callSyncMethod(BukkitPlugin.getInstance(), () -> {
                delegateWorld.save();
                return true;
            }).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof IOException) throw (IOException) t;
            if (t instanceof IllegalArgumentException) throw (IllegalArgumentException) t;
            throw new IOException(t);
        }
    }

    @Override
    public Path getWorldFolder() {
        return worldFolder;
    }

    @Override
    public Key getDimension() {
        return dimension;
    }

}
