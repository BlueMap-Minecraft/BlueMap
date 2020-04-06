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
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;

@Mod(Plugin.PLUGIN_ID)
public class ForgeMod implements ServerInterface {
	
	private Plugin bluemap;
	
	private Map<String, UUID> worldUUIDs;
	
	private ForgeCommands commands;
	
	public ForgeMod() {
		Logger.global = new Log4jLogger(LogManager.getLogger(Plugin.PLUGIN_NAME));
		
		this.bluemap = new Plugin("forge", this);
		this.commands = new ForgeCommands(this, bluemap);
		this.worldUUIDs = new HashMap<>();
		
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
		this.worldUUIDs.clear();
		
		for (ServerWorld world : event.getServer().getWorlds()) {
			try {
				registerWorld(world);
			} catch (IOException e) {
				Logger.global.logError("Failed to register world: " + world.getProviderName(), e);
			}
			
			try {
				world.save(null, false, false);
			} catch (Throwable t) {
				Logger.global.logError("Failed to save world: " + world.getProviderName(), t);
			}
		}
		
		this.commands.registerCommands(event.getCommandDispatcher());
		
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
	
	private void registerWorld(ServerWorld world) throws IOException {
		getUUIDForWorld(world);
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
		synchronized (worldUUIDs) {
			String key = worldFolder.getCanonicalPath();
			
			UUID uuid = worldUUIDs.get(key);
			if (uuid == null) {
				throw new IOException("There is no world with this folder loaded: " + worldFolder.getPath());
			}
			
			return uuid;
		}
	}
	
	public UUID getUUIDForWorld(ServerWorld world) throws IOException {
		synchronized (worldUUIDs) {
			String key = getFolderForWorld(world).getPath();
			
			UUID uuid = worldUUIDs.get(key);
			if (uuid == null) {
				uuid = UUID.randomUUID();
				worldUUIDs.put(key, uuid);
			}
			
			return uuid;	
		}
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
	public File getConfigFolder() {
		return new File("config/bluemap");
	}
    
}
