package de.bluecolored.bluemap.forge;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.logger.Logger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

@Mod(Plugin.PLUGIN_ID)
public class ForgeMod implements ServerInterface {
	
	private Plugin bluemap;
	
	private MinecraftServer server;
	private Map<String, UUID> worldUUIDs;
	
	public ForgeMod() {
		Logger.global = new Log4jLogger(LogManager.getLogger());
		
		this.bluemap = new Plugin("forge", this);
		this.worldUUIDs = new HashMap<>();
		
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
		this.server = event.getServer();
		this.worldUUIDs.clear();
		
		for (ServerWorld world : event.getServer().getWorlds()) {
			try {
				world.save(null, false, false);
			} catch (Throwable t) {
				Logger.global.logError("Failed to save world: " + world.getProviderName(), t);
			}
		}
		
		new Thread(() -> {
			try {
				Logger.global.logInfo("Loading...");
				bluemap.load();
				if (bluemap.isLoaded()) Logger.global.logInfo("Loaded!");
			} catch (Throwable t) {
				Logger.global.logError("Failed to load!", t);
			}
		}).start();
    }
	
	@SubscribeEvent
    public void onServerStopping(FMLServerStoppingEvent event) {
		Logger.global.logInfo("Stopping...");
		bluemap.unload();
		Logger.global.logInfo("Saved and stopped!");
    }

	@Override
	public void registerListener(ServerEventListener listener) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unregisterAllListeners() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public UUID getUUIDForWorld(File worldFolder) throws IOException {
		
		
		worldFolder = worldFolder.getCanonicalFile();
		
		for (ServerWorld world : server.getWorlds()) {
			if (worldFolder.equals(world.getSaveHandler().getWorldDirectory().getCanonicalFile())) return getUUIDForWorld(world);
		}

		throw new IOException("There is no world with this folder loaded: " + worldFolder.getPath());
	}
	
	public UUID getUUIDForWorld(World world) {
		synchronized (worldUUIDs) {
			String key = world.getWorldInfo().getWorldName();
			
			UUID uuid = worldUUIDs.get(key);
			if (uuid == null) {
				uuid = UUID.randomUUID();
				worldUUIDs.put(key, uuid);
			}
			
			return uuid;	
		}
	}

	@Override
	public File getConfigFolder() {
		//TODO
		return new File(server.getDataDirectory(), "config");
	}
    
}
