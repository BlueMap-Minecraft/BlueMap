package de.bluecolored.bluemap.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import de.bluecolored.bluemap.common.plugin.Commands;

public class BukkitCommands implements CommandExecutor {
	
	private Commands commands;
	
	public BukkitCommands(Commands commands) {
		this.commands = commands;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		commands.executeRootCommand(new BukkitCommandSource(sender));
		
		return true;
	}

}
