/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.bluecolored.bluemap.forge;

import java.io.IOException;
import java.util.UUID;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import de.bluecolored.bluemap.common.plugin.Commands;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.plugin.text.TextColor;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

public class ForgeCommands {

	private ForgeMod mod;
	private Commands commands;
	
	public ForgeCommands(ForgeMod mod, Plugin bluemap) {
		this.mod = mod;
		this.commands = bluemap.getCommands();
	}
	
	public void registerCommands(CommandDispatcher<CommandSource> dispatcher) {
		LiteralArgumentBuilder<CommandSource> base = literal("bluemap");

		PermissionAPI.registerNode("bluemap.status", DefaultPermissionLevel.OP, "Permission for using /bluemap");
		base.executes(c -> {
			if (!checkPermission(c, "bluemap.status")) return 0;
			
			commands.executeRootCommand(new ForgeCommandSource(c.getSource()));
			return 1;
		});

		PermissionAPI.registerNode("bluemap.reload", DefaultPermissionLevel.OP, "Permission for using /bluemap reload");
		base.then(literal("reload").executes(c -> {
			if (!checkPermission(c, "bluemap.reload")) return 0;
			
			commands.executeReloadCommand(new ForgeCommandSource(c.getSource()));
			return 1;
		}));

		PermissionAPI.registerNode("bluemap.pause", DefaultPermissionLevel.OP, "Permission for using /bluemap pause");
		base.then(literal("pause").executes(c -> {
			if (!checkPermission(c, "bluemap.pause")) return 0;
			
			commands.executePauseCommand(new ForgeCommandSource(c.getSource()));
			return 1;
		}));

		PermissionAPI.registerNode("bluemap.resume", DefaultPermissionLevel.OP, "Permission for using /bluemap resume");
		base.then(literal("resume").executes(c -> {
			if (!checkPermission(c, "bluemap.resume")) return 0;
			
			commands.executeResumeCommand(new ForgeCommandSource(c.getSource()));
			return 1;
		}));

		PermissionAPI.registerNode("bluemap.render", DefaultPermissionLevel.OP, "Permission for using /bluemap render");
		Command<CommandSource> renderCommand = c -> {
			if (!checkPermission(c, "bluemap.render")) return 0;
			
			String worldName = null;
			try {
				worldName = c.getArgument("world", String.class);
			} catch (IllegalArgumentException ex) {}

			int blockRadius = -1;
			try {
				blockRadius = c.getArgument("block-radius", Integer.class);
			} catch (IllegalArgumentException ex) {}
			
			PlayerEntity player = null;
			try {
				player = c.getSource().asPlayer();
			} catch (CommandSyntaxException ex) {}
			
			if (player == null && blockRadius != -1) {
				throw new SimpleCommandExceptionType(new LiteralMessage("You can only use a block-radius if you are a player!")).create();
			}
			
			if (worldName == null) {
				if (player == null) throw new SimpleCommandExceptionType(new LiteralMessage("You need to define a world! (/bluemap render <world>)")).create();
				Vector2i center = new Vector2i(player.getPosition().getX(), player.getPosition().getZ());

				UUID world;
				try {
					world = mod.getUUIDForWorld((ServerWorld) player.getEntityWorld());
				} catch (IOException ex) {
					throw new SimpleCommandExceptionType(new LiteralMessage("Could not detect the world you are currently in, try to define a world using /bluemap render <world>")).create();
				}

				return commands.executeRenderWorldCommand(new ForgeCommandSource(c.getSource()), world, center, blockRadius) ? 1 : 0;
			} else {
				if (player == null) {
					return commands.executeRenderCommand(new ForgeCommandSource(c.getSource()), worldName) ? 1 : 0;
				} else {
					Vector2i center = new Vector2i(player.getPosition().getX(), player.getPosition().getZ());
					return commands.executeRenderCommand(new ForgeCommandSource(c.getSource()), worldName, center, blockRadius) ? 1 : 0;
				}
			}
			
		};

		base.then(literal("render")
			.executes(renderCommand)
			.then(argument("block-radius", IntegerArgumentType.integer(0)).executes(renderCommand))
			.then(argument("world", StringArgumentType.word())
				.executes(renderCommand)
				.then(argument("block-radius", IntegerArgumentType.integer(0))).executes(renderCommand)	
			)
			
			.then(literal("prioritize").then(argument("task-uuid", StringArgumentType.word()).executes(c -> {
				if (!checkPermission(c, "bluemap.render")) return 0;
				
				try {
					UUID taskUUID = UUID.fromString(c.getArgument("task-uuid", String.class));
					commands.executePrioritizeRenderTaskCommand(new ForgeCommandSource(c.getSource()), taskUUID);
					return 1;
				} catch (IllegalArgumentException ex) {
					throw new SimpleCommandExceptionType(new LiteralMessage("Invalid task-uuid!")).create(); 
				}
			})))
			
			.then(literal("remove").then(argument("task-uuid", StringArgumentType.word()).executes(c -> {
				if (!checkPermission(c, "bluemap.render")) return 0;
				
				try {
					UUID taskUUID = UUID.fromString(c.getArgument("task-uuid", String.class));
					commands.executeRemoveRenderTaskCommand(new ForgeCommandSource(c.getSource()), taskUUID);
					return 1;
				} catch (IllegalArgumentException ex) {
					throw new SimpleCommandExceptionType(new LiteralMessage("Invalid task-uuid!")).create(); 
				}
			})))
		);
		
		PermissionAPI.registerNode("bluemap.debug", DefaultPermissionLevel.OP, "Permission for using /bluemap debug");
		base.then(literal("debug").executes(c -> {
			if (!checkPermission(c, "bluemap.debug")) return 0;
			
			Entity entity = c.getSource().assertIsEntity();
			BlockPos mcPos = entity.getPosition();
			Vector3i pos = new Vector3i(mcPos.getX(), mcPos.getY(), mcPos.getZ()); 
			
			UUID world;
			try {
				world = mod.getUUIDForWorld((ServerWorld) entity.getEntityWorld());
			} catch (IOException e) {
				throw new SimpleCommandExceptionType(new LiteralMessage("Could not detect the world you are currently in!")).create();
			}
			
			commands.executeDebugCommand(new ForgeCommandSource(c.getSource()), world, pos);
			return 1;
		}));
		
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
			if (command.getSource().hasPermissionLevel(1)) {
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
