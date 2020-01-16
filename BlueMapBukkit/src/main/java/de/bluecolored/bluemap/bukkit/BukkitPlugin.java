package de.bluecolored.bluemap.bukkit;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bstats.bukkit.MetricsLite;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.logger.Logger;

public class BukkitPlugin extends JavaPlugin implements ServerInterface {
	
	private Plugin bluemap;
	private EventForwarder eventForwarder;
	private BukkitCommands commands;
	
	public BukkitPlugin() {
		Logger.global = new JavaLogger(getLogger());

		this.eventForwarder = new EventForwarder();
		this.bluemap = new Plugin("bukkit", this);
		this.commands = new BukkitCommands(bluemap.getCommands());
	}
	
	@Override
	public void onEnable() {
		new MetricsLite(this);
		
		getServer().getPluginManager().registerEvents(eventForwarder, this);
		getCommand("bluemap").setExecutor(commands);
		
		getServer().getScheduler().runTaskAsynchronously(this, () -> {
			try {
				Logger.global.logInfo("Loading...");
				this.bluemap.load();
				if (bluemap.isLoaded()) Logger.global.logInfo("Loaded!");
			} catch (Throwable t) {
				Logger.global.logError("Failed to load!", t);
			}
		});
	}
	
	@Override
	public void onDisable() {
		Logger.global.logInfo("Stopping...");
		bluemap.unload();
		Logger.global.logInfo("Saved and stopped!");
	}

	@Override
	public void registerListener(ServerEventListener listener) {
		eventForwarder.addListener(listener);
	}

	@Override
	public void unregisterAllListeners() {
		eventForwarder.removeAllListeners();
	}

	@Override
	public UUID getUUIDForWorld(File worldFolder) throws IOException {
		worldFolder = worldFolder.getCanonicalFile();
		for (World world : getServer().getWorlds()) {
			Logger.global.logInfo("Found world-folder: " + world.getWorldFolder().getCanonicalPath());
			if (worldFolder.equals(world.getWorldFolder().getCanonicalFile())) return world.getUID();
		}
		
		throw new IOException("There is no world with this folder loaded: " + worldFolder.getCanonicalPath());
	}

	@Override
	public File getConfigFolder() {
		return getDataFolder();
	}
	
	public Plugin getBlueMap() {
		return bluemap;
	}

}
