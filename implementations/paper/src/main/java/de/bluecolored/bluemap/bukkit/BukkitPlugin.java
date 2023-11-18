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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.serverinterface.Server;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.util.Key;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BukkitPlugin extends JavaPlugin implements Server, Listener {

    private static BukkitPlugin instance;

    private final Plugin pluginInstance;
    private final EventForwarder eventForwarder;
    private final BukkitCommands commands;
    private final MinecraftVersion minecraftVersion;

    private final Map<UUID, Player> onlinePlayerMap;
    private final List<BukkitPlayer> onlinePlayerList;

    private final Collection<ScheduledTask> scheduledTasks;

    private final LoadingCache<World, ServerWorld> worlds;

    public BukkitPlugin() {
        Logger.global.clear();
        Logger.global.put(new JavaLogger(getLogger()));

        //try to get best matching minecraft-version
        MinecraftVersion version = MinecraftVersion.LATEST_SUPPORTED;
        try {
            String versionString = getServer().getBukkitVersion();
            Matcher versionMatcher = Pattern.compile("(\\d+(?:\\.\\d+){1,2})[-_].*").matcher(versionString);
            if (!versionMatcher.matches()) throw new IllegalArgumentException();
            version = MinecraftVersion.of(versionMatcher.group(1));
        } catch (IllegalArgumentException e) {
            Logger.global.logWarning("Failed to detect the minecraft version of this server! Using latest version: " + version.getVersionString());
        }
        this.minecraftVersion = version;

        this.onlinePlayerMap = new ConcurrentHashMap<>();
        this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());

        this.scheduledTasks = Collections.synchronizedCollection(Collections.newSetFromMap(new WeakHashMap<>()));

        this.eventForwarder = new EventForwarder();
        this.pluginInstance = new Plugin("bukkit", this);
        this.commands = new BukkitCommands(this.pluginInstance);

        this.worlds = Caffeine.newBuilder()
                .executor(BlueMap.THREAD_POOL)
                .weakKeys()
                .maximumSize(1000)
                .build(BukkitWorld::new);

        BukkitPlugin.instance = this;
    }

    @Override
    public void onEnable() {

        //register events
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(eventForwarder, this);

        //register commands
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            for (BukkitCommand command : commands.getRootCommands()) {
                commandMap.register(command.getLabel(), command);
            }
        } catch(NoSuchFieldException | SecurityException | IllegalAccessException e) {
            Logger.global.logError("Failed to register commands!", e);
        }

        //tab completions
        getServer().getPluginManager().registerEvents(commands, this);

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
    public MinecraftVersion getMinecraftVersion() {
        return minecraftVersion;
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
    public Collection<ServerWorld> getLoadedWorlds() {
        Collection<ServerWorld> loadedWorlds = new ArrayList<>(3);
        for (World world : Bukkit.getWorlds()) {
            loadedWorlds.add(worlds.get(world));
        }
        return loadedWorlds;
    }

    @Override
    public Optional<ServerWorld> getWorld(Object world) {
        if (world instanceof Path)
            return getWorld((Path) world);

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
            return Optional.of(getWorld((World) world));

        return Optional.empty();
    }

    public ServerWorld getWorld(World world) {
        return worlds.get(world);
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

    @Override
    public Collection<Player> getOnlinePlayers() {
        return onlinePlayerMap.values();
    }

    @Override
    public Optional<Player> getPlayer(UUID uuid) {
        return Optional.ofNullable(onlinePlayerMap.get(uuid));
    }

    private void initPlayer(org.bukkit.entity.Player bukkitPlayer) {
        BukkitPlayer player = new BukkitPlayer(bukkitPlayer.getUniqueId());
        onlinePlayerMap.put(bukkitPlayer.getUniqueId(), player);
        onlinePlayerList.add(player);

        // update player every 20 seconds
        scheduledTasks.add(
            bukkitPlayer.getScheduler().runAtFixedRate(this, task -> {
                player.update();
            }, () -> {}, 20, 20)
        );
    }

}
