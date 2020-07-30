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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bstats.bukkit.MetricsLite;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.plugin.java.JavaPlugin;

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.PlayerState;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.logger.Logger;

public class BukkitPlugin extends JavaPlugin implements ServerInterface {
	
	private static BukkitPlugin instance;
	
	private Plugin bluemap;
	private EventForwarder eventForwarder;
	private BukkitCommands commands;
	
	public BukkitPlugin() {
		Logger.global = new JavaLogger(getLogger());

		this.eventForwarder = new EventForwarder();
		this.bluemap = new Plugin("bukkit", this);
		this.commands = new BukkitCommands(this.bluemap);
		
		BukkitPlugin.instance = this;
	}
	
	@Override
	public void onEnable() {
		new MetricsLite(this);
		
		//save world so the level.dat is present on new worlds
		Logger.global.logInfo("Saving all worlds once, to make sure the level.dat is present...");
		for (World world : getServer().getWorlds()) {
			world.save();
		}
		
		//register events
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
		
		//load bluemap
		getServer().getScheduler().runTaskAsynchronously(this, () -> {
			try {
				Logger.global.logInfo("Loading...");
				this.bluemap.load();
				if (bluemap.isLoaded()) Logger.global.logInfo("Loaded!");
			} catch (Throwable t) {
				Logger.global.logError("Failed to load!", t);
			}
		});
	}
	
	@Override
	public void onDisable() {
		Logger.global.logInfo("Stopping...");
		bluemap.unload();
		Logger.global.logInfo("Saved and stopped!");
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
	public UUID getUUIDForWorld(File worldFolder) throws IOException {
		//if it is a dimension folder
		if (!new File(worldFolder, "level.dat").exists()) {
			worldFolder = worldFolder.getParentFile();
		}
		
		final File normalizedWorldFolder = worldFolder.getCanonicalFile();

		Future<UUID> futureUUID;
		if (!Bukkit.isPrimaryThread()) {
			futureUUID = Bukkit.getScheduler().callSyncMethod(BukkitPlugin.getInstance(), () -> getUUIDForWorldSync(normalizedWorldFolder));
		} else {
			futureUUID = CompletableFuture.completedFuture(getUUIDForWorldSync(normalizedWorldFolder));
		}
		
		try {
			return futureUUID.get();
		} catch (InterruptedException e) {
			throw new IOException(e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			} else {
				throw new IOException(e);
			}
		}
	}
	
	@Override
	public String getWorldName(UUID worldUUID) {
		World world = getServer().getWorld(worldUUID);
		if (world != null) return world.getName();
		
		return null;
	}
	
	private UUID getUUIDForWorldSync (File worldFolder) throws IOException {
		for (World world : getServer().getWorlds()) {
			if (worldFolder.equals(world.getWorldFolder().getCanonicalFile())) return world.getUID();
		}
		
		throw new IOException("There is no world with this folder loaded: " + worldFolder.getPath());
	}

	@Override
	public File getConfigFolder() {
		return getDataFolder();
	}
	
	public Plugin getBlueMap() {
		return bluemap;
	}

	public static BukkitPlugin getInstance() {
		return instance;
	}

	@Override
	public Collection<PlayerState> getOnlinePlayers() {
		// TODO Implement
		return Collections.emptyList();
	}

	@Override
	public Optional<PlayerState> getPlayer(UUID uuid) {
		// TODO Implement
		return Optional.empty();
	}
	
}
