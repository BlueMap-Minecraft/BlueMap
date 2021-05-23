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

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.inject.Inject;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.Player;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.ParseResourceException;
import de.bluecolored.bluemap.sponge8.SpongeCommands.SpongeCommandProxy;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.NBTUtil;
import org.spongepowered.api.Platform;
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
import org.spongepowered.plugin.PluginContainer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

@org.spongepowered.plugin.jvm.Plugin(Plugin.PLUGIN_ID)
public class SpongePlugin implements ServerInterface {

	private final PluginContainer pluginContainer;

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
	
	@Inject
	public SpongePlugin(org.apache.logging.log4j.Logger logger, PluginContainer pluginContainer/*, Metrics.Factory metricsFactory*/) {
		Logger.global = new Log4J2Logger(logger);
		this.pluginContainer = pluginContainer;

		this.onlinePlayerMap = new ConcurrentHashMap<>();
		this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());

		final String versionFromSponge = Sponge.platform().container(Platform.Component.GAME).metadata().version();
		MinecraftVersion version = new MinecraftVersion(1, 16);
		try {
			version = MinecraftVersion.of(versionFromSponge);
		} catch (IllegalArgumentException e) {
			Logger.global.logWarning("Failed to find a matching version for version-name '" + versionFromSponge + "'! Using latest known sponge-version: " + version.getVersionString());
		}
		
		this.pluginInstance = new Plugin(version, "sponge-8.0.0", this);
		this.commands = new SpongeCommands(pluginInstance);

		//bstats
		//metricsFactory.make(5911);
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
		try {
			CompoundTag levelSponge = (CompoundTag) NBTUtil.readTag(new File(worldFolder, "level.dat"));
			CompoundTag spongeData = levelSponge.getCompoundTag("SpongeData");
			int[] uuidIntArray = spongeData.getIntArray("UUID");
			if (uuidIntArray.length != 4) throw new IOException("World-UUID is stored in a wrong format! Is the worlds level.dat corrupted?");
			return intArrayToUuid(uuidIntArray);
		} catch (IOException | RuntimeException e) {
			throw new IOException("Failed to read the worlds level.dat!", e);
		}
	}

	@Override
	public String getWorldName(UUID worldUUID) {
		return getServerWorld(worldUUID)
				.flatMap(
						serverWorld -> serverWorld
								.properties()
								.displayName()
								.map(SpongeComponents.plainSerializer()::serialize)
				)
				.orElse(null);
	}

	private Optional<ServerWorld> getServerWorld(UUID worldUUID) {
		for (ServerWorld world : Sponge.server().worldManager().worlds()) {
			if (world.uniqueId().equals(worldUUID)) return Optional.of(world);
		}

		return Optional.empty();
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
				return metricsEnabled == Tristate.TRUE;
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
		return new Vector3d(vec.x(), vec.y(), vec.z());
	}

	public static Vector3i fromSpongePoweredVector(org.spongepowered.math.vector.Vector3i vec) {
		return new Vector3i(vec.x(), vec.y(), vec.z());
	}

	public static Vector2i fromSpongePoweredVector(org.spongepowered.math.vector.Vector2i vec) {
		return new Vector2i(vec.x(), vec.y());
	}

	private static UUID intArrayToUuid(int[] array) {
		if (array.length != 4) throw new IllegalArgumentException("Int array has to contain exactly 4 ints!");
		return new UUID(
				(long) array[0] << 32 | (long) array[1] & 0x00000000FFFFFFFFL,
				(long) array[2] << 32 | (long) array[3] & 0x00000000FFFFFFFFL
		);
	}
	
}
