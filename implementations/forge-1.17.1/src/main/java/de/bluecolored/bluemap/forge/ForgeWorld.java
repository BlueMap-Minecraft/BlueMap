package de.bluecolored.bluemap.forge;

import de.bluecolored.bluemap.common.plugin.serverinterface.Dimension;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public class ForgeWorld implements ServerWorld {

    private final WeakReference<ServerLevel> delegate;
    private final Path saveFolder;

    public ForgeWorld(ServerLevel delegate) {
        this.delegate = new WeakReference<>(delegate);

        MinecraftServer server = delegate.getServer();
        Path worldFolder = delegate.getServer().getServerDirectory().toPath().resolve(server.getWorldPath(LevelResource.ROOT));
        this.saveFolder = DimensionType.getStorageFolder(delegate.dimension(), worldFolder.toFile()).toPath()
                .toAbsolutePath().normalize();
    }

    @Override
    public Dimension getDimension() {
        ServerLevel world = delegate.get();
        if (world != null) {
            if (world.dimension().equals(Level.NETHER)) return Dimension.NETHER;
            if (world.dimension().equals(Level.END)) return Dimension.END;
            if (world.dimension().equals(Level.OVERWORLD)) return Dimension.OVERWORLD;
        }

        return ServerWorld.super.getDimension();
    }

    @Override
    public boolean persistWorldChanges() throws IOException {
        ServerLevel world = delegate.get();
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
