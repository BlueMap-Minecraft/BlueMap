package de.bluecolored.bluemap.bukkit;

import org.bukkit.plugin.java.JavaPlugin;

public class BukkitPlugin extends JavaPlugin {

	private static BukkitPlugin instance;
	
	@Override
	public void onEnable() {
		
	}
	
	@Override
	public void onDisable() {
		
	}
	
	public static BukkitPlugin getInstance() {
		return instance;
	}

}
