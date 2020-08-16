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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.commands.Commands;
import de.bluecolored.bluemap.common.plugin.serverinterface.Player;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.ParseResourceException;
import de.bluecolored.bluemap.fabric.events.PlayerJoinCallback;
import de.bluecolored.bluemap.fabric.events.PlayerLeaveCallback;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.fabricmc.fabric.api.event.server.ServerTickCallback;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class FabricMod implements ModInitializer, ServerInterface {

	private Plugin pluginInstance = null;
	private MinecraftServer serverInstance = null;
	
	private Map<File, UUID> worldUUIDs;
	private FabricEventForwarder eventForwarder;
	
	private LoadingCache<ServerWorld, UUID> worldUuidCache;

	private int playerUpdateIndex = 0;
	private Map<UUID, Player> onlinePlayerMap;
	private List<FabricPlayer> onlinePlayerList;
	
	public FabricMod() {
		Logger.global = new Log4jLogger(LogManager.getLogger(Plugin.PLUGIN_NAME));
		
		this.onlinePlayerMap = new ConcurrentHashMap<>();
		this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());
		
		pluginInstance = new Plugin("fabric", this);
		
		this.worldUUIDs = new ConcurrentHashMap<>();
		this.eventForwarder = new FabricEventForwarder(this);
		this.worldUuidCache = CacheBuilder.newBuilder()
				.weakKeys()
				.maximumSize(1000)
				.build(new CacheLoader<ServerWorld, UUID>() {
					@Override
					public UUID load(ServerWorld key) throws Exception {
						return loadUUIDForWorld(key);
					}
				});
	}
	
	@Override
	public void onInitialize() {
		
		//register commands
		CommandRegistry.INSTANCE.register(true, dispatcher -> {
			new Commands<>(pluginInstance, dispatcher, fabricSource -> new FabricCommandSource(this, pluginInstance, fabricSource));
		});
		
		ServerStartCallback.EVENT.register((MinecraftServer server) -> {
			this.serverInstance = server;
			
			new Thread(()->{
				Logger.global.logInfo("Loading BlueMap...");
				
				try {
					pluginInstance.load();
					if (pluginInstance.isLoaded()) Logger.global.logInfo("BlueMap loaded!");
				} catch (IOException | ParseResourceException e) {
					Logger.global.logError("Failed to load bluemap!", e);
				}
			}).start();
		});
		
		ServerStopCallback.EVENT.register((MinecraftServer server) -> {
			pluginInstance.unload();
			Logger.global.logInfo("BlueMap unloaded!");
		});
		
		PlayerJoinCallback.EVENT.register(this::onPlayerJoin);
		PlayerLeaveCallback.EVENT.register(this::onPlayerLeave);
		
		ServerTickCallback.EVENT.register((MinecraftServer server) -> {
			if (server == this.serverInstance) this.updateSomePlayers();
		});
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

	public UUID getUUIDForWorld(ServerWorld world) throws IOException {
		try {
			return worldUuidCache.get(world);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException) throw (IOException) cause;
			else throw new IOException(cause);
		}
	}

	private UUID loadUUIDForWorld(ServerWorld world) throws IOException {
		File dimensionDir = world.getDimension().getType().getSaveDirectory(world.getSaveHandler().getWorldDir());
		return getUUIDForWorld(dimensionDir);
	}

	@Override
	public File getConfigFolder() {
		return new File("config/bluemap");
	}

	public void onPlayerJoin(MinecraftServer server, ServerPlayerEntity playerInstance) {
		if (this.serverInstance != server) return;
		
		FabricPlayer player = new FabricPlayer(this, playerInstance);
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
				onlinePlayerList.get(i).update();
			}
		}
	}
	
}
