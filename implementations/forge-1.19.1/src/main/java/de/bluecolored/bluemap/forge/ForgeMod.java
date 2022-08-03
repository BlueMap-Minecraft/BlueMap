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
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.serverinterface.ServerInterface;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.logger.Logger;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod(Plugin.PLUGIN_ID)
public class ForgeMod implements ServerInterface {

    private final Plugin pluginInstance;
    private MinecraftServer serverInstance = null;

    private final ForgeEventForwarder eventForwarder;
    private final LoadingCache<ServerLevel, ServerWorld> worlds;

    private int playerUpdateIndex = 0;
    private final Map<UUID, Player> onlinePlayerMap;
    private final List<ForgePlayer> onlinePlayerList;

    public ForgeMod() {
        Logger.global = new Log4jLogger(LogManager.getLogger(Plugin.PLUGIN_NAME));

        this.onlinePlayerMap = new ConcurrentHashMap<>();
        this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());

        this.pluginInstance = new Plugin("forge-1.19.1", this);

        this.eventForwarder = new ForgeEventForwarder();
        this.worlds = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .weakKeys()
                .maximumSize(1000)
                .build(ForgeWorld::new);

        MinecraftForge.EVENT_BUS.register(this);

        //Make sure the mod being absent on the other network side does not cause the client to display the server as incompatible
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.serverInstance = event.getServer();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        //register commands
        new Commands<>(pluginInstance, event.getDispatcher(), forgeSource ->
                new ForgeCommandSource(this, pluginInstance, forgeSource)
        );
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
            } catch (IOException e) {
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
    public MinecraftVersion getMinecraftVersion() {
        try {
            return MinecraftVersion.of(SharedConstants.getCurrentVersion().getReleaseTarget());
        } catch (IllegalArgumentException ex) {
            return MinecraftVersion.LATEST_SUPPORTED;
        }
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
    public Collection<ServerWorld> getLoadedWorlds() {
        Collection<ServerWorld> loadedWorlds = new ArrayList<>(3);
        for (ServerLevel serverWorld : serverInstance.getAllLevels()) {
            loadedWorlds.add(worlds.get(serverWorld));
        }
        return loadedWorlds;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<ServerWorld> getWorld(Object world) {
        if (world instanceof Path)
            return getWorld((Path) world);

        if (world instanceof String) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse((String) world);
            if (resourceLocation != null) world = serverInstance.getLevel(ResourceKey.create(Registry.DIMENSION_REGISTRY, resourceLocation));
        }

        if (world instanceof ResourceKey) {
            try {
                world = serverInstance.getLevel((ResourceKey<Level>) world);
            } catch (ClassCastException ignored) {}
        }

        if (world instanceof ServerLevel)
            return Optional.of(getWorld((ServerLevel) world));

        return Optional.empty();
    }

    public ServerWorld getWorld(ServerLevel world) {
        return worlds.get(world);
    }

    @Override
    public Path getConfigFolder() {
        return Path.of("config", "bluemap");
    }

    @Override
    public Optional<Path> getModsFolder() {
        return Optional.of(Path.of("mods"));
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerLoggedInEvent evt) {
        var playerInstance = evt.getEntity();
        if (!(playerInstance instanceof ServerPlayer)) return;

        ForgePlayer player = new ForgePlayer(playerInstance.getUUID(), this, getPlugin().getBlueMap());
        onlinePlayerMap.put(player.getUuid(), player);
        onlinePlayerList.add(player);
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerLoggedOutEvent evt) {
        var player = evt.getEntity();
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
