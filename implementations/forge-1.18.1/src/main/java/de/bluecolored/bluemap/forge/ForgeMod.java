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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.commands.Commands;
import de.bluecolored.bluemap.common.plugin.serverinterface.Player;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.ParseResourceException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

@Mod(Plugin.PLUGIN_ID)
public class ForgeMod implements ServerInterface {

    private final Plugin pluginInstance;
    private MinecraftServer serverInstance = null;

    private final Map<File, UUID> worldUUIDs;
    private final ForgeEventForwarder eventForwarder;

    private final LoadingCache<ServerLevel, UUID> worldUuidCache;

    private int playerUpdateIndex = 0;
    private final Map<UUID, Player> onlinePlayerMap;
    private final List<ForgePlayer> onlinePlayerList;

    public ForgeMod() {
        Logger.global = new Log4jLogger(LogManager.getLogger(Plugin.PLUGIN_NAME));

        this.onlinePlayerMap = new ConcurrentHashMap<>();
        this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());

        String versionString = net.minecraft.DetectedVersion.tryDetectVersion().getId();
        MinecraftVersion mcVersion;
        try {
            mcVersion = MinecraftVersion.of(versionString);
        } catch (IllegalArgumentException ex) {
            mcVersion = new MinecraftVersion(1, 18, 1);
            Logger.global.logWarning("Failed to derive version from version-string '" + versionString +
                                     "', falling back to version: " + mcVersion.getVersionString());
        }
        this.pluginInstance = new Plugin(mcVersion, "forge-1.16.2", this);

        this.worldUUIDs = new ConcurrentHashMap<>();
        this.eventForwarder = new ForgeEventForwarder();
        this.worldUuidCache = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .weakKeys()
                .maximumSize(1000)
                .build(this::loadUUIDForWorld);

        MinecraftForge.EVENT_BUS.register(this);

        //Make sure the mod being absent on the other network side does not cause the client to display the server as incompatible
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.serverInstance = event.getServer();

        //register commands
        new Commands<>(pluginInstance, event.getServer().getCommands().getDispatcher(), forgeSource -> new ForgeCommandSource(this, pluginInstance, forgeSource));
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        //save worlds to generate level.dat files
        serverInstance.saveAllChunks(false, true, true);

        new Thread(() -> {
            Logger.global.logInfo("Loading...");

            try {
                pluginInstance.load();
                if (pluginInstance.isLoaded()) Logger.global.logInfo("Loaded!");
            } catch (IOException | ParseResourceException e) {
                Logger.global.logError("Failed to load bluemap!", e);
                pluginInstance.unload();
            }
        }).start();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        pluginInstance.unload();
        Logger.global.logInfo("BlueMap unloaded!");
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent evt) {
        updateSomePlayers();
    }

    @Override
    public void registerListener(ServerEventListener listener) {
        eventForwarder.addEventListener(listener);
    }

    @Override
    public void unregisterAllListeners() {
        eventForwarder.removeAllListeners();
    }

    @Override
    public UUID getUUIDForWorld(File worldFolder) throws IOException {
        worldFolder = worldFolder.getCanonicalFile();

        UUID uuid = worldUUIDs.get(worldFolder);
        if (uuid == null) {
            uuid = UUID.randomUUID();
            worldUUIDs.put(worldFolder, uuid);
        }

        return uuid;
    }

    public UUID getUUIDForWorld(ServerLevel world) throws IOException {
        try {
            return worldUuidCache.get(world);
        } catch (RuntimeException e) {
            throw new IOException(e);
        }
    }

    private UUID loadUUIDForWorld(ServerLevel world) throws IOException {
        File key = getFolderForWorld(world);

        UUID uuid = worldUUIDs.get(key);
        if (uuid == null) {
            uuid = UUID.randomUUID();
            worldUUIDs.put(key, uuid);
        }

        return uuid;
    }

    private File getFolderForWorld(ServerLevel world) throws IOException {
        MinecraftServer server = world.getServer();
        Path worldFolder = world.getServer().getServerDirectory().toPath().resolve(server.getWorldPath(LevelResource.ROOT));
        Path dimensionFolder = DimensionType.getStorageFolder(world.dimension(), worldFolder);
        return dimensionFolder.toFile().getCanonicalFile();
    }

    @Override
    public boolean persistWorldChanges(UUID worldUUID) throws IOException, IllegalArgumentException {
        final CompletableFuture<Boolean> taskResult = new CompletableFuture<>();

        serverInstance.execute(() -> {
            try {
                for (ServerLevel world : serverInstance.getAllLevels()) {
                    if (getUUIDForWorld(world).equals(worldUUID)) {
                        world.save(null, true, false);
                    }
                }

                taskResult.complete(true);
            } catch (Exception e) {
                taskResult.completeExceptionally(e);
            }
        });

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
    public File getConfigFolder() {
        return new File("config/bluemap");
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerLoggedInEvent evt) {
        net.minecraft.world.entity.player.Player playerInstance = evt.getPlayer();
        if (!(playerInstance instanceof ServerPlayer)) return;

        ForgePlayer player = new ForgePlayer(this, playerInstance.getUUID());
        onlinePlayerMap.put(player.getUuid(), player);
        onlinePlayerList.add(player);
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerLoggedOutEvent evt) {
        net.minecraft.world.entity.player.Player player = evt.getPlayer();
        if (!(player instanceof ServerPlayer)) return;

        UUID playerUUID = player.getUUID();
        onlinePlayerMap.remove(playerUUID);
        synchronized (onlinePlayerList) {
            onlinePlayerList.removeIf(p -> p.getUuid().equals(playerUUID));
        }
    }

    public MinecraftServer getServer() {
        return this.serverInstance;
    }

    public Plugin getPlugin() {
        return this.pluginInstance;
    }

    @Override
    public Collection<Player> getOnlinePlayers() {
        return onlinePlayerMap.values();
    }

    @Override
    public Optional<Player> getPlayer(UUID uuid) {
        return Optional.ofNullable(onlinePlayerMap.get(uuid));
    }

    /**
     * Only update some of the online players each tick to minimize performance impact on the server-thread.
     * Only call this method on the server-thread.
     */
    private void updateSomePlayers() {
        int onlinePlayerCount = onlinePlayerList.size();
        if (onlinePlayerCount == 0) return;

        int playersToBeUpdated = onlinePlayerCount / 20; //with 20 tps, each player is updated once a second
        if (playersToBeUpdated == 0) playersToBeUpdated = 1;

        for (int i = 0; i < playersToBeUpdated; i++) {
            playerUpdateIndex++;
            if (playerUpdateIndex >= 20 && playerUpdateIndex >= onlinePlayerCount) playerUpdateIndex = 0;

            if (playerUpdateIndex < onlinePlayerCount) {
                onlinePlayerList.get(playerUpdateIndex).update();
            }
        }
    }

}
