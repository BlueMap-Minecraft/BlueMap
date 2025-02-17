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
import de.bluecolored.bluecommands.brigadier.BrigadierBridge;
import de.bluecolored.bluemap.common.commands.BrigadierExecutionHandler;
import de.bluecolored.bluemap.common.commands.Commands;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.serverinterface.Server;
import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod(Plugin.PLUGIN_ID)
public class ForgeMod implements Server {

    private final Plugin pluginInstance;
    private MinecraftServer serverInstance = null;

    private final ForgeEventForwarder eventForwarder;
    private final LoadingCache<ServerLevel, ServerWorld> worlds;

    private int playerUpdateIndex = 0;
    private final Map<UUID, Player> onlinePlayerMap;
    private final List<ForgePlayer> onlinePlayerList;

    public ForgeMod() {
        Logger.global.clear();
        Logger.global.put(new Log4jLogger(LogManager.getLogger(Plugin.PLUGIN_NAME)));

        this.onlinePlayerMap = new ConcurrentHashMap<>();
        this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());

        this.pluginInstance = new Plugin("neoforge", this);

        this.eventForwarder = new ForgeEventForwarder();
        this.worlds = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .weakKeys()
                .maximumSize(1000)
                .build(ForgeWorld::new);

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        this.serverInstance = event.getServer();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        //register commands
        BrigadierBridge.createCommandNodes(
                Commands.create(pluginInstance),
                new BrigadierExecutionHandler(pluginInstance),
                (CommandSourceStack forgeSource) -> new ForgeCommandSource(this, forgeSource)
        ).forEach(event.getDispatcher().getRoot()::addChild);
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
        }, "BlueMap-Plugin-Loading").start();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        pluginInstance.unload();
        Logger.global.logInfo("BlueMap unloaded!");
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent.Post evt) {
        updateSomePlayers();
    }

    @Override
    public String getMinecraftVersion() {
        return SharedConstants.getCurrentVersion().getId();
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
    public Collection<ServerWorld> getLoadedServerWorlds() {
        Collection<ServerWorld> loadedWorlds = new ArrayList<>(3);
        for (ServerLevel serverWorld : serverInstance.getAllLevels()) {
            loadedWorlds.add(worlds.get(serverWorld));
        }
        return loadedWorlds;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<ServerWorld> getServerWorld(Object world) {

        if (world instanceof String) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse((String) world);
            if (resourceLocation != null) world = serverInstance.getLevel(ResourceKey.create(Registries.DIMENSION, resourceLocation));
        }

        if (world instanceof ResourceKey) {
            try {
                world = serverInstance.getLevel((ResourceKey<Level>) world);
            } catch (ClassCastException ignored) {}
        }

        if (world instanceof ServerLevel)
            return Optional.of(getServerWorld((ServerLevel) world));

        return Optional.empty();
    }

    public ServerWorld getServerWorld(ServerLevel world) {
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
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        var playerInstance = evt.getEntity();
        if (!(playerInstance instanceof ServerPlayer)) return;

        ForgePlayer player = new ForgePlayer(playerInstance.getUUID(), this);
        onlinePlayerMap.put(player.getUuid(), player);
        onlinePlayerList.add(player);
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
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
