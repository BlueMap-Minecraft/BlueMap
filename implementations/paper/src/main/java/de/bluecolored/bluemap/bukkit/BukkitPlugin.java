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

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
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
import de.bluecolored.bluemap.core.logger.JavaLogger;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Key;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitPlugin extends JavaPlugin implements Server, Listener {

    private static BukkitPlugin instance;

    private final Plugin pluginInstance;
    private final EventForwarder eventForwarder;

    private final Map<UUID, Player> onlinePlayerMap;
    private final List<BukkitPlayer> onlinePlayerList;

    private final Collection<ScheduledTask> scheduledTasks;

    private final LoadingCache<World, ServerWorld> worlds;

    public BukkitPlugin() {
        Logger.global.clear();
        Logger.global.put(new JavaLogger(getLogger()));

        this.onlinePlayerMap = new ConcurrentHashMap<>();
        this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());

        this.scheduledTasks = Collections.synchronizedCollection(Collections.newSetFromMap(new WeakHashMap<>()));

        this.eventForwarder = new EventForwarder();
        this.pluginInstance = new Plugin("paper", this);

        this.worlds = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .weakKeys()
                .maximumSize(1000)
                .build(BukkitWorld::new);

        BukkitPlugin.instance = this;
    }

    @Override
    public void onEnable() {

        //save world so the level.dat is present on new worlds
        if (!FoliaSupport.IS_FOLIA) {
            Logger.global.logInfo("Saving all worlds once, to make sure the level.dat is present...");
            for (World world : getServer().getWorlds()) {
                world.save();
            }
        }

        //register events
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(eventForwarder, this);

        //register commands
        //noinspection UnstableApiUsage
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {

            //noinspection UnstableApiUsage
            BrigadierBridge.createCommandNodes(
                    Commands.create(pluginInstance),
                    new BrigadierExecutionHandler(pluginInstance),
                    BukkitCommandSource::new
            ).forEach(commands.registrar().getDispatcher().getRoot()::addChild);

        });

        //update online-player collections
        this.onlinePlayerList.clear();
        this.onlinePlayerMap.clear();
        for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
            initPlayer(player);
        }

        //load bluemap
        new Thread(() -> {
            try {
                Logger.global.logInfo("Loading...");
                this.pluginInstance.load();
                if (pluginInstance.isLoaded()) Logger.global.logInfo("Loaded!");
            } catch (IOException | RuntimeException e) {
                Logger.global.logError("Failed to load!", e);
                this.pluginInstance.unload();
            }
        }, "BlueMap-Load").start();

        //bstats
        new Metrics(this, 5912);
    }

    @Override
    public void onDisable() {
        Logger.global.logInfo("Stopping...");
        scheduledTasks.forEach(task -> {
            if (task != null) task.cancel();
        });
        scheduledTasks.clear();
        pluginInstance.unload();
        Logger.global.logInfo("Saved and stopped!");
    }

    @Override
    public String getMinecraftVersion() {
        return ServerBuildInfo.buildInfo().minecraftVersionId();
    }

    @Override
    public void registerListener(ServerEventListener listener) {
        eventForwarder.addListener(listener);
    }

    @Override
    public void unregisterAllListeners() {
        eventForwarder.removeAllListeners();
    }

    @Override
    public Collection<ServerWorld> getLoadedServerWorlds() {
        Collection<ServerWorld> loadedWorlds = new ArrayList<>(3);
        for (World world : Bukkit.getWorlds()) {
            loadedWorlds.add(worlds.get(world));
        }
        return loadedWorlds;
    }

    @Override
    public Optional<ServerWorld> getServerWorld(Object world) {

        if (world instanceof String) {
            var serverWorld = Bukkit.getWorld((String) world);
            if (serverWorld != null) world = serverWorld;
        }

        if (world instanceof String) {
            var serverWorld = Bukkit.getWorld(new Key((String) world).getValue());
            if (serverWorld != null) world = serverWorld;
        }

        if (world instanceof UUID) {
            var serverWorld = Bukkit.getWorld((UUID) world);
            if (serverWorld != null) world = serverWorld;
        }

        if (world instanceof World)
            return Optional.of(getServerWorld((World) world));

        return Optional.empty();
    }

    public ServerWorld getServerWorld(World world) {
        return worlds.get(Objects.requireNonNull(world));
    }

    @Override
    public Path getConfigFolder() {
        return getDataFolder().toPath();
    }

    @Override
    public Optional<Path> getModsFolder() {
        return Optional.of(Path.of("mods")); // in case this is a Bukkit/Forge hybrid
    }

    public Plugin getPlugin() {
        return pluginInstance;
    }

    public static BukkitPlugin getInstance() {
        return instance;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent evt) {
        initPlayer(evt.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeave(PlayerQuitEvent evt) {
        UUID playerUUID = evt.getPlayer().getUniqueId();
        onlinePlayerMap.remove(playerUUID);
        synchronized (onlinePlayerList) {
            onlinePlayerList.removeIf(p -> p.getUuid().equals(playerUUID));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerPostRespawnEvent evt) {
        initPlayer(evt.getPlayer());
    }

    @Override
    public Collection<Player> getOnlinePlayers() {
        return onlinePlayerMap.values();
    }

    private void initPlayer(org.bukkit.entity.Player bukkitPlayer) {
        BukkitPlayer player = new BukkitPlayer(bukkitPlayer);
        onlinePlayerMap.put(bukkitPlayer.getUniqueId(), player);
        onlinePlayerList.add(player);

        // update player every 20 seconds
        scheduledTasks.add(
            bukkitPlayer.getScheduler().runAtFixedRate(this, task -> {
                player.update();
            }, null, 20, 20)
        );
    }

}
