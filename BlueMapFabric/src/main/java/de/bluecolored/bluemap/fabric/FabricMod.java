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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.commands.Commands;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.ParseResourceException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.dimension.DimensionType;

public class FabricMod implements ModInitializer, ServerInterface {

	private Plugin pluginInstance = null;
	
	private Map<File, UUID> worldUuids;
	private FabricEventForwarder eventForwarder;
	
	private LoadingCache<ServerWorld, UUID> worldUuidCache;
	
	public FabricMod() {
		Logger.global = new Log4jLogger(LogManager.getLogger(Plugin.PLUGIN_NAME));
		
		pluginInstance = new Plugin("fabric", this);
		
		this.worldUuids = new ConcurrentHashMap<>();
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
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			new Commands<>(pluginInstance, dispatcher, fabricSource -> new FabricCommandSource(this, pluginInstance, fabricSource));
		});
		
		ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer server) -> {
			new Thread(()->{
				Logger.global.logInfo("Loading BlueMap...");
				
				try {
					pluginInstance.load();
					Logger.global.logInfo("BlueMap loaded!");
				} catch (IOException | ParseResourceException e) {
					Logger.global.logError("Failed to load bluemap!", e);
				}
			}).start();
		});
		
		ServerLifecycleEvents.SERVER_STOPPING.register((MinecraftServer server) -> {
			pluginInstance.unload();
			Logger.global.logInfo("BlueMap unloaded!");
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
		
		UUID uuid = worldUuids.get(worldFolder);
		if (uuid == null) {
			uuid = UUID.randomUUID();
			worldUuids.put(worldFolder, uuid);
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
		MinecraftServer server = world.getServer();
		String worldName = server.getSaveProperties().getLevelName();
		File worldFolder = new File(world.getServer().getRunDirectory(), worldName);
		File dimensionFolder = DimensionType.getSaveDirectory(world.getRegistryKey(), worldFolder);
		File dimensionDir = dimensionFolder.getCanonicalFile();
		return getUUIDForWorld(dimensionDir);
	}

	@Override
	public File getConfigFolder() {
		return new File("config/bluemap");
	}
	
}
