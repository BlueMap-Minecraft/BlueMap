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
package de.bluecolored.bluemap.common.plugin.commands;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.marker.MarkerAPI;
import de.bluecolored.bluemap.api.marker.MarkerSet;
import de.bluecolored.bluemap.api.marker.POIMarker;
import de.bluecolored.bluemap.common.MapType;
import de.bluecolored.bluemap.common.RenderTask;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.plugin.text.TextColor;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.mca.Chunk;
import de.bluecolored.bluemap.core.mca.ChunkAnvil112;
import de.bluecolored.bluemap.core.mca.MCAWorld;
import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.World;

public class Commands<S> {
	
	public static final String DEFAULT_MARKER_SET_ID = "markers"; 

	private final Plugin plugin;
	private final CommandDispatcher<S> dispatcher;
	private Function<S, CommandSource> commandSourceInterface;
	
	private CommandHelper helper;
	
	public Commands(Plugin plugin, CommandDispatcher<S> dispatcher, Function<S, CommandSource> commandSourceInterface) {
		this.plugin = plugin;
		this.dispatcher = dispatcher;
		this.commandSourceInterface = commandSourceInterface;
		
		this.helper = new CommandHelper(plugin);
		
		init();
	}
	
	public void init() {
		// commands
		LiteralCommandNode<S> baseCommand = 
				literal("bluemap")
				.requires(requirementsUnloaded("bluemap.status"))
				.executes(this::statusCommand)
				.build();
		
		LiteralCommandNode<S> reloadCommand = 
				literal("reload")
				.requires(requirementsUnloaded("bluemap.reload"))
				.executes(this::reloadCommand)
				.build();
		
		LiteralCommandNode<S> debugCommand = 
				literal("debug")
				.requires(requirements("bluemap.debug"))
				.executes(this::debugCommand)

				.then(argument("world", StringArgumentType.string()).suggests(new WorldSuggestionProvider<>(plugin))
						.then(argument("x", DoubleArgumentType.doubleArg())
								.then(argument("y", DoubleArgumentType.doubleArg())
										.then(argument("z", DoubleArgumentType.doubleArg())
												.executes(this::debugCommand)))))
				.build();
		
		LiteralCommandNode<S> pauseCommand = 
				literal("pause")
				.requires(requirements("bluemap.pause"))
				.executes(this::pauseCommand)
				.build();
		
		LiteralCommandNode<S> resumeCommand = 
				literal("resume")
				.requires(requirements("bluemap.resume"))
				.executes(this::resumeCommand)
				.build();
		
		LiteralCommandNode<S> renderCommand = 
				literal("render")
				.requires(requirements("bluemap.render"))
				.executes(this::renderCommand) // /bluemap render

				.then(argument("radius", IntegerArgumentType.integer())
						.executes(this::renderCommand)) // /bluemap render <radius>
				
				.then(argument("x", DoubleArgumentType.doubleArg())
						.then(argument("z", DoubleArgumentType.doubleArg())
								.then(argument("radius", IntegerArgumentType.integer())
										.executes(this::renderCommand)))) // /bluemap render <x> <z> <radius>
				
				.then(argument("world|map", StringArgumentType.string()).suggests(new WorldOrMapSuggestionProvider<>(plugin))
						.executes(this::renderCommand) // /bluemap render <world|map>
						
						.then(argument("x", DoubleArgumentType.doubleArg())
								.then(argument("z", DoubleArgumentType.doubleArg())
										.then(argument("radius", IntegerArgumentType.integer())
												.executes(this::renderCommand))))) // /bluemap render <world|map> <x> <z> <radius>
				.build();
		
		LiteralCommandNode<S> prioRenderCommand = 
				literal("prioritize")
				.requires(requirements("bluemap.render"))
				.then(argument("uuid", StringArgumentType.string())
						.executes(this::prioritizeRenderTaskCommand))
				.build();
		
		LiteralCommandNode<S> cancelRenderCommand = 
				literal("cancel")
				.requires(requirements("bluemap.render"))
				.then(argument("uuid", StringArgumentType.string())
						.executes(this::cancelRenderTaskCommand))
				.build();
		
		LiteralCommandNode<S> worldsCommand = 
				literal("worlds")
				.requires(requirements("bluemap.status"))
				.executes(this::worldsCommand)
				.build();
		
		LiteralCommandNode<S> mapsCommand = 
				literal("maps")
				.requires(requirements("bluemap.status"))
				.executes(this::mapsCommand)
				.build();
		
		LiteralCommandNode<S> markerCommand = 
				literal("marker")
				.requires(requirements("bluemap.marker"))
				.build();
		
		LiteralCommandNode<S> createMarkerCommand = 
				literal("create")
				.requires(requirements("bluemap.marker"))
				.then(argument("id", StringArgumentType.word())
						.then(argument("map", StringArgumentType.string()).suggests(new MapSuggestionProvider<>(plugin))
						
							.then(argument("label", StringArgumentType.string())
									.executes(this::createMarkerCommand))
							
							.then(argument("x", DoubleArgumentType.doubleArg())
									.then(argument("y", DoubleArgumentType.doubleArg())
											.then(argument("z", DoubleArgumentType.doubleArg())
													.then(argument("label", StringArgumentType.string())
															.executes(this::createMarkerCommand)))))))
				.build();
		
		LiteralCommandNode<S> removeMarkerCommand = 
				literal("remove")
				.requires(requirements("bluemap.marker"))
				.then(argument("id", StringArgumentType.word()).suggests(MarkerIdSuggestionProvider.getInstance())
						.executes(this::removeMarkerCommand))
				.build();
		
		// command tree
		dispatcher.getRoot().addChild(baseCommand);
		baseCommand.addChild(reloadCommand);
		baseCommand.addChild(debugCommand);
		baseCommand.addChild(pauseCommand);
		baseCommand.addChild(resumeCommand);
		baseCommand.addChild(renderCommand);
		renderCommand.addChild(prioRenderCommand);
		renderCommand.addChild(cancelRenderCommand);
		baseCommand.addChild(worldsCommand);
		baseCommand.addChild(mapsCommand);
		baseCommand.addChild(markerCommand);
		markerCommand.addChild(createMarkerCommand);
		markerCommand.addChild(removeMarkerCommand);
	}
	
	private Predicate<S> requirements(String permission){
		return s -> {
			CommandSource source = commandSourceInterface.apply(s);
			return plugin.isLoaded() && source.hasPermission(permission);
		};
	}
	
	private Predicate<S> requirementsUnloaded(String permission){
		return s -> {
			CommandSource source = commandSourceInterface.apply(s);
			return source.hasPermission(permission);
		};
	}
	
	private LiteralArgumentBuilder<S> literal(String name){
		return LiteralArgumentBuilder.<S>literal(name);
	}
	
	private <T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type){
		return RequiredArgumentBuilder.<S, T>argument(name, type);
	}
	
	private <T> Optional<T> getOptionalArgument(CommandContext<S> context, String argumentName, Class<T> type) {
		try {
			return Optional.of(context.getArgument(argumentName, type));
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}
	
	private Optional<World> parseWorld(String worldName) {
		for (World world : plugin.getWorlds()) {
			if (world.getName().equalsIgnoreCase(worldName)) {
				return Optional.of(world);
			}
		}
		
		return Optional.empty();
	}
	
	private Optional<MapType> parseMap(String mapId) {
		for (MapType map : plugin.getMapTypes()) {
			if (map.getId().equalsIgnoreCase(mapId)) {
				return Optional.of(map);
			}
		}
		
		return Optional.empty();
	}
	
	private Optional<UUID> parseUUID(String uuidString) {
		try {
			return Optional.of(UUID.fromString(uuidString));
		} catch (IllegalArgumentException ex) {
			return Optional.empty();			
		}
	}
	
	
	// --- COMMANDS ---
	
	public int statusCommand(CommandContext<S> context) {
		CommandSource source = commandSourceInterface.apply(context.getSource());
		
		if (!plugin.isLoaded()) {
			source.sendMessage(Text.of(TextColor.RED, "BlueMap is not loaded! Try /bluemap reload"));
			return 0;
		}
		
		source.sendMessages(helper.createStatusMessage());
		return 1;
	}
	
	public int reloadCommand(CommandContext<S> context) {
		CommandSource source = commandSourceInterface.apply(context.getSource());
		
		source.sendMessage(Text.of(TextColor.GOLD, "Reloading BlueMap..."));
		
		new Thread(() -> {
			try {
				plugin.reload();
				
				if (plugin.isLoaded()) {
					source.sendMessage(Text.of(TextColor.GREEN, "BlueMap reloaded!"));
				} else {
					source.sendMessage(Text.of(TextColor.RED, "Could not load BlueMap! See the console for details!"));
				}

			} catch (Exception ex) {
				Logger.global.logError("Failed to reload BlueMap!", ex);
				
				source.sendMessage(Text.of(TextColor.RED, "There was an error reloading BlueMap! See the console for details!"));
			}
		}).start();
		return 1;
	}

	public int debugCommand(CommandContext<S> context) throws CommandSyntaxException {
		final CommandSource source = commandSourceInterface.apply(context.getSource());
		
		// parse arguments
		Optional<String> worldName = getOptionalArgument(context, "world", String.class);
		Optional<Double> x = getOptionalArgument(context, "x", Double.class);
		Optional<Double> y = getOptionalArgument(context, "y", Double.class);
		Optional<Double> z = getOptionalArgument(context, "z", Double.class);
		
		final World world;
		final Vector3d position;
		
		if (worldName.isPresent() && x.isPresent() && y.isPresent() && z.isPresent()) {
			world = parseWorld(worldName.get()).orElse(null);
			position = new Vector3d(x.get(), y.get(), z.get());
			
			if (world == null) {
				source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.worldHelperHover(), " with this name: ", TextColor.WHITE, worldName.get()));
				return 0;
			}
		} else {
			world = source.getWorld().orElse(null);
			position = source.getPosition().orElse(null);
			
			if (world == null || position == null) {
				source.sendMessage(Text.of(TextColor.RED, "Can't detect a location from this command-source, you'll have to define a world and position!"));
				return 0;
			}
		}
		
		new Thread(() -> {
			// collect and output debug info
			Vector3i blockPos = position.floor().toInt();
			Block block = world.getBlock(blockPos);
			Block blockBelow = world.getBlock(blockPos.add(0, -1, 0));
			
			String blockIdMeta = "";
			String blockBelowIdMeta = "";
			
			if (world instanceof MCAWorld) {
				Chunk chunk = ((MCAWorld) world).getChunk(MCAWorld.blockToChunk(blockPos));
				if (chunk instanceof ChunkAnvil112) {
					blockIdMeta = " (" + ((ChunkAnvil112) chunk).getBlockIdMeta(blockPos) + ")";
					blockBelowIdMeta = " (" + ((ChunkAnvil112) chunk).getBlockIdMeta(blockPos.add(0, -1, 0)) + ")";
				}
			}
			
			source.sendMessages(Lists.newArrayList(
					Text.of(TextColor.GOLD, "Block at you: ", TextColor.WHITE, block, TextColor.GRAY, blockIdMeta),
					Text.of(TextColor.GOLD, "Block below you: ", TextColor.WHITE, blockBelow, TextColor.GRAY, blockBelowIdMeta)
				));
		}).start();
		
		return 1;
	}
	
	public int pauseCommand(CommandContext<S> context) {
		CommandSource source = commandSourceInterface.apply(context.getSource());
		
		if (plugin.getRenderManager().isRunning()) {
			plugin.getRenderManager().stop();
			source.sendMessage(Text.of(TextColor.GREEN, "BlueMap rendering paused!"));
			return 1;
		} else {
			source.sendMessage(Text.of(TextColor.RED, "BlueMap rendering are already paused!"));
			return 0;
		}
	}
	
	public int resumeCommand(CommandContext<S> context) {
		CommandSource source = commandSourceInterface.apply(context.getSource());
		
		if (!plugin.getRenderManager().isRunning()) {
			plugin.getRenderManager().start();
			source.sendMessage(Text.of(TextColor.GREEN, "BlueMap renders resumed!"));
			return 1;
		} else {
			source.sendMessage(Text.of(TextColor.RED, "BlueMap renders are already running!"));
			return 0;
		}
	}

	public int renderCommand(CommandContext<S> context) {
		final CommandSource source = commandSourceInterface.apply(context.getSource());
		
		// parse world/map argument
		Optional<String> worldOrMap = getOptionalArgument(context, "world|map", String.class);
		
		final World world;
		final MapType map;
		if (worldOrMap.isPresent()) {
			world = parseWorld(worldOrMap.get()).orElse(null);
			
			if (world == null) {
				map = parseMap(worldOrMap.get()).orElse(null);
				
				if (map == null) {
					source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.worldHelperHover(), " or ", helper.mapHelperHover(), " with this name: ", TextColor.WHITE, worldOrMap.get()));
					return 0;
				}
			} else {
				map = null;
			}
		} else {
			world = source.getWorld().orElse(null);
			map = null;
			
			if (world == null) {
				source.sendMessage(Text.of(TextColor.RED, "Can't detect a world from this command-source, you'll have to define a world or a map to render!").setHoverText(Text.of(TextColor.GRAY, "/bluemap render <world|map>")));
				return 0;
			}
		}

		// parse radius and center arguments
		final int radius = getOptionalArgument(context, "radius", Integer.class).orElse(-1);
		final Vector2i center;
		if (radius >= 0) {
			Optional<Double> x = getOptionalArgument(context, "x", Double.class);
			Optional<Double> z = getOptionalArgument(context, "z", Double.class);
			
			if (x.isPresent() && z.isPresent()) {
				center = new Vector2i(x.get(), z.get());
			} else {
				Vector3d position = source.getPosition().orElse(null);
				if (position == null) {
					source.sendMessage(Text.of(TextColor.RED, "Can't detect a position from this command-source, you'll have to define x,z coordinates to render with a radius!").setHoverText(Text.of(TextColor.GRAY, "/bluemap render <x> <z> " + radius)));
					return 0;
				}
				
				center = position.toVector2(true).floor().toInt();
			}
		} else {
			center = null;
		}
		
		// execute render
		new Thread(() -> {
			if (world != null) {
				helper.createWorldRenderTask(source, world, center, radius);
			} else {
				helper.createMapRenderTask(source, map, center, radius);
			}
		}).start();
		
		return 1;
	}
	
	public int prioritizeRenderTaskCommand(CommandContext<S> context) {
		CommandSource source = commandSourceInterface.apply(context.getSource());
		
		String uuidString = context.getArgument("uuid", String.class);
		Optional<UUID> taskUUID = parseUUID(uuidString);
		if (!taskUUID.isPresent()) {
			source.sendMessage(Text.of(TextColor.RED, "Not a valid UUID: " + uuidString));
			return 0;
		}
		
		for (RenderTask task : plugin.getRenderManager().getRenderTasks()) {
			if (task.getUuid().equals(taskUUID.get())) {
				plugin.getRenderManager().prioritizeRenderTask(task);

				source.sendMessages(helper.createStatusMessage());
				return 1;
			}
		}

		source.sendMessage(Text.of(TextColor.RED, "There is no render-task with this UUID: " + uuidString));
		return 0;
	}
	
	public int cancelRenderTaskCommand(CommandContext<S> context) {
		CommandSource source = commandSourceInterface.apply(context.getSource());
		
		String uuidString = context.getArgument("uuid", String.class);
		Optional<UUID> taskUUID = parseUUID(uuidString);
		if (!taskUUID.isPresent()) {
			source.sendMessage(Text.of(TextColor.RED, "Not a valid UUID: " + uuidString));
			return 0;
		}
		
		for (RenderTask task : plugin.getRenderManager().getRenderTasks()) {
			if (task.getUuid().equals(taskUUID.get())) {
				plugin.getRenderManager().removeRenderTask(task);

				source.sendMessages(helper.createStatusMessage());
				return 1;
			}
		}

		source.sendMessage(Text.of(TextColor.RED, "There is no render-task with this UUID: " + uuidString));
		return 0;
	}
	
	public int worldsCommand(CommandContext<S> context) {
		CommandSource source = commandSourceInterface.apply(context.getSource());
		
		source.sendMessage(Text.of(TextColor.BLUE, "Worlds loaded by BlueMap:"));
		for (World world : plugin.getWorlds()) {
			source.sendMessage(Text.of(TextColor.GRAY, " - ", TextColor.WHITE, world.getName()).setHoverText(Text.of(TextColor.GRAY, world.getUUID())));
		}
		
		return 1;
	}
	
	public int mapsCommand(CommandContext<S> context) {
		CommandSource source = commandSourceInterface.apply(context.getSource());
		
		source.sendMessage(Text.of(TextColor.BLUE, "Maps loaded by BlueMap:"));
		for (MapType map : plugin.getMapTypes()) {
			source.sendMessage(Text.of(TextColor.GRAY, " - ", TextColor.WHITE, map.getId(), TextColor.GRAY, " (" + map.getName() + ")").setHoverText(Text.of(TextColor.WHITE, "World: ", TextColor.GRAY, map.getWorld().getName())));
		}
		
		return 1;
	}
	
	public int createMarkerCommand(CommandContext<S> context) {
		CommandSource source = commandSourceInterface.apply(context.getSource());
		
		String markerId = context.getArgument("id", String.class);
		String markerLabel = context.getArgument("label", String.class)
				.replace("<", "&lt;")
				.replace(">", "&gt;");  //no html via commands
		
		// parse world/map argument
		String mapString = context.getArgument("map", String.class);
		MapType map = parseMap(mapString).orElse(null);
		
		if (map == null) {
			source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.mapHelperHover(), " with this name: ", TextColor.WHITE, mapString));
			return 0;
		}
		
		// parse position
		Optional<Double> x = getOptionalArgument(context, "x", Double.class);
		Optional<Double> y = getOptionalArgument(context, "y", Double.class);
		Optional<Double> z = getOptionalArgument(context, "z", Double.class);
		
		Vector3d position;
		
		if (x.isPresent() && y.isPresent() && z.isPresent()) {
			position = new Vector3d(x.get(), y.get(), z.get());
		} else {
			position = source.getPosition().orElse(null);
			
			if (position == null) {
				source.sendMessage(Text.of(TextColor.RED, "Can't detect a position from this command-source, you'll have to define the x,y,z coordinates for the marker!").setHoverText(Text.of(TextColor.GRAY, "/bluemap marker create " + markerId + " " + "[world|map] <x> <y> <z> <label>")));
				return 0;
			}
		}
		
		// get api
		BlueMapAPI api = BlueMapAPI.getInstance().orElse(null);
		if (api == null) {
			source.sendMessage(Text.of(TextColor.RED, "MarkerAPI is not available, try ", TextColor.GRAY, "/bluemap reload"));
			return 0;
		}
		
		// resolve api-map
		Optional<BlueMapMap> apiMap = api.getMap(map.getId());
		if (!apiMap.isPresent()) {
			source.sendMessage(Text.of(TextColor.RED, "Failed to get map from API, try ", TextColor.GRAY, "/bluemap reload"));
			return 0;
		}
		
		// add marker
		try {
			MarkerAPI markerApi = api.getMarkerAPI();
			
			MarkerSet set = markerApi.getMarkerSet(DEFAULT_MARKER_SET_ID).orElse(null);
			if (set == null) {
				set = markerApi.createMarkerSet(DEFAULT_MARKER_SET_ID);
				set.setLabel("Markers");
			}
			
			if (set.getMarker(markerId).isPresent()) {
				source.sendMessage(Text.of(TextColor.RED, "There already is a marker with this id: ", TextColor.WHITE, markerId));
				return 0;
			}
			
			POIMarker marker = set.createPOIMarker(markerId, apiMap.get(), position);
			marker.setLabel(markerLabel);
			
			markerApi.save();
			MarkerIdSuggestionProvider.getInstance().forceUpdate();
		} catch (IOException e) {
			source.sendMessage(Text.of(TextColor.RED, "There was an error trying to add the marker, please check the console for details!"));
			Logger.global.logError("Exception trying to add a marker!", e);
		}

		source.sendMessage(Text.of(TextColor.GREEN, "Marker added!"));
		return 1;
	}
	
	public int removeMarkerCommand(CommandContext<S> context) {
		CommandSource source = commandSourceInterface.apply(context.getSource());

		String markerId = context.getArgument("id", String.class);
		
		BlueMapAPI api = BlueMapAPI.getInstance().orElse(null);
		if (api == null) {
			source.sendMessage(Text.of(TextColor.RED, "MarkerAPI is not available, try ", TextColor.GRAY, "/bluemap reload"));
			return 0;
		}
		
		try {
			MarkerAPI markerApi = api.getMarkerAPI();
			
			MarkerSet set = markerApi.createMarkerSet("markers");
			if (!set.removeMarker(markerId)) {
				source.sendMessage(Text.of(TextColor.RED, "There is no marker with this id: ", TextColor.WHITE, markerId));
			}
			
			markerApi.save();
			MarkerIdSuggestionProvider.getInstance().forceUpdate();
		} catch (IOException e) {
			source.sendMessage(Text.of(TextColor.RED, "There was an error trying to remove the marker, please check the console for details!"));
			Logger.global.logError("Exception trying to remove a marker!", e);
		}

		source.sendMessage(Text.of(TextColor.GREEN, "Marker removed!"));
		return 1;
	}
	
}
