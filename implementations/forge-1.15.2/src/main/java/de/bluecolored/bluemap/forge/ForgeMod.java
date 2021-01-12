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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import org.apache.logging.log4j.LogManager;

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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

@Mod(Plugin.PLUGIN_ID)
public class ForgeMod implements ServerInterface {

	private Plugin pluginInstance = null;
	private MinecraftServer serverInstance = null;
	
	private Map<File, UUID> worldUUIDs;
	private ForgeEventForwarder eventForwarder;
	
	private LoadingCache<ServerWorld, UUID> worldUuidCache;
	
	private int playerUpdateIndex = 0;
	private Map<UUID, Player> onlinePlayerMap;
	private List<ForgePlayer> onlinePlayerList;
	
	public ForgeMod() {
		Logger.global = new Log4jLogger(LogManager.getLogger(Plugin.PLUGIN_NAME));
		
		this.onlinePlayerMap = new ConcurrentHashMap<>();
		this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());
		
		this.pluginInstance = new Plugin(MinecraftVersion.MC_1_15, "forge-1.15.2", this);
		
		this.worldUUIDs = new ConcurrentHashMap<>();
		this.eventForwarder = new ForgeEventForwarder(this);
		this.worldUuidCache = Caffeine.newBuilder()
				.executor(BlueMap.THREAD_POOL)
				.weakKeys()
				.maximumSize(1000)
				.build(this::loadUUIDForWorld);

		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
		this.serverInstance = event.getServer();

		//register commands
		new Commands<>(pluginInstance, event.getServer().getCommandManager().getDispatcher(), forgeSource -> new ForgeCommandSource(this, pluginInstance, forgeSource));
    }

	@SubscribeEvent
	public void onServerStarted(FMLServerStartedEvent event) {
		//save worlds to generate level.dat files
		serverInstance.save(false, true, true);

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
    public void onServerStopping(FMLServerStoppingEvent event) {
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
		File key = getFolderForWorld(world);
		
		UUID uuid = worldUUIDs.get(key);
		if (uuid == null) {
			uuid = UUID.randomUUID();
			worldUUIDs.put(key, uuid);
		}
		
		return uuid;
	}
	
	private File getFolderForWorld(ServerWorld world) throws IOException {
		File worldFolder = world.getSaveHandler().getWorldDirectory();
		
		int dimensionId = world.getDimension().getType().getId();
		if (dimensionId != 0) {
			worldFolder = new File(worldFolder, "DIM" + dimensionId);
		}
		
		return worldFolder.getCanonicalFile();
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

	@SubscribeEvent
	public void onPlayerJoin(PlayerLoggedInEvent evt) {
		PlayerEntity playerInstance = evt.getPlayer();
		if (!(playerInstance instanceof ServerPlayerEntity)) return;

		ForgePlayer player = new ForgePlayer(this, playerInstance.getUniqueID());
		onlinePlayerMap.put(player.getUuid(), player);
		onlinePlayerList.add(player);
	}

	@SubscribeEvent
	public void onPlayerLeave(PlayerLoggedOutEvent evt) {
		PlayerEntity player = evt.getPlayer();
		if (!(player instanceof ServerPlayerEntity)) return;
		
		UUID playerUUID = player.getUniqueID();
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
