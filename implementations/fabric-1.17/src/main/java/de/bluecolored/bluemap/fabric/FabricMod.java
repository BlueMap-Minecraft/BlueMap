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
import de.bluecolored.bluemap.common.plugin.serverinterface.Player;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.ParseResourceException;
import de.bluecolored.bluemap.fabric.events.PlayerJoinCallback;
import de.bluecolored.bluemap.fabric.events.PlayerLeaveCallback;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

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
		
		pluginInstance = new Plugin(new MinecraftVersion(1, 17), "fabric-1.17", this);
		
		this.worldUUIDs = new ConcurrentHashMap<>();
		this.eventForwarder = new FabricEventForwarder(this);
		this.worldUuidCache = Caffeine.newBuilder()
				.executor(BlueMap.THREAD_POOL)
				.weakKeys()
				.maximumSize(1000)
				.build(this::loadUUIDForWorld);
	}
	
	@Override
	public void onInitialize() {
		
		//register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			new Commands<>(pluginInstance, dispatcher, fabricSource -> new FabricCommandSource(this, pluginInstance, fabricSource));
		});
		
		ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
			this.serverInstance = server;
			
			new Thread(()->{
				Logger.global.logInfo("Loading BlueMap...");	
				
				try {
					pluginInstance.load();
					if (pluginInstance.isLoaded()) Logger.global.logInfo("BlueMap loaded!");
				} catch (IOException | ParseResourceException e) {
					Logger.global.logError("Failed to load bluemap!", e);
					pluginInstance.unload();
				}
			}).start();
		});
		
		ServerLifecycleEvents.SERVER_STOPPING.register((MinecraftServer server) -> {
			pluginInstance.unload();
			Logger.global.logInfo("BlueMap unloaded!");
		});
		
		PlayerJoinCallback.EVENT.register(this::onPlayerJoin);
		PlayerLeaveCallback.EVENT.register(this::onPlayerLeave);
		
		ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> {
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
		} catch (RuntimeException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException) throw (IOException) cause;
			else throw new IOException(cause);
		}
	}

	private UUID loadUUIDForWorld(ServerWorld world) throws IOException {
		MinecraftServer server = world.getServer();
		File worldFolder = world.getServer().getRunDirectory().toPath().resolve(server.getSavePath(WorldSavePath.ROOT)).toFile();
		File dimensionFolder = DimensionType.getSaveDirectory(world.getRegistryKey(), worldFolder);
		File dimensionDir = dimensionFolder.getCanonicalFile();
		return getUUIDForWorld(dimensionDir);
	}
	
	@Override
	public boolean persistWorldChanges(UUID worldUUID) throws IOException, IllegalArgumentException {
		final CompletableFuture<Boolean> taskResult = new CompletableFuture<>();
		
		serverInstance.execute(() -> {
			try {
				for (ServerWorld world : serverInstance.getWorlds()) {
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

	public void onPlayerJoin(MinecraftServer server, ServerPlayerEntity playerInstance) {
		if (this.serverInstance != server) return;

		FabricPlayer player = new FabricPlayer(this, playerInstance.getUuid());
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
				onlinePlayerList.get(playerUpdateIndex).update();
			}
		}
	}
	
}
