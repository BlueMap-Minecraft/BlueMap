package de.bluecolored.bluemap.bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.common.plugin.Commands;
import de.bluecolored.bluemap.common.plugin.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.plugin.text.TextColor;

public class BukkitCommands implements CommandExecutor {
	
	private Commands bluemapCommands;
	
	private Collection<Command> commands;
	
	public BukkitCommands(Commands commands) {
		this.bluemapCommands = commands;
		this.commands = new ArrayList<>();
		initCommands();
	}
	
	private void initCommands() {
		
		commands.add(new Command("bluemap.status") {
			@Override
			public boolean execute(CommandSender sender, CommandSource source, String[] args) {
				if (args.length != 0) return false;
				
				bluemapCommands.executeRootCommand(source);
				return true;
			}
		});
		
		commands.add(new Command("bluemap.reload", "reload") {
			@Override
			public boolean execute(CommandSender sender, CommandSource source, String[] args) {
				if (args.length != 0) return false;
				
				bluemapCommands.executeReloadCommand(source);
				return true;
			}
		});
		
		commands.add(new Command("bluemap.pause", "pause") {
			@Override
			public boolean execute(CommandSender sender, CommandSource source, String[] args) {
				if (args.length != 0) return false;
				
				bluemapCommands.executePauseCommand(source);
				return true;
			}
		});
		
		commands.add(new Command("bluemap.resume", "resume") {
			@Override
			public boolean execute(CommandSender sender, CommandSource source, String[] args) {
				if (args.length != 0) return false;
				
				bluemapCommands.executeResumeCommand(source);
				return true;
			}
		});
		
		commands.add(new Command("bluemap.rendertask.create.world", "render") {
			@Override
			public boolean execute(CommandSender sender, CommandSource source, String[] args) {
				if (args.length > 1) return false;
				
				World world;
				if (args.length == 1) {
					world = Bukkit.getWorld(args[0]);
					if (world == null) {
						source.sendMessage(Text.of(TextColor.RED, "There is no world named '" + args[0] + "'!"));
						return true;
					}
				} else {
					if (sender instanceof Player) {
						Player player = (Player) sender;
						world = player.getWorld();
					} else {
						source.sendMessage(Text.of(TextColor.RED, "Since you are not a player, you have to specify a world!"));
						return true;
					}
				}
				
				bluemapCommands.executeRenderWorldCommand(source, world.getUID());
				return true;
			}
		});
		
		commands.add(new Command("bluemap.rendertask.prioritize", "render", "prioritize") {
			@Override
			public boolean execute(CommandSender sender, CommandSource source, String[] args) {
				if (args.length != 1) return false;
				
				try {
					UUID uuid = UUID.fromString(args[0]);
					bluemapCommands.executePrioritizeRenderTaskCommand(source, uuid);
					return true;
				} catch (IllegalArgumentException ex) {
					source.sendMessage(Text.of(TextColor.RED, "'" + args[0] + "' is not a valid UUID!"));
					return true;
				}
			}
		});
		
		commands.add(new Command("bluemap.rendertask.remove", "render", "remove") {
			@Override
			public boolean execute(CommandSender sender, CommandSource source, String[] args) {
				if (args.length != 1) return false;
				
				try {
					UUID uuid = UUID.fromString(args[0]);
					bluemapCommands.executeRemoveRenderTaskCommand(source, uuid);
					return true;
				} catch (IllegalArgumentException ex) {
					source.sendMessage(Text.of(TextColor.RED, "'" + args[0] + "' is not a valid UUID!"));
					return true;
				}
			}
		});
		
		commands.add(new Command("bluemap.debug", "debug") {
			@Override
			public boolean execute(CommandSender sender, CommandSource source, String[] args) {
				if (!(sender instanceof Player)) {
					source.sendMessage(Text.of(TextColor.RED, "You have to be a player to use this command!"));
					return true;
				}
				
				Player player = (Player) sender;
				UUID world = player.getWorld().getUID();
				Vector3i pos = new Vector3i(
						player.getLocation().getBlockX(),
						player.getLocation().getBlockY(),
						player.getLocation().getBlockZ()
						);
				
				bluemapCommands.executeDebugCommand(source, world, pos);
				return true;
			}
		});
		
	}

	@Override
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command bukkitCommand, String label, String[] args) {
		int max = -1;
		Command maxCommand = null;
		for (Command command : commands) {
			int matchSize = command.matches(args);
			if (matchSize > max) {
				maxCommand = command;
				max = matchSize;
			}
		}
		
		if (maxCommand == null) return false;

		BukkitCommandSource source = new BukkitCommandSource(sender);
		
		if (!maxCommand.checkPermission(sender)) {
			source.sendMessage(Text.of(TextColor.RED, "You don't have permission to use this command!"));
			return true;
		}
		
		return maxCommand.execute(sender, source, Arrays.copyOfRange(args, max, args.length));
	}

	private abstract class Command {
		
		private String[] command;
		private String permission;
		
		public Command(String permission, String... command) {
			this.command = command;
		}
		
		public abstract boolean execute(CommandSender sender, CommandSource source, String[] args);
		
		public int matches(String[] args) {
			if (args.length < command.length) return -1;
			
			for (int i = 0; i < command.length; i++) {
				if (!args[i].equalsIgnoreCase(command[i])) return -1;
			}
			
			return command.length;
		}
		
		public boolean checkPermission(CommandSender sender) {
			if (sender.isOp()) return true;
			return sender.hasPermission(permission);
		}
		
	}
	
}
