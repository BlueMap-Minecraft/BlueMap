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

import javax.inject.Inject;

import org.bstats.sponge.MetricsLite2;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.Player;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.sponge.SpongeCommands.SpongeCommandProxy;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.NBTUtil;

@org.spongepowered.api.plugin.Plugin (
		id = Plugin.PLUGIN_ID, 
		name = Plugin.PLUGIN_NAME,
		authors = { "Blue (Lukas Rieger)" },
		description = "This plugin provides a fully 3D map of your world for your browser!"
		)
public class SpongePlugin implements ServerInterface {

	@Inject
	@ConfigDir(sharedRoot = false)
	private Path configurationDir;
	
	@SuppressWarnings("unused")
	@Inject
    private MetricsLite2 metrics;
	
	private Plugin bluemap;
	private SpongeCommands commands;

	private SpongeExecutorService asyncExecutor;
	
	private int playerUpdateIndex = 0;
	private Map<UUID, Player> onlinePlayerMap;
	private List<SpongePlayer> onlinePlayerList;
	
	@Inject
	public SpongePlugin(org.slf4j.Logger logger) {
		Logger.global = new Slf4jLogger(logger);
		
		this.onlinePlayerMap = new ConcurrentHashMap<>();
		this.onlinePlayerList = Collections.synchronizedList(new ArrayList<>());
		
		this.bluemap = new Plugin("sponge", this);
		this.commands = new SpongeCommands(bluemap);
	}
	
	@Listener
	public void onServerStart(GameStartingServerEvent evt) {
		asyncExecutor = Sponge.getScheduler().createAsyncExecutor(this);
		
		//save all world properties to generate level_sponge.dat files
		for (WorldProperties properties : Sponge.getServer().getAllWorldProperties()) {
			Sponge.getServer().saveWorldProperties(properties);
		}
		
		//register commands
		for(SpongeCommandProxy command : commands.getRootCommands()) {
			Sponge.getCommandManager().register(this, command, command.getLabel());
		}
		
		//start updating players
		Task.builder()
		.intervalTicks(1)
		.execute(this::updateSomePlayers)
		.submit(this);
		
		asyncExecutor.execute(() -> {
			try {
				Logger.global.logInfo("Loading...");
				bluemap.load();
				if (bluemap.isLoaded()) Logger.global.logInfo("Loaded!");
			} catch (Throwable t) {
				Logger.global.logError("Failed to load!", t);
			}
		});
	}

	@Listener
	public void onServerStop(GameStoppingEvent evt) {
		Logger.global.logInfo("Stopping...");
		Sponge.getScheduler().getScheduledTasks(this).forEach(t -> t.cancel());
		bluemap.unload();
		Logger.global.logInfo("Saved and stopped!");
	}
	
	@Listener
	public void onServerReload(GameReloadEvent evt) {
		asyncExecutor.execute(() -> {
			try {
				Logger.global.logInfo("Reloading...");
				bluemap.reload();
				Logger.global.logInfo("Reloaded!");
			} catch (Exception e) {
				Logger.global.logError("Failed to load!", e);
			}
		});
	}

	
	@Listener
	public void onPlayerJoin(ClientConnectionEvent.Join evt) {
		SpongePlayer player = new SpongePlayer(evt.getTargetEntity());
		onlinePlayerMap.put(evt.getTargetEntity().getUniqueId(), player);
		onlinePlayerList.add(player);
	}
	
	@Listener
	public void onPlayerLeave(ClientConnectionEvent.Disconnect evt) {
		UUID playerUUID = evt.getTargetEntity().getUniqueId();
		onlinePlayerMap.remove(playerUUID);
		synchronized (onlinePlayerList) {
			onlinePlayerList.removeIf(p -> p.getUuid().equals(playerUUID));	
		}
	}

	@Override
	public void registerListener(ServerEventListener listener) {
		Sponge.getEventManager().registerListeners(this, new EventForwarder(listener));
	}

	@Override
	public void unregisterAllListeners() {
		Sponge.getEventManager().unregisterPluginListeners(this);
		Sponge.getEventManager().registerListeners(this, this);
	}

	@Override
	public UUID getUUIDForWorld(File worldFolder) throws IOException {
		try {
			CompoundTag levelSponge = (CompoundTag) NBTUtil.readTag(new File(worldFolder, "level_sponge.dat"));
			CompoundTag spongeData = levelSponge.getCompoundTag("SpongeData");
			long most = spongeData.getLong("UUIDMost");
			long least = spongeData.getLong("UUIDLeast");
			return new UUID(most, least);
		} catch (Throwable t) {
			throw new IOException("Failed to read level_sponge.dat", t);
		}
	}
	
	@Override
	public String getWorldName(UUID worldUUID) {
		Optional<World> world = Sponge.getServer().getWorld(worldUUID);
		if (world.isPresent()) return world.get().getName();
		
		return null;
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
		PluginContainer pluginContainer = Sponge.getPluginManager().fromInstance(this).orElse(null);
		if (pluginContainer != null) {
			Tristate metricsEnabled = Sponge.getMetricsConfigManager().getCollectionState(pluginContainer);
			if (metricsEnabled != Tristate.UNDEFINED) {
				return metricsEnabled == Tristate.TRUE ? true : false;
			}
		}
		
		return Sponge.getMetricsConfigManager().getGlobalCollectionState().asBoolean();
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
