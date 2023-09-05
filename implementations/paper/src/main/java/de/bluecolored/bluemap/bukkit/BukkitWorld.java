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

import de.bluecolored.bluemap.common.serverinterface.Dimension;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import org.bukkit.World;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class BukkitWorld implements ServerWorld {

    private final WeakReference<World> delegate;
    private final Path saveFolder;

    public BukkitWorld(World delegate) {
        this.delegate = new WeakReference<>(delegate);
        Dimension dimension = getDimension();
        Path saveFolder = delegate.getWorldFolder().toPath()
                .resolve(dimension.getDimensionSubPath())
                .toAbsolutePath().normalize();

        // fix for hybrids
        if (!Files.exists(saveFolder)) {
            Path direct = delegate.getWorldFolder().toPath();
            if (Files.exists(direct) && direct.endsWith(dimension.getDimensionSubPath()))
                saveFolder = direct;
        }

        this.saveFolder = saveFolder;
    }

    @Override
    public Dimension getDimension() {
        World world = delegate.get();
        if (world != null) {
            if (world.getEnvironment().equals(World.Environment.NETHER)) return Dimension.NETHER;
            if (world.getEnvironment().equals(World.Environment.THE_END)) return Dimension.END;
        }
        return Dimension.OVERWORLD;
    }

    @Override
    public Optional<String> getId() {
        World world = delegate.get();
        if (world != null) {
            return Optional.of(world.getUID().toString());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getName() {
        World world = delegate.get();
        if (world != null) {
            return Optional.of(world.getName());
        }
        return Optional.empty();
    }

    @Override
    public boolean persistWorldChanges() {
        /* Not supported by folia
        World world = delegate.get();
        if (world != null) {
            world.save();
            return true;
        }
        */
        return false;
    }

    @Override
    public Path getSaveFolder() {
        return this.saveFolder;
    }

}
