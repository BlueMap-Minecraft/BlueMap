package de.bluecolored.bluemap.forge;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import de.bluecolored.bluemap.common.plugin.Commands;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.plugin.text.TextColor;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.server.permission.PermissionAPI;

public class ForgeCommands {

	private ForgeMod mod;
	private Plugin bluemap;
	private Commands commands;
	
	public ForgeCommands(ForgeMod mod, Plugin bluemap) {
		this.mod = mod;
		this.bluemap = bluemap;
		this.commands = bluemap.getCommands();
	}
	
	public void registerCommands(CommandDispatcher<CommandSource> dispatcher) {
		
		LiteralArgumentBuilder<CommandSource> base = literal("bluemap");
		
		base.executes(c -> {
			if (!checkPermission(c, "bluemap.status")) return 0;
			
			commands.executeRootCommand(new ForgeCommandSource(c.getSource()));
			return 1;
		});
		
		base.then(literal("reload")).executes(c -> {
			if (!checkPermission(c, "bluemap.reload")) return 0;
			
			commands.executeReloadCommand(new ForgeCommandSource(c.getSource()));
			return 1;
		});
		
		base.then(literal("pause")).executes(c -> {
			if (!checkPermission(c, "bluemap.pause")) return 0;
			
			commands.executePauseCommand(new ForgeCommandSource(c.getSource()));
			return 1;
		});
		
		base.then(literal("resume")).executes(c -> {
			if (!checkPermission(c, "bluemap.resume")) return 0;
			
			commands.executeResumeCommand(new ForgeCommandSource(c.getSource()));
			return 1;
		});
		
		Command<CommandSource> renderCommand = c -> {
			if (!checkPermission(c, "bluemap.render")) return 0;
			
			String worldName = null;
			try {
				c.getArgument("world", String.class);
			} catch (IllegalArgumentException ex) {}

			int blockRadius = -1;
			try {
				c.getArgument("block-radius", Integer.class);
			} catch (IllegalArgumentException ex) {}
			
			PlayerEntity player = null;
			try {
				player = c.getSource().asPlayer();
			} catch (CommandSyntaxException ex) {}
			
			if (player == null) {
				if (worldName == null) throw new SimpleCommandExceptionType(new LiteralMessage("There is no world with this name: " + worldName)).create();
			} else {
				
			}
			
			return 1;
		};
		
		base.then(literal("render")).executes(renderCommand);
		base.then(literal("render")).then(argument("world", StringArgumentType.word())).executes(renderCommand);
		base.then(literal("render")).then(argument("block-radius", IntegerArgumentType.integer(0))).executes(renderCommand);
		base.then(literal("render")).then(argument("world", StringArgumentType.word())).then(argument("block-radius", IntegerArgumentType.integer(0))).executes(renderCommand);
		
		dispatcher.register(base);
	}
	
	private boolean checkPermission(CommandContext<CommandSource> command, String permission) {
		ForgeCommandSource cs = new ForgeCommandSource(command.getSource());
		
		boolean hasPermission = false;
		try {
			if (PermissionAPI.hasPermission(command.getSource().asPlayer(), permission)) {
				hasPermission = true;
			}
		} catch (CommandSyntaxException ex) {
			if (command.getSource().hasPermissionLevel(2)) {
				hasPermission = true;
			}
		}
		
		if (!hasPermission) {
			cs.sendMessage(Text.of(TextColor.RED, "You don't have the permissions to use this command!"));
		}
		
		return hasPermission;
	}
	
	public static LiteralArgumentBuilder<CommandSource> literal(String name){
		return LiteralArgumentBuilder.<CommandSource>literal(name);
	}
	
	public static <S extends CommandSource, T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type){
		return RequiredArgumentBuilder.<S, T>argument(name, type);
	}
	
}
