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
package de.bluecolored.bluemap.forge;

import de.bluecolored.bluemap.common.plugin.serverinterface.Dimension;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.storage.FolderName;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class ForgeWorld implements ServerWorld {

    private final WeakReference<net.minecraft.world.server.ServerWorld> delegate;
    private final Path saveFolder;

    public ForgeWorld(net.minecraft.world.server.ServerWorld delegate) {
        this.delegate = new WeakReference<>(delegate);

        MinecraftServer server = delegate.getServer();
        Path worldFolder = delegate.getServer().getDataDirectory().toPath().resolve(server.func_240776_a_(FolderName.field_237253_i_));
        this.saveFolder = DimensionType.func_236031_a_(delegate.func_234923_W_(), worldFolder.toFile()).toPath()
                .toAbsolutePath().normalize();
    }

    @Override
    public Dimension getDimension() {
        net.minecraft.world.server.ServerWorld world = delegate.get();
        if (world != null) {
            if (world.func_234923_W_().equals(World.field_234919_h_)) return Dimension.NETHER;
            if (world.func_234923_W_().equals(World.field_234920_i_)) return Dimension.END;
            if (world.func_234923_W_().equals(World.field_234918_g_)) return Dimension.OVERWORLD;
        }

        return ServerWorld.super.getDimension();
    }

    @Override
    public boolean persistWorldChanges() throws IOException {
        net.minecraft.world.server.ServerWorld world = delegate.get();
        if (world == null) return false;

        var taskResult = CompletableFuture.supplyAsync(() -> {
            try {
                world.save(null, true, false);
                return true;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, world.getServer());

        try {
            return taskResult.get();
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
    public Path getSaveFolder() {
        return this.saveFolder;
    }

}
