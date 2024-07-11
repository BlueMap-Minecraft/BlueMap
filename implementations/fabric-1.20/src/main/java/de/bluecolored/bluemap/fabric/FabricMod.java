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
package de.bluecolored.bluemap.fabric;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.commands.Commands;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.serverinterface.Server;
import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Key;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.SharedConstants;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FabricMod implements ModInitializer, Server {

    private final Plugin pluginInstance;
    private MinecraftServer serverInstance = null;

    private final FabricEventForwarder eventForwarder;

    private final LoadingCache<net.minecraft.server.world.ServerWorld, ServerWorld> worlds;

    private int playerUpdateIndex = 0;
    private final Map<UUID, Player> onlinePlayerMap;
    private final List<FabricPlayer> onlinePlayerList;

    public FabricMod() {
        Logger.global.clear();
        Logger.global.put(new Log4jLogger(LogManager.getLogger(Plugin.PLUGIN_NAME)));

        this.onlinePlayerMap = new ConcurrentHashMap<>();
        this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());

        pluginInstance = new Plugin("fabric-1.20", this);

        this.eventForwarder = new FabricEventForwarder(this);
        this.worlds = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .weakKeys()
                .maximumSize(1000)
                .build(FabricWorld::new);
    }

    @Override
    public void onInitialize() {

        //register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                new Commands<>(pluginInstance, dispatcher, fabricSource ->
                        new FabricCommandSource(this, pluginInstance, fabricSource)
                )
        );

        ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
            this.serverInstance = server;

            new Thread(()->{
                Logger.global.logInfo("Loading BlueMap...");

                try {
                    pluginInstance.load();
                    if (pluginInstance.isLoaded()) Logger.global.logInfo("BlueMap loaded!");
                } catch (IOException e) {
                    Logger.global.logError("Failed to load bluemap!", e);
                    pluginInstance.unload();
                }
            }, "BlueMap-Plugin-Loading").start();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register((MinecraftServer server) -> {
            pluginInstance.unload();
            Logger.global.logInfo("BlueMap unloaded!");
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            this.onPlayerJoin(server, handler.getPlayer());
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            this.onPlayerLeave(server, handler.getPlayer());
        });

        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
            if (server == this.serverInstance) this.updateSomePlayers();
        });
    }

    @Override
    public String getMinecraftVersion() {
        return SharedConstants.getGameVersion().getId();
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
        for (net.minecraft.server.world.ServerWorld serverWorld : serverInstance.getWorlds()) {
            loadedWorlds.add(worlds.get(serverWorld));
        }
        return loadedWorlds;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<ServerWorld> getServerWorld(Object world) {

        if (world instanceof String) {
            Identifier identifier = Identifier.tryParse((String) world);
            if (identifier != null) world = serverInstance.getWorld(RegistryKey.of(RegistryKeys.WORLD, identifier));
        }

        if (world instanceof RegistryKey) {
            try {
                world = serverInstance.getWorld((RegistryKey<World>) world);
            } catch (ClassCastException ignored) {}
        }

        if (world instanceof net.minecraft.server.world.ServerWorld)
            return Optional.of(getServerWorld((net.minecraft.server.world.ServerWorld) world));

        return Optional.empty();
    }

    public ServerWorld getServerWorld(net.minecraft.server.world.ServerWorld serverWorld) {
        return worlds.get(serverWorld);
    }

    @Override
    public Path getConfigFolder() {
        return Path.of("config", "bluemap");
    }

    @Override
    public Optional<Path> getModsFolder() {
        return Optional.of(Path.of("mods"));
    }

    public void onPlayerJoin(MinecraftServer server, ServerPlayerEntity playerInstance) {
        if (this.serverInstance != server) return;

        FabricPlayer player = new FabricPlayer(playerInstance.getUuid(), this);
        onlinePlayerMap.put(player.getUuid(), player);
        onlinePlayerList.add(player);
    }

    public void onPlayerLeave(MinecraftServer server, ServerPlayerEntity player) {
        if (this.serverInstance != server) return;

        UUID playerUUID = player.getUuid();
        onlinePlayerMap.remove(playerUUID);
        synchronized (onlinePlayerList) {
            onlinePlayerList.removeIf(p -> p.getUuid().equals(playerUUID));
        }
    }

    public MinecraftServer getServer() {
        return this.serverInstance;
    }

    public Plugin getPluginInstance() {
        return pluginInstance;
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

    @Override
    public Map<Key, Integer> getSkyBrightness() {
        Map<Key, Integer> skyBrightness = new HashMap<>();
        for (net.minecraft.server.world.ServerWorld world : serverInstance.getWorlds()) {
            skyBrightness.put(worlds.get(world).getDimension(), world.getAmbientDarkness());
        }
        return skyBrightness;
    }

}
