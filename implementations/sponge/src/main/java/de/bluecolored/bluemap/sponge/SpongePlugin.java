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

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.serverinterface.Server;
import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.sponge.SpongeCommands.SpongeCommandProxy;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RefreshGameEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Ticks;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.plugin.PluginContainer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@org.spongepowered.plugin.builtin.jvm.Plugin(Plugin.PLUGIN_ID)
public class SpongePlugin implements Server {
    private static SpongePlugin instance;

    private final PluginContainer pluginContainer;

    @SuppressWarnings("unused")
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configurationDir;

    private final Plugin pluginInstance;
    private final SpongeCommands commands;

    private ExecutorService asyncExecutor;
    private ExecutorService syncExecutor;

    private int playerUpdateIndex = 0;
    private final Map<UUID, Player> onlinePlayerMap;
    private final List<SpongePlayer> onlinePlayerList;

    private final LoadingCache<org.spongepowered.api.world.server.ServerWorld, ServerWorld> worlds;

    @Inject
    public SpongePlugin(org.apache.logging.log4j.Logger logger, PluginContainer pluginContainer/*, Metrics.Factory metricsFactory*/) {
        Logger.global.clear();
        Logger.global.put(new Log4J2Logger(logger));

        this.pluginContainer = pluginContainer;

        this.onlinePlayerMap = new ConcurrentHashMap<>();
        this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());

        this.pluginInstance = new Plugin("sponge", this);
        this.commands = new SpongeCommands(pluginInstance);

        this.worlds = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .weakKeys()
                .maximumSize(1000)
                .build(SpongeWorld::new);

        //bstats
        //metricsFactory.make(5911);

        SpongePlugin.instance = this;
    }

    @Listener
    public void onRegisterCommands(final RegisterCommandEvent<Command.Raw> event) {
        //register commands
        for(SpongeCommandProxy command : commands.getRootCommands()) {
            event.register(this.pluginContainer, command, command.getLabel());
        }

    }

    @Listener
    public void onServerStart(StartedEngineEvent<org.spongepowered.api.Server> evt) {
        asyncExecutor = evt.game().asyncScheduler().executor(pluginContainer);
        syncExecutor = evt.engine().scheduler().executor(pluginContainer);

        //start updating players
        Task task = Task.builder()
                .interval(Ticks.of(1))
                .execute(this::updateSomePlayers)
                .plugin(pluginContainer)
                .build();
        evt.engine().scheduler().submit(task);

        asyncExecutor.execute(() -> {
            try {
                Logger.global.logInfo("Loading...");
                pluginInstance.load();
                if (pluginInstance.isLoaded()) Logger.global.logInfo("Loaded!");
            } catch (IOException | RuntimeException e) {
                Logger.global.logError("Failed to load!", e);
                pluginInstance.unload();
            }
        });
    }

    @Listener
    public void onServerStop(StoppingEngineEvent<org.spongepowered.api.Server> evt) {
        Logger.global.logInfo("Stopping...");
        evt.engine().scheduler().tasks(pluginContainer).forEach(ScheduledTask::cancel);
        pluginInstance.unload();
        Logger.global.logInfo("Saved and stopped!");
    }

    @Listener
    public void onServerReload(RefreshGameEvent evt) {
        asyncExecutor.execute(() -> {
            try {
                Logger.global.logInfo("Reloading...");
                pluginInstance.reload();
                Logger.global.logInfo("Reloaded!");
            } catch (IOException | RuntimeException e) {
                Logger.global.logError("Failed to load!", e);
                pluginInstance.unload();
            }
        });

        // update player list
        onlinePlayerMap.clear();
        synchronized (onlinePlayerList) {
            onlinePlayerList.clear();
            for (ServerPlayer spongePlayer : Sponge.server().onlinePlayers()) {
                SpongePlayer player = new SpongePlayer(spongePlayer.uniqueId());
                onlinePlayerMap.put(spongePlayer.uniqueId(), player);
                onlinePlayerList.add(player);
            }
        }
    }

    @Listener
    public void onPlayerJoin(ServerSideConnectionEvent.Join evt) {
        SpongePlayer player = new SpongePlayer(evt.player().uniqueId());
        onlinePlayerMap.put(evt.player().uniqueId(), player);
        onlinePlayerList.add(player);
    }

    @Listener
    public void onPlayerLeave(ServerSideConnectionEvent.Leave evt) {
        UUID playerUUID = evt.player().uniqueId();
        onlinePlayerMap.remove(playerUUID);
        synchronized (onlinePlayerList) {
            onlinePlayerList.removeIf(p -> p.getUuid().equals(playerUUID));
        }
    }

    @Override
    public String getMinecraftVersion() {
        return Sponge.platform().minecraftVersion().name();
    }

    @Override
    public void registerListener(ServerEventListener listener) {
        Sponge.eventManager().registerListeners(this.pluginContainer, new EventForwarder(listener));
    }

    @Override
    public void unregisterAllListeners() {
        Sponge.eventManager().unregisterListeners(this.pluginContainer);
        Sponge.eventManager().registerListeners(this.pluginContainer, this);
    }

    @Override
    public Collection<ServerWorld> getLoadedServerWorlds() {
        Collection<ServerWorld> loadedWorlds = new ArrayList<>(3);
        for (var world : Sponge.server().worldManager().worlds()) {
            loadedWorlds.add(worlds.get(world));
        }
        return loadedWorlds;
    }

    @Override
    public Optional<ServerWorld> getServerWorld(Object world) {

        if (world instanceof String) {
            ResourceKey resourceKey = ResourceKey.resolve((String) world);
            var serverWorld = Sponge.server().worldManager().world(resourceKey).orElse(null);
            if (serverWorld != null) world = serverWorld;
        }

        if (world instanceof ResourceKey) {
            var serverWorld = Sponge.server().worldManager().world((ResourceKey) world).orElse(null);
            if (serverWorld != null) world = serverWorld;
        }

        if (world instanceof org.spongepowered.api.world.server.ServerWorld)
            return Optional.of(getServerWorld((org.spongepowered.api.world.server.ServerWorld) world));

        return Optional.empty();
    }

    public ServerWorld getServerWorld(org.spongepowered.api.world.server.ServerWorld world) {
        return worlds.get(world);
    }

    @Override
    public Path getConfigFolder() {
        return configurationDir;
    }

    @Override
    public Optional<Path> getModsFolder() {
        return Optional.of(Path.of("mods"));
    }

    @Override
    public Collection<Player> getOnlinePlayers() {
        return onlinePlayerMap.values();
    }

    @Override
    public de.bluecolored.bluemap.core.util.Tristate isMetricsEnabled() {
        if (pluginContainer != null) {
            Tristate metricsEnabled = Sponge.metricsConfigManager().collectionState(pluginContainer);
            if (metricsEnabled != Tristate.UNDEFINED) {
                if (metricsEnabled == Tristate.TRUE) return de.bluecolored.bluemap.core.util.Tristate.TRUE;
                return de.bluecolored.bluemap.core.util.Tristate.FALSE;
            }
        }

        return Sponge.metricsConfigManager().globalCollectionState() == Tristate.TRUE ?
                de.bluecolored.bluemap.core.util.Tristate.TRUE :
                de.bluecolored.bluemap.core.util.Tristate.FALSE;
    }

    /**
     * Only update some of the online players each tick to minimize performance impact on the server-thread.
     * Only call this method on the server-thread.
     */
    private void updateSomePlayers() {
        synchronized (onlinePlayerList) {
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

    public static Vector3d fromSpongePoweredVector(org.spongepowered.math.vector.Vector3d vec) {
        return new Vector3d(vec.x(), vec.y(), vec.z());
    }

    public static Vector3i fromSpongePoweredVector(org.spongepowered.math.vector.Vector3i vec) {
        return new Vector3i(vec.x(), vec.y(), vec.z());
    }

    public static Vector2i fromSpongePoweredVector(org.spongepowered.math.vector.Vector2i vec) {
        return new Vector2i(vec.x(), vec.y());
    }

    public ExecutorService getSyncExecutor() {
        return syncExecutor;
    }

    public Plugin getPlugin() {
        return pluginInstance;
    }

    public static SpongePlugin getInstance() {
        return instance;
    }

    @Override
    public Map<Key, Integer> getSkyBrightness() {
        Map<Key, Integer> skyBrightness = new HashMap<>();
        for (var world : Sponge.server().worldManager().worlds()) {
            int darken = 0;
            long time = world.properties().dayTime().asTicks().ticks() % 24000;

            if (time >= 12100 && time <= 12800) darken = 3;
            else if (time > 12800 && time <= 13200) darken = 6;
            else if (time > 13200 && time <= 22700) darken = 11;
            else if (time > 23000 && time <= 23400) darken = 6;
            else if (time > 23400 && time <= 23850) darken = 3;
            else darken = 0;

            skyBrightness.put(worlds.get(world).getDimension(), darken);
        }
        return skyBrightness;
    }
}
