package de.bluecolored.bluemap.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import de.bluecolored.bluemap.common.plugin.Plugin;

public class Commands implements CommandExecutor {

	private BukkitPlugin plugin;
	private Plugin bluemap; 
	
	public Commands(BukkitPlugin plugin) {
		this.plugin = plugin;
		this.bluemap = plugin.getBlueMap();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		
		return true;
	}

}
