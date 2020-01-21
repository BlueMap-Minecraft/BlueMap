package de.bluecolored.bluemap.bukkit;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bstats.bukkit.MetricsLite;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.logger.Logger;

public class BukkitPlugin extends JavaPlugin implements ServerInterface {
	
	private static BukkitPlugin instance;
	
	private Plugin bluemap;
	private EventForwarder eventForwarder;
	private BukkitCommands commands;
	
	public BukkitPlugin() {
		Logger.global = new JavaLogger(getLogger());

		this.eventForwarder = new EventForwarder();
		this.bluemap = new Plugin("bukkit", this);
		this.commands = new BukkitCommands(bluemap.getCommands());
		
		BukkitPlugin.instance = this;
	}
	
	@Override
	public void onEnable() {
		new MetricsLite(this);
		
		//save world so the level.dat is present on new worlds
		Logger.global.logInfo("Saving all worlds once, to make sure the level.dat is present...");
		for (World world : getServer().getWorlds()) {
			world.save();
		}
		
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
		final File normalizedWorldFolder = worldFolder.getCanonicalFile();

		Future<UUID> futureUUID;
		if (!Bukkit.isPrimaryThread()) {
			futureUUID = Bukkit.getScheduler().callSyncMethod(BukkitPlugin.getInstance(), () -> getUUIDForWorldSync(normalizedWorldFolder));
		} else {
			futureUUID = CompletableFuture.completedFuture(getUUIDForWorldSync(normalizedWorldFolder));
		}
		
		try {
			return futureUUID.get();
		} catch (InterruptedException e) {
			throw new IOException(e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			} else {
				throw new IOException(e);
			}
		}
	}
	
	private UUID getUUIDForWorldSync (File worldFolder) throws IOException {
		for (World world : getServer().getWorlds()) {
			if (worldFolder.equals(world.getWorldFolder().getCanonicalFile())) return world.getUID();
		}
		
		throw new IOException("There is no world with this folder loaded: " + worldFolder.getPath());
	}

	@Override
	public File getConfigFolder() {
		return getDataFolder();
	}
	
	public Plugin getBlueMap() {
		return bluemap;
	}

	public static BukkitPlugin getInstance() {
		return instance;
	}
	
}
