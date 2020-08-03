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
import net.fabricmc.fabric.api.event.server.ServerStartCallback;
import net.fabricmc.fabric.api.event.server.ServerStopCallback;
import net.fabricmc.fabric.api.registry.CommandRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

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
		CommandRegistry.INSTANCE.register(true, dispatcher -> {
			new Commands<>(pluginInstance, dispatcher, fabricSource -> new FabricCommandSource(this, pluginInstance, fabricSource));
		});
		
		ServerStartCallback.EVENT.register((MinecraftServer server) -> {
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
		
		ServerStopCallback.EVENT.register((MinecraftServer server) -> {
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
		File dimensionDir = world.getDimension().getType().getSaveDirectory(world.getSaveHandler().getWorldDir());
		return getUUIDForWorld(dimensionDir);
	}

	@Override
	public File getConfigFolder() {
		return new File("config/bluemap");
	}
	
}
