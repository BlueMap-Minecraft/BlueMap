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
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resources.pack.datapack.DataPack;
import de.bluecolored.bluemap.core.util.Key;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class BukkitWorld implements ServerWorld {

    private final WeakReference<World> delegate;
    private final Path worldFolder;
    private final Key dimension;

    public BukkitWorld(World delegate) {
        this.delegate = new WeakReference<>(delegate);

        Path dimensionFolder = delegate.getWorldPath().toAbsolutePath().normalize();
        int nameCount = dimensionFolder.getNameCount();
        if (nameCount < 3 || !"dimensions".equals(dimensionFolder.getName(nameCount - 3).toString())) {
            Logger.global.logWarning("Unexpected dimension folder path layout '" + dimensionFolder + "'.");
            this.worldFolder = dimensionFolder;
            this.dimension = DataPack.DIMENSION_OVERWORLD;
        } else {
            this.worldFolder = dimensionFolder.getParent().getParent().getParent();
            this.dimension = new Key(
                    dimensionFolder.getName(nameCount - 2).toString(),
                    dimensionFolder.getName(nameCount - 1).toString()
            );
        }
    }

    @Override
    public boolean persistWorldChanges() {
        if (!FoliaSupport.IS_FOLIA) {
            World world = delegate.get();
            if (world != null) {
                Executor mainThread = Bukkit.getScheduler().getMainThreadExecutor(BukkitPlugin.getInstance());
                CompletableFuture
                        .runAsync(world::save, mainThread)
                        .join();
                return true;
            }
        }

        return false;
    }

    @Override
    public Path getWorldFolder() {
        return worldFolder;
    }

    @Override
    public Key getDimension() {
        return dimension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BukkitWorld that = (BukkitWorld) o;
        Object world = delegate.get();
        return world != null && world.equals(that.delegate.get());
    }

    @Override
    public int hashCode() {
        Object world = delegate.get();
        return world != null ? world.hashCode() : 0;
    }

}
