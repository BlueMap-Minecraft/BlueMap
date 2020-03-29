package de.bluecolored.bluemap.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.bluecolored.bluemap.common.plugin.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.plugin.text.Text;

public class BukkitCommandSource implements CommandSource {

	private CommandSender delegate;
	
	public BukkitCommandSource(CommandSender delegate) {
		this.delegate = delegate;
	}

	@Override
	public void sendMessage(Text text) {
		Bukkit.getScheduler().runTask(BukkitPlugin.getInstance(), () -> {
			if (delegate instanceof Player) {
				Player player = (Player) delegate;
				
				//kinda hacky but works 
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + player.getName() + " " + text.toJSONString());
				return;
			}
			
			delegate.sendMessage(text.toPlainString());
		});
	}
	
}
