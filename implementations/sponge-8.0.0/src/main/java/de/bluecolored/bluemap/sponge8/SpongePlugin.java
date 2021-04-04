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
package de.bluecolored.bluemap.sponge8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.inject.Inject;
import org.spongepowered.api.Platform;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.adventure.SpongeComponents;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
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
import org.spongepowered.api.world.server.ServerWorld;

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.Player;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.ParseResourceException;
import de.bluecolored.bluemap.sponge8.SpongeCommands.SpongeCommandProxy;
import org.spongepowered.plugin.PluginContainer;

@org.spongepowered.plugin.jvm.Plugin(Plugin.PLUGIN_ID)
public class SpongePlugin implements ServerInterface {

	private final PluginContainer pluginContainer;
	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configurationDir;

// TODO Bstats needs updating
//	@Inject
//	@SuppressWarnings("unused")
//	private MetricsLite2 metrics;
	
	private Plugin pluginInstance;
	private SpongeCommands commands;

	private Map<File, UUID> worldUUIDs = new ConcurrentHashMap<>();

	private ExecutorService asyncExecutor;
	private ExecutorService syncExecutor;
	
	private int playerUpdateIndex = 0;
	private Map<UUID, Player> onlinePlayerMap;
	private List<SpongePlayer> onlinePlayerList;
	
	@Inject
	public SpongePlugin(org.apache.logging.log4j.Logger logger, PluginContainer pluginContainer) {
		Logger.global = new Log4J2Logger(logger);
		this.pluginContainer = pluginContainer;

		this.onlinePlayerMap = new ConcurrentHashMap<>();
		this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());

		final String versionFromSponge = Sponge.platform().container(Platform.Component.GAME).getMetadata().getVersion();
		MinecraftVersion version = MinecraftVersion.MC_1_16;
		try {
			version = MinecraftVersion.fromVersionString(versionFromSponge);
		} catch (IllegalArgumentException e) {
			Logger.global.logWarning("Failed to find a matching version for version-name '" + versionFromSponge + "'! Using latest known sponge-version: " + version.getVersionString());
		}
		
		this.pluginInstance = new Plugin(version, "sponge", this);
		this.commands = new SpongeCommands(pluginInstance);
	}

	@Listener
	public void onRegisterCommands(final RegisterCommandEvent<Command.Raw> event) {
		//register commands
		for(SpongeCommandProxy command : commands.getRootCommands()) {
			event.register(this.pluginContainer, command, command.getLabel());
		}

	}

	@Listener
	public void onServerStart(StartedEngineEvent<Server> evt) {
		asyncExecutor = evt.game().asyncScheduler().createExecutor(pluginContainer);
		syncExecutor = evt.engine().scheduler().createExecutor(pluginContainer);
		
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
			} catch (IOException | ParseResourceException | RuntimeException e) {
				Logger.global.logError("Failed to load!", e);
				pluginInstance.unload();
			}
		});
	}

	@Listener
	public void onServerStop(StoppingEngineEvent<Server> evt) {
		Logger.global.logInfo("Stopping...");
		evt.engine().scheduler().tasksByPlugin(pluginContainer).forEach(ScheduledTask::cancel);
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
			} catch (IOException | ParseResourceException | RuntimeException e) {
				Logger.global.logError("Failed to load!", e);
				pluginInstance.unload();
			}
		});
	}

	@Listener
	public void onPlayerJoin(ServerSideConnectionEvent.Join evt) {
		SpongePlayer player = new SpongePlayer(evt.player().uniqueId());
		onlinePlayerMap.put(evt.player().uniqueId(), player);
		onlinePlayerList.add(player);
	}
	
	@Listener
	public void onPlayerLeave(ServerSideConnectionEvent.Disconnect evt) {
		UUID playerUUID = evt.player().uniqueId();
		onlinePlayerMap.remove(playerUUID);
		synchronized (onlinePlayerList) {
			onlinePlayerList.removeIf(p -> p.getUuid().equals(playerUUID));	
		}
	}

	@Override
	public void registerListener(ServerEventListener listener) {
		Sponge.eventManager().registerListeners(this.pluginContainer, new EventForwarder(listener));
	}

	@Override
	public void unregisterAllListeners() {
		Sponge.eventManager().unregisterPluginListeners(this.pluginContainer);
		Sponge.eventManager().registerListeners(this.pluginContainer, this);
	}

	@Override
	public UUID getUUIDForWorld(File worldFolder) throws IOException {
		// this logic derives the the world key from the folder structure
		final Pattern customDimension = Pattern.compile(".+/dimensions/([a-z0-9_.-]+)/([a-z0-9._-]+)$".replace("/", File.separator));
		final Matcher matcher = customDimension.matcher(worldFolder.toString());
		final ResourceKey key;
		if (matcher.matches()) {
			key = ResourceKey.of(matcher.group(1), matcher.group(2));
		} else if ("DIM-1".equals(worldFolder.getName())) {
			key = ResourceKey.minecraft("the_nether");
		} else if ("DIM1".equals(worldFolder.getName())) {
			key = ResourceKey.minecraft("the_end");
		} else {
			// assume it's the main world
			key = Sponge.server().worldManager().defaultWorld().key();
		}

		return Sponge.server().worldManager().world(key)
				.map(ServerWorld::uniqueId)
				.orElse(null);
	}
	
	@Override
	public String getWorldName(UUID worldUUID) {
		return getServerWorld(worldUUID)
				.map(serverWorld -> serverWorld
						.properties()
						.displayName()
						.map(SpongeComponents.plainSerializer()::serialize)
						.orElse(serverWorld.key().asString()))
				.orElse(null);
	}

	private Optional<ServerWorld> getServerWorld(UUID worldUUID) {
		return Sponge.server().worldManager().worldKey(worldUUID).flatMap(k -> Sponge.server().worldManager().world(k));
	}

	@Override
	public File getConfigFolder() {
		return configurationDir.toFile();
	}

	@Override
	public Collection<Player> getOnlinePlayers() {
		return onlinePlayerMap.values();
	}

	@Override
	public Optional<Player> getPlayer(UUID uuid) {
		return Optional.ofNullable(onlinePlayerMap.get(uuid));
	}
	
	@Override
	public boolean isMetricsEnabled(boolean configValue) {
		if (pluginContainer != null) {
			Tristate metricsEnabled = Sponge.metricsConfigManager().collectionState(pluginContainer);
			if (metricsEnabled != Tristate.UNDEFINED) {
				return metricsEnabled == Tristate.TRUE ? true : false;
			}
		}
		
		return Sponge.metricsConfigManager().globalCollectionState().asBoolean();
	}
	
	@Override
	public boolean persistWorldChanges(UUID worldUUID) throws IOException, IllegalArgumentException {
		try {
			return syncExecutor.submit(() -> {
				ServerWorld world = getServerWorld(worldUUID).orElse(null);
				if (world == null) throw new IllegalArgumentException("There is no world with this uuid: " + worldUUID);
				world.save();
				
				return true;
			}).get();
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

	public static Vector3d fromSpongePoweredVector(org.spongepowered.math.vector.Vector3d vec) {
		return new Vector3d(vec.getX(), vec.getY(), vec.getZ());
	}

	public static Vector3i fromSpongePoweredVector(org.spongepowered.math.vector.Vector3i vec) {
		return new Vector3i(vec.getX(), vec.getY(), vec.getZ());
	}

	public static Vector2i fromSpongePoweredVector(org.spongepowered.math.vector.Vector2i vec) {
		return new Vector2i(vec.getX(), vec.getY());
	}
	
}
