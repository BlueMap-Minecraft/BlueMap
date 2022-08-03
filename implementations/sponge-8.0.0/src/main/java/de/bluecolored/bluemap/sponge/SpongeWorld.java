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
package de.bluecolored.bluemap.sponge;

import de.bluecolored.bluemap.common.serverinterface.Dimension;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spongepowered.api.world.WorldTypes;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class SpongeWorld implements ServerWorld {

    private final WeakReference<org.spongepowered.api.world.server.ServerWorld> delegate;
    private final Path saveFolder;

    public SpongeWorld(org.spongepowered.api.world.server.ServerWorld delegate) {
        this.delegate = new WeakReference<>(delegate);
        this.saveFolder = delegate.directory()
                .resolve(getDimension().getDimensionSubPath())
                .toAbsolutePath().normalize();
    }

    @Override
    public Dimension getDimension() {
        var world = delegate.get();
        if (world != null) {
            if (world.worldType().equals(WorldTypes.THE_NETHER.get())) return Dimension.NETHER;
            if (world.worldType().equals(WorldTypes.THE_END.get())) return Dimension.END;
        }
        return Dimension.OVERWORLD;
    }

    @Override
    public Optional<String> getId() {
        var world = delegate.get();
        if (world != null) {
            return Optional.of(world.uniqueId().toString());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getName() {
        var world = delegate.get();
        if (world != null) {
            return world.properties().displayName()
                    .map(PlainTextComponentSerializer.plainText()::serialize);
        }
        return Optional.empty();
    }

    @Override
    public boolean persistWorldChanges() throws IOException {
        var delegateWorld = delegate.get();
        if (delegateWorld == null) return false;

        try {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return delegateWorld.save();
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }, SpongePlugin.getInstance().getSyncExecutor()).get();
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
