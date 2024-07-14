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

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.PluginState;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.plugin.text.TextColor;
import de.bluecolored.bluemap.common.plugin.text.TextFormat;
import de.bluecolored.bluemap.common.rendermanager.*;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.common.debug.StateDumper;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.map.renderstate.TileInfoRegion;
import de.bluecolored.bluemap.core.map.renderstate.TileState;
import de.bluecolored.bluemap.core.storage.MapStorage;
import de.bluecolored.bluemap.core.storage.Storage;
import de.bluecolored.bluemap.core.util.Grid;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.ChunkConsumer;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.block.Block;
import de.bluecolored.bluemap.core.world.block.entity.BlockEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Commands<S> {

    private final Plugin plugin;
    private final CommandDispatcher<S> dispatcher;
    private final Function<S, CommandSource> commandSourceInterface;

    private final CommandHelper helper;

    public Commands(Plugin plugin, CommandDispatcher<S> dispatcher, Function<S, CommandSource> commandSourceInterface) {
        this.plugin = plugin;
        this.dispatcher = dispatcher;
        this.commandSourceInterface = commandSourceInterface;

        this.helper = new CommandHelper(plugin);

        init();
    }

    public void init() {
        // commands
        LiteralCommandNode<S> baseCommand = literal("bluemap")
                .requires(requirementsUnloaded("bluemap.status"))
                .executes(this::statusCommand)
                .build();

        LiteralCommandNode<S> versionCommand = literal("version")
                .requires(requirementsUnloaded("bluemap.version"))
                .executes(this::versionCommand)
                .build();

        LiteralCommandNode<S> helpCommand = literal("help")
                .requires(requirementsUnloaded("bluemap.help"))
                .executes(this::helpCommand)
                .build();

        LiteralCommandNode<S> reloadCommand = literal("reload")
                .requires(requirementsUnloaded("bluemap.reload"))
                .executes(context -> this.reloadCommand(context, false))

                .then(literal("light")
                        .executes(context -> this.reloadCommand(context, true)))

                .build();

        LiteralCommandNode<S> debugCommand = literal("debug")
                .requires(requirementsUnloaded("bluemap.debug"))

                .then(literal("block")
                        .requires(requirements("bluemap.debug"))
                        .executes(this::debugBlockCommand)

                        .then(argument("world", StringArgumentType.string()).suggests(new WorldSuggestionProvider<>(plugin))
                                .then(argument("x", DoubleArgumentType.doubleArg())
                                        .then(argument("y", DoubleArgumentType.doubleArg())
                                                .then(argument("z", DoubleArgumentType.doubleArg())
                                                        .executes(this::debugBlockCommand))))))

                .then(literal("map")
                        .requires(requirements("bluemap.debug"))
                        .then(argument("map", StringArgumentType.string()).suggests(new MapSuggestionProvider<>(plugin))
                                .executes(this::debugMapCommand)

                                .then(argument("x", IntegerArgumentType.integer())
                                        .then(argument("z", IntegerArgumentType.integer())
                                                .executes(this::debugMapCommand)))))

                .then(literal("flush")
                        .requires(requirements("bluemap.debug"))
                        .executes(this::debugFlushCommand)

                        .then(argument("world", StringArgumentType.string()).suggests(new WorldSuggestionProvider<>(plugin))
                                .executes(this::debugFlushCommand)))

                .then(literal("cache")
                        .requires(requirements("bluemap.debug"))
                        .executes(this::debugClearCacheCommand))


                .then(literal("dump")
                        .executes(this::debugDumpCommand))

                .build();

        LiteralCommandNode<S> stopCommand = literal("stop")
                .requires(requirements("bluemap.stop"))
                .executes(this::stopCommand)
                .build();

        LiteralCommandNode<S> startCommand = literal("start")
                .requires(requirements("bluemap.start"))
                .executes(this::startCommand)
                .build();

        LiteralCommandNode<S> freezeCommand = literal("freeze")
                .requires(requirements("bluemap.freeze"))
                .then(argument("map", StringArgumentType.string()).suggests(new MapSuggestionProvider<>(plugin))
                        .executes(this::freezeCommand))
                        .build();

        LiteralCommandNode<S> unfreezeCommand = literal("unfreeze")
                .requires(requirements("bluemap.freeze"))
                .then(argument("map", StringArgumentType.string()).suggests(new MapSuggestionProvider<>(plugin))
                        .executes(this::unfreezeCommand))
                .build();

        LiteralCommandNode<S> forceUpdateCommand =
                addRenderArguments(
                        literal("force-update")
                        .requires(requirements("bluemap.update.force")),
                        this::forceUpdateCommand
                ).build();

        LiteralCommandNode<S> fixEdgesCommand =
                addRenderArguments(
                        literal("fix-edges")
                                .requires(requirements("bluemap.update.force")),
                        this::fixEdgesCommand
                ).build();

        LiteralCommandNode<S> updateCommand =
                addRenderArguments(
                        literal("update")
                        .requires(requirements("bluemap.update")),
                        this::updateCommand
                ).build();

        LiteralCommandNode<S> purgeCommand = literal("purge")
                .requires(requirements("bluemap.purge"))
                .then(argument("map", StringArgumentType.string()).suggests(new MapSuggestionProvider<>(plugin))
                        .executes(this::purgeCommand))
                .build();

        LiteralCommandNode<S> cancelCommand = literal("cancel")
                .requires(requirements("bluemap.cancel"))
                .executes(this::cancelCommand)
                .then(argument("task-ref", StringArgumentType.string()).suggests(new TaskRefSuggestionProvider<>(helper))
                        .executes(this::cancelCommand))
                .build();

        LiteralCommandNode<S> worldsCommand = literal("worlds")
                .requires(requirements("bluemap.status"))
                .executes(this::worldsCommand)
                .build();

        LiteralCommandNode<S> mapsCommand = literal("maps")
                .requires(requirements("bluemap.status"))
                .executes(this::mapsCommand)
                .build();

        LiteralCommandNode<S> storagesCommand = literal("storages")
                .requires(requirements("bluemap.status"))
                .executes(this::storagesCommand)

                .then(argument("storage", StringArgumentType.string()).suggests(new StorageSuggestionProvider<>(plugin))
                        .executes(this::storagesInfoCommand)

                        .then(literal("delete")
                                .requires(requirements("bluemap.delete"))
                                .then(argument("map", StringArgumentType.string())
                                        .executes(this::storagesDeleteMapCommand))))

                .build();

        // command tree
        dispatcher.getRoot().addChild(baseCommand);
        baseCommand.addChild(versionCommand);
        baseCommand.addChild(helpCommand);
        baseCommand.addChild(reloadCommand);
        baseCommand.addChild(debugCommand);
        baseCommand.addChild(stopCommand);
        baseCommand.addChild(startCommand);
        baseCommand.addChild(freezeCommand);
        baseCommand.addChild(unfreezeCommand);
        baseCommand.addChild(forceUpdateCommand);
        baseCommand.addChild(fixEdgesCommand);
        baseCommand.addChild(updateCommand);
        baseCommand.addChild(cancelCommand);
        baseCommand.addChild(purgeCommand);
        baseCommand.addChild(worldsCommand);
        baseCommand.addChild(mapsCommand);
        baseCommand.addChild(storagesCommand);
    }

    private <B extends ArgumentBuilder<S, B>> B addRenderArguments(B builder, Command<S> command) {
        return builder
            .executes(command) // /bluemap render

            .then(argument("radius", IntegerArgumentType.integer())
                    .executes(command)) // /bluemap render <radius>

            .then(argument("x", DoubleArgumentType.doubleArg())
                    .then(argument("z", DoubleArgumentType.doubleArg())
                            .then(argument("radius", IntegerArgumentType.integer())
                                    .executes(command)))) // /bluemap render <x> <z> <radius>

            .then(argument("world|map", StringArgumentType.string()).suggests(new WorldOrMapSuggestionProvider<>(plugin))
                    .executes(command) // /bluemap render <world|map>

                    .then(argument("x", DoubleArgumentType.doubleArg())
                            .then(argument("z", DoubleArgumentType.doubleArg())
                                    .then(argument("radius", IntegerArgumentType.integer())
                                            .executes(command))))); // /bluemap render <world|map> <x> <z> <radius>
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
        return LiteralArgumentBuilder.literal(name);
    }

    private <T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type){
        return RequiredArgumentBuilder.argument(name, type);
    }

    private <T> Optional<T> getOptionalArgument(CommandContext<S> context, String argumentName, Class<T> type) {
        try {
            return Optional.of(context.getArgument(argumentName, type));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private Optional<World> parseWorld(String worldId) {
        for (var entry : plugin.getBlueMap().getWorlds().entrySet()) {
            if (entry.getKey().equals(worldId)) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }

    private Optional<BmMap> parseMap(String mapId) {
        for (BmMap map : plugin.getBlueMap().getMaps().values()) {
            if (map.getId().equals(mapId)) {
                return Optional.of(map);
            }
        }

        return Optional.empty();
    }


    // --- COMMANDS ---

    public int statusCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        if (!plugin.isLoaded()) {
            source.sendMessage(Text.of(TextColor.RED, "BlueMap is not loaded! Try /bluemap reload"));
            return 0;
        }

        new Thread(() -> {
            source.sendMessages(helper.createStatusMessage());
        }, "BlueMap-Plugin-StatusCommand").start();

        return 1;
    }

    public int versionCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        int renderThreadCount = 0;
        if (plugin.isLoaded()) {
            renderThreadCount = plugin.getRenderManager().getWorkerThreadCount();
        }

        String minecraftVersion = plugin.getServerInterface().getMinecraftVersion();

        source.sendMessage(Text.of(TextFormat.BOLD, TextColor.BLUE, "Version: ", TextColor.WHITE, BlueMap.VERSION));
        source.sendMessage(Text.of(TextColor.GRAY, "Commit: ", TextColor.WHITE, BlueMap.GIT_HASH));
        source.sendMessage(Text.of(TextColor.GRAY, "Implementation: ", TextColor.WHITE, plugin.getImplementationType()));
        source.sendMessage(Text.of(TextColor.GRAY, "Minecraft: ", TextColor.WHITE, minecraftVersion));
        source.sendMessage(Text.of(TextColor.GRAY, "Render-threads: ", TextColor.WHITE, renderThreadCount));
        source.sendMessage(Text.of(TextColor.GRAY, "Available processors: ", TextColor.WHITE, Runtime.getRuntime().availableProcessors()));
        source.sendMessage(Text.of(TextColor.GRAY, "Available memory: ", TextColor.WHITE, (Runtime.getRuntime().maxMemory() / 1024L / 1024L) + " MiB"));

        String clipboardValue =
                "Version: " + BlueMap.VERSION + "\n" +
                "Commit: " + BlueMap.GIT_HASH + "\n" +
                "Implementation: " + plugin.getImplementationType() + "\n" +
                "Minecraft: " + minecraftVersion + "\n" +
                "Render-threads: " + renderThreadCount + "\n" +
                "Available processors: " + Runtime.getRuntime().availableProcessors() + "\n" +
                "Available memory: " + Runtime.getRuntime().maxMemory() / 1024L / 1024L + " MiB";
        source.sendMessage(Text.of(TextColor.DARK_GRAY, "[copy to clipboard]")
                .setClickAction(Text.ClickAction.COPY_TO_CLIPBOARD, clipboardValue)
                .setHoverText(Text.of(TextColor.GRAY, "click to copy the above text .. ", TextFormat.ITALIC, TextColor.GRAY, "duh!")));

        return 1;
    }

    public int helpCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        source.sendMessage(Text.of(TextColor.BLUE, "BlueMap Commands:"));
        for (String usage : dispatcher.getAllUsage(dispatcher.getRoot().getChild("bluemap"), context.getSource(), true)) {
            Text usageText = Text.of(TextColor.GREEN, "/bluemap");

            String[] arguments = usage.split(" ");
            for (String arg : arguments) {
                if (arg.isEmpty()) continue;
                if (arg.charAt(0) == '<' && arg.charAt(arg.length() - 1) == '>') {
                    usageText.addChild(Text.of(TextColor.GRAY, " " + arg));
                } else {
                    usageText.addChild(Text.of(TextColor.WHITE, " " + arg));
                }
            }

            source.sendMessage(usageText);
        }

        source.sendMessage(
                Text.of(TextColor.BLUE, "\nOpen this link to get a description for each command:\n")
                .addChild(Text.of(TextColor.GRAY, "https://bluecolo.red/bluemap-commands").setClickAction(Text.ClickAction.OPEN_URL, "https://bluecolo.red/bluemap-commands"))
                );

        return 1;
    }

    public int reloadCommand(CommandContext<S> context, boolean light) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        source.sendMessage(Text.of(TextColor.GOLD, "Reloading BlueMap..."));

        new Thread(() -> {
            try {
                if (light) {
                    plugin.lightReload();
                } else {
                    plugin.reload();
                }

                if (plugin.isLoaded()) {
                    source.sendMessage(Text.of(TextColor.GREEN, "BlueMap reloaded!"));
                } else {
                    source.sendMessage(Text.of(TextColor.RED, "Could not load BlueMap! See the console for details!"));
                }

            } catch (Exception ex) {
                Logger.global.logError("Failed to reload BlueMap!", ex);

                source.sendMessage(Text.of(TextColor.RED, "There was an error reloading BlueMap! See the console for details!"));
            }
        }, "BlueMap-Plugin-ReloadCommand").start();
        return 1;
    }

    public int debugClearCacheCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        for (World world : plugin.getBlueMap().getWorlds().values()) {
            world.invalidateChunkCache();
        }

        source.sendMessage(Text.of(TextColor.GREEN, "All caches cleared!"));
        return 1;
    }


    public int debugFlushCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        // parse arguments
        Optional<String> worldName = getOptionalArgument(context, "world", String.class);

        final World world;
        if (worldName.isPresent()) {
            world = parseWorld(worldName.get()).orElse(null);

            if (world == null) {
                source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.worldHelperHover(), " with this id: ", TextColor.WHITE, worldName.get()));
                return 0;
            }
        } else {
            world = source.getWorld().orElse(null);

            if (world == null) {
                source.sendMessage(Text.of(TextColor.RED, "Can't detect a location from this command-source, you'll have to define a world!"));
                return 0;
            }
        }

        new Thread(() -> {
            source.sendMessage(Text.of(TextColor.GOLD, "Saving world and flushing changes..."));
            try {
                if (plugin.flushWorldUpdates(world)) {
                    source.sendMessage(Text.of(TextColor.GREEN, "Successfully saved and flushed all changes."));
                } else {
                    source.sendMessage(Text.of(TextColor.RED, "This operation is not supported by this implementation (" + plugin.getImplementationType() + ")"));
                }
            } catch (IOException ex) {
                source.sendMessage(Text.of(TextColor.RED, "There was an unexpected exception trying to save the world. Please check the console for more details..."));
                Logger.global.logError("Unexpected exception trying to save the world!", ex);
            }
        }, "BlueMap-Plugin-DebugFlushCommand").start();

        return 1;
    }

    public int debugMapCommand(CommandContext<S> context) {
        final CommandSource source = commandSourceInterface.apply(context.getSource());

        // parse arguments
        String mapId = context.getArgument("map", String.class);
        Optional<Integer> x = getOptionalArgument(context, "x", Integer.class);
        Optional<Integer> z = getOptionalArgument(context, "z", Integer.class);

        final BmMap map = parseMap(mapId).orElse(null);
        if (map == null) {
            source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.mapHelperHover(), " with this id: ", TextColor.WHITE, mapId));
            return 0;
        }

        final Vector2i position;
        if (x.isPresent() && z.isPresent()) {
            position = new Vector2i(x.get(), z.get());
        } else {
            position = source.getPosition()
                    .map(v -> v.toVector2(true))
                    .map(Vector2d::floor)
                    .map(Vector2d::toInt)
                    .orElse(null);

            if (position == null) {
                source.sendMessage(Text.of(TextColor.RED, "Can't detect a location from this command-source, you'll have to define a position!"));
                return 0;
            }
        }

        new Thread(() -> {
            // collect and output debug info
            Grid chunkGrid = map.getWorld().getChunkGrid();
            Grid regionGrid = map.getWorld().getRegionGrid();
            Grid tileGrid = map.getHiresModelManager().getTileGrid();

            Vector2i regionPos = regionGrid.getCell(position);
            Vector2i chunkPos = chunkGrid.getCell(position);
            Vector2i tilePos = tileGrid.getCell(position);

            TileInfoRegion.TileInfo tileInfo = map.getMapTileState().get(tilePos.getX(), tilePos.getY());

            int lastChunkHash = map.getMapChunkState().get(chunkPos.getX(), chunkPos.getY());
            int currentChunkHash = 0;

            class FindHashConsumer implements ChunkConsumer.ListOnly {
                public int timestamp = 0;

                @Override
                public void accept(int chunkX, int chunkZ, int timestamp) {
                    if (chunkPos.getX() == chunkX && chunkPos.getY() == chunkZ)
                        this.timestamp = timestamp;
                }
            }

            try {
                FindHashConsumer findHashConsumer = new FindHashConsumer();
                map.getWorld().getRegion(regionPos.getX(), regionPos.getY())
                        .iterateAllChunks(findHashConsumer);
                currentChunkHash = findHashConsumer.timestamp;
            } catch (IOException e) {
                Logger.global.logError("Failed to load chunk-hash.", e);
            }

            Map<String, Object> lines = new LinkedHashMap<>();
            lines.put("region-pos", regionPos);
            lines.put("chunk-pos", chunkPos);
            lines.put("chunk-curr-hash", currentChunkHash);
            lines.put("chunk-last-hash", lastChunkHash);
            lines.put("tile-pos", tilePos);
            lines.put("tile-render-time", tileInfo.getRenderTime());
            lines.put("tile-state", tileInfo.getState().getKey().getFormatted());

            source.sendMessage(Text.of(TextColor.GOLD, "Map tile info:"));
            source.sendMessage(formatMap(lines));
        }, "BlueMap-Plugin-DebugMapCommand").start();

        return 1;
    }

    public int debugBlockCommand(CommandContext<S> context) {
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
                source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.worldHelperHover(), " with this id: ", TextColor.WHITE, worldName.get()));
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
            Block<?> block = new Block<>(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
            Block<?> blockBelow = new Block<>(world, blockPos.getX(), blockPos.getY() - 1, blockPos.getZ());

            source.sendMessages(Arrays.asList(
                    Text.of(TextColor.GOLD, "Block at you: \n", formatBlock(block)),
                    Text.of(TextColor.GOLD, "Block below you: \n", formatBlock(blockBelow))
                ));
        }, "BlueMap-Plugin-DebugBlockCommand").start();

        return 1;
    }

    private Text formatBlock(Block<?> block) {
        World world = block.getWorld();
        Chunk chunk = block.getChunk();

        Map<String, Object> lines = new LinkedHashMap<>();
        lines.put("world-id", world.getId());
        lines.put("world-name", world.getName());
        lines.put("chunk-is-generated", chunk.isGenerated());
        lines.put("chunk-has-lightdata", chunk.hasLightData());
        lines.put("chunk-inhabited-time", chunk.getInhabitedTime());
        lines.put("block-state", block.getBlockState());
        lines.put("biome", block.getBiome().getKey());
        lines.put("position", block.getX() + " | " + block.getY() + " | " + block.getZ());
        lines.put("block-light", block.getBlockLightLevel());
        lines.put("sun-light", block.getSunLightLevel());

        BlockEntity blockEntity = block.getBlockEntity();
        if (blockEntity != null) {
            lines.put("block-entity", blockEntity);
        }

        return formatMap(lines);
    }

    private Text formatMap(Map<String, Object> lines) {
        Object[] textElements = lines.entrySet().stream()
                .flatMap(e -> Stream.of(TextColor.GRAY, e.getKey(), ": ", TextColor.WHITE, e.getValue(), "\n"))
                .toArray(Object[]::new);
        textElements[textElements.length - 1] = "";

        return Text.of(textElements);
    }

    public int debugDumpCommand(CommandContext<S> context) {
        final CommandSource source = commandSourceInterface.apply(context.getSource());

        try {
            Path file = plugin.getBlueMap().getConfig().getCoreConfig().getData().resolve("dump.json");
            StateDumper.global().dump(file);

            source.sendMessage(Text.of(TextColor.GREEN, "Dump created at: " + file));
            return 1;
        } catch (IOException ex) {
            Logger.global.logError("Failed to create dump!", ex);
            source.sendMessage(Text.of(TextColor.RED, "Exception trying to create dump! See console for details."));
            return 0;
        }
    }

    public int stopCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        if (plugin.getRenderManager().isRunning()) {
            new Thread(() -> {
                plugin.getPluginState().setRenderThreadsEnabled(false);

                plugin.getRenderManager().stop();
                source.sendMessage(Text.of(TextColor.GREEN, "Render-Threads stopped!"));

                plugin.save();
            }, "BlueMap-Plugin-StopCommand").start();
        } else {
            source.sendMessage(Text.of(TextColor.RED, "Render-Threads are already stopped!"));
            return 0;
        }

        return 1;
    }

    public int startCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        if (!plugin.getRenderManager().isRunning()) {
            new Thread(() -> {
                plugin.getPluginState().setRenderThreadsEnabled(true);

                plugin.getRenderManager().start(plugin.getBlueMap().getConfig().getCoreConfig().resolveRenderThreadCount());
                source.sendMessage(Text.of(TextColor.GREEN, "Render-Threads started!"));

                plugin.save();
            }, "BlueMap-Plugin-StartCommand").start();
        } else {
            source.sendMessage(Text.of(TextColor.RED, "Render-Threads are already running!"));
            return 0;
        }

        return 1;
    }

    public int freezeCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        // parse map argument
        String mapString = context.getArgument("map", String.class);
        BmMap map = parseMap(mapString).orElse(null);

        if (map == null) {
            source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.mapHelperHover(), " with this id: ", TextColor.WHITE, mapString));
            return 0;
        }

        PluginState.MapState mapState = plugin.getPluginState().getMapState(map);
        if (mapState.isUpdateEnabled()) {
            new Thread(() -> {
                mapState.setUpdateEnabled(false);

                plugin.stopWatchingMap(map);
                plugin.getRenderManager().removeRenderTasksIf(task -> {
                    if (task instanceof MapUpdateTask)
                        return ((MapUpdateTask) task).getMap().equals(map);

                    if (task instanceof WorldRegionRenderTask)
                        return ((WorldRegionRenderTask) task).getMap().equals(map);

                    return false;
                });

                source.sendMessage(Text.of(TextColor.GREEN, "Map ", TextColor.WHITE, mapString, TextColor.GREEN, " is now frozen and will no longer be automatically updated!"));
                source.sendMessage(Text.of(TextColor.GRAY, "Any currently scheduled updates for this map have been cancelled."));

                plugin.save();
            }, "BlueMap-Plugin-FreezeCommand").start();
        } else {
            source.sendMessage(Text.of(TextColor.RED, "This map is already frozen!"));
            return 0;
        }

        return 1;
    }

    public int unfreezeCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        // parse map argument
        String mapString = context.getArgument("map", String.class);
        BmMap map = parseMap(mapString).orElse(null);

        if (map == null) {
            source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.mapHelperHover(), " with this id: ", TextColor.WHITE, mapString));
            return 0;
        }

        PluginState.MapState mapState = plugin.getPluginState().getMapState(map);
        if (!mapState.isUpdateEnabled()) {
            new Thread(() -> {
                mapState.setUpdateEnabled(true);

                plugin.startWatchingMap(map);
                plugin.getRenderManager().scheduleRenderTask(new MapUpdateTask(map));

                source.sendMessage(Text.of(TextColor.GREEN, "Map ", TextColor.WHITE, mapString, TextColor.GREEN, " is no longer frozen and will be automatically updated!"));

                plugin.save();
            }, "BlueMap-Plugin-UnfreezeCommand").start();
        } else {
            source.sendMessage(Text.of(TextColor.RED, "This map is not frozen!"));
            return 0;
        }

        return 1;
    }

    public int forceUpdateCommand(CommandContext<S> context) {
        return updateCommand(context, s -> true);
    }

    public int fixEdgesCommand(CommandContext<S> context) {
        return updateCommand(context, s -> s == TileState.RENDERED_EDGE);
    }

    public int updateCommand(CommandContext<S> context) {
        return updateCommand(context, s -> false);
    }

    public int updateCommand(CommandContext<S> context, Predicate<TileState> force) {
        final CommandSource source = commandSourceInterface.apply(context.getSource());

        // parse world/map argument
        Optional<String> worldOrMap = getOptionalArgument(context, "world|map", String.class);

        final World worldToRender;
        final BmMap mapToRender;
        if (worldOrMap.isPresent()) {
            worldToRender = parseWorld(worldOrMap.get()).orElse(null);

            if (worldToRender == null) {
                mapToRender = parseMap(worldOrMap.get()).orElse(null);

                if (mapToRender == null) {
                    source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.worldHelperHover(), " or ",
                            helper.mapHelperHover(), " with this id: ", TextColor.WHITE, worldOrMap.get()));
                    return 0;
                }
            } else {
                mapToRender = null;
            }
        } else {
            worldToRender = source.getWorld().orElse(null);
            mapToRender = null;

            if (worldToRender == null) {
                source.sendMessage(Text.of(TextColor.RED, "Can't detect a world from this command-source, you'll have to define a world or a map to update!"));
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
                    source.sendMessage(Text.of(TextColor.RED, "Can't detect a position from this command-source, you'll have to define x,z coordinates to update with a radius!"));
                    return 0;
                }

                center = position.toVector2(true).floor().toInt();
            }
        } else {
            center = null;
        }

        // execute render
        new Thread(() -> {
            try {
                List<BmMap> maps = new ArrayList<>();
                if (worldToRender != null) {
                    plugin.flushWorldUpdates(worldToRender);
                    for (BmMap map : plugin.getBlueMap().getMaps().values()) {
                        if (map.getWorld().equals(worldToRender)) maps.add(map);
                    }
                } else {
                    plugin.flushWorldUpdates(mapToRender.getWorld());
                    maps.add(mapToRender);
                }

                if (maps.isEmpty()) {
                    source.sendMessage(Text.of(TextColor.RED, "No map has been found for this world that could be updated!"));
                    return;
                }

                for (BmMap map : maps) {
                    MapUpdateTask updateTask = new MapUpdateTask(map, center, radius, force);
                    plugin.getRenderManager().scheduleRenderTask(updateTask);

                    source.sendMessage(Text.of(TextColor.GREEN, "Created new Update-Task for map '" + map.getId() + "' ",
                            TextColor.GRAY, "(" + updateTask.getRegions().size() + " regions, ~" + updateTask.getRegions().size() * 1024L + " chunks)"));
                }
                source.sendMessage(Text.of(TextColor.GREEN, "Use ", TextColor.GRAY, "/bluemap", TextColor.GREEN, " to see the progress."));

            } catch (IOException ex) {
                source.sendMessage(Text.of(TextColor.RED, "There was an unexpected exception trying to save the world. Please check the console for more details..."));
                Logger.global.logError("Unexpected exception trying to save the world!", ex);
            }
        }, "BlueMap-Plugin-UpdateCommand").start();

        return 1;
    }

    public int cancelCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        Optional<String> ref = getOptionalArgument(context,"task-ref", String.class);
        if (ref.isEmpty()) {
            plugin.getRenderManager().removeAllRenderTasks();
            source.sendMessage(Text.of(TextColor.GREEN, "All tasks cancelled!"));
            source.sendMessage(Text.of(TextColor.GRAY, "(Note, that an already started task might not be removed immediately. Some tasks needs to do some tidying-work first)"));
            return 1;
        }

        Optional<RenderTask> task = helper.getTaskForRef(ref.get());

        if (task.isEmpty()) {
            source.sendMessage(Text.of(TextColor.RED, "There is no task with this reference '" + ref.get() + "'!"));
            return 0;
        }

        if (plugin.getRenderManager().removeRenderTask(task.get())) {
            source.sendMessage(Text.of(TextColor.GREEN, "Task cancelled!"));
            source.sendMessage(Text.of(TextColor.GRAY, "(Note, that an already started task might not be removed immediately. Some tasks needs to do some tidying-work first)"));
            return 1;
        } else {
            source.sendMessage(Text.of(TextColor.RED, "This task is either completed or got cancelled already!"));
            return 0;
        }
    }

    public int purgeCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        // parse map argument
        String mapString = context.getArgument("map", String.class);
        BmMap map = parseMap(mapString).orElse(null);

        if (map == null) {
            source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.mapHelperHover(), " with this id: ", TextColor.WHITE, mapString));
            return 0;
        }

        new Thread(() -> {
            try {
                // delete map
                MapPurgeTask purgeTask = new MapPurgeTask(map);

                plugin.getRenderManager().scheduleRenderTaskNext(purgeTask);
                source.sendMessage(Text.of(TextColor.GREEN, "Created new Task to purge map '" + map.getId() + "'"));

                // cancel task if currently rendering the same map
                RenderTask currentRenderTask = plugin.getRenderManager().getCurrentRenderTask();
                if (currentRenderTask instanceof MapUpdateTask && ((MapUpdateTask) currentRenderTask).getMap().getId().equals(map.getId())) {
                    currentRenderTask.cancel();
                }

                // start updating the map after the purge
                if (plugin.getPluginState().getMapState(map).isUpdateEnabled()) {
                    RenderTask updateTask = new MapUpdateTask(map);
                    plugin.getRenderManager().scheduleRenderTask(updateTask);
                    source.sendMessage(Text.of(TextColor.GREEN, "Created new Update-Task for map '" + map.getId() + "'"));
                    source.sendMessage(Text.of(TextColor.GRAY, "If you don't want this map to render again after the purge, use ",
                            TextColor.DARK_GRAY, "/bluemap freeze " + map.getId(), TextColor.GRAY, " first!"));
                }

                source.sendMessage(Text.of(TextColor.GREEN, "Use ", TextColor.GRAY, "/bluemap", TextColor.GREEN, " to see the progress."));
            } catch (IllegalArgumentException e) {
                source.sendMessage(Text.of(TextColor.RED, "There was an error trying to purge '" + map.getId() + "', see console for details."));
                Logger.global.logError("Failed to purge map '" + map.getId() + "'!", e);
            }
        }, "BlueMap-Plugin-PurgeCommand").start();

        return 1;
    }

    public int worldsCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        source.sendMessage(Text.of(TextColor.BLUE, "Worlds loaded by BlueMap:"));
        for (var entry : plugin.getBlueMap().getWorlds().entrySet()) {
            source.sendMessage(Text.of(TextColor.GRAY, " - ", TextColor.WHITE, entry.getKey()));
        }

        return 1;
    }

    public int mapsCommand(CommandContext<S> context) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.of(TextColor.BLUE, "Maps loaded by BlueMap:"));

        for (BmMap map : plugin.getBlueMap().getMaps().values()) {
            boolean frozen = !plugin.getPluginState().getMapState(map).isUpdateEnabled();

            lines.add(Text.of(TextColor.GRAY, " - ",
                    TextColor.WHITE, map.getId(),
                    TextColor.GRAY, " (" + map.getName() + ")"));

            lines.add(Text.of(TextColor.GRAY, "\u00A0\u00A0\u00A0World: ",
                    TextColor.DARK_GRAY, map.getWorld().getId()));
            lines.add(Text.of(TextColor.GRAY, "\u00A0\u00A0\u00A0Last Update: ",
                    TextColor.DARK_GRAY, helper.formatTime(map.getMapTileState().getLastRenderTime() * 1000L)));

            if (frozen)
                lines.add(Text.of(TextColor.AQUA, TextFormat.ITALIC, "\u00A0\u00A0\u00A0This map is frozen!"));
        }

        CommandSource source = commandSourceInterface.apply(context.getSource());
        source.sendMessages(lines);

        return 1;
    }

    public int storagesCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        source.sendMessage(Text.of(TextColor.BLUE, "Storages loaded by BlueMap:"));
        for (var entry : plugin.getBlueMap().getConfig().getStorageConfigs().entrySet()) {
            String storageTypeKey = "?";
            try {
                storageTypeKey = entry.getValue().getStorageType().getKey().getFormatted();
            } catch (ConfigurationException ignore) {} // should never happen

            source.sendMessage(Text.of(TextColor.GRAY, " - ", TextColor.WHITE, entry.getKey())
                    .setHoverText(Text.of(storageTypeKey))
                    .setClickAction(Text.ClickAction.RUN_COMMAND, "/bluemap storages " + entry.getKey())
            );
        }

        return 1;
    }

    public int storagesInfoCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());
        String storageId = context.getArgument("storage", String.class);

        Storage storage;
        try {
            storage = plugin.getBlueMap().getOrLoadStorage(storageId);
        } catch (ConfigurationException | InterruptedException ex) {
            Logger.global.logError("Unexpected exception trying to load storage '" + storageId + "'!", ex);
            source.sendMessage(Text.of(TextColor.RED, "There was an unexpected exception trying to load this storage. Please check the console for more details..."));
            return 0;
        }

        Collection<String> mapIds;
        try {
            mapIds = storage.mapIds().toList();
        } catch (IOException ex) {
            Logger.global.logError("Unexpected exception trying to load mapIds from storage '" + storageId + "'!", ex);
            source.sendMessage(Text.of(TextColor.RED, "There was an unexpected exception trying to access this storage. Please check the console for more details..."));
            return 0;
        }

        source.sendMessage(Text.of(TextColor.BLUE, "Storage '", storageId, "':"));
        if (mapIds.isEmpty()) {
            source.sendMessage(Text.of(TextColor.GRAY, " <empty storage>"));
        } else {
            for (String mapId : mapIds) {
                BmMap map = plugin.getBlueMap().getMaps().get(mapId);
                boolean isLoaded = map != null && map.getStorage().equals(storage.map(mapId));

                if (isLoaded) {
                    source.sendMessage(Text.of(TextColor.GRAY, " - ", TextColor.WHITE, mapId, TextColor.GREEN, TextFormat.ITALIC, " (loaded)"));
                } else {
                    source.sendMessage(Text.of(TextColor.GRAY, " - ", TextColor.WHITE, mapId, TextColor.DARK_GRAY, TextFormat.ITALIC, " (unloaded/static/remote)"));
                }
            }
        }

        return 1;
    }

    public int storagesDeleteMapCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());
        String storageId = context.getArgument("storage", String.class);
        String mapId = context.getArgument("map", String.class);

        MapStorage storage;
        try {
            storage = plugin.getBlueMap().getOrLoadStorage(storageId).map(mapId);
        } catch (ConfigurationException | InterruptedException ex) {
            Logger.global.logError("Unexpected exception trying to load storage '" + storageId + "'!", ex);
            source.sendMessage(Text.of(TextColor.RED, "There was an unexpected exception trying to load this storage. Please check the console for more details..."));
            return 0;
        }

        BmMap map = plugin.getBlueMap().getMaps().get(mapId);
        boolean isLoaded = map != null && map.getStorage().equals(storage);
        if (isLoaded) {
            Text purgeCommand = Text.of(TextColor.WHITE, "/bluemap purge " + mapId)
                            .setClickAction(Text.ClickAction.SUGGEST_COMMAND, "/bluemap purge " + mapId);
            source.sendMessage(Text.of(TextColor.RED, "Can't delete a loaded map!\n" +
                    "Unload the map by removing its config-file first,\n" +
                    "or use ", purgeCommand, " if you want to purge it."));
            return 0;
        }

        // delete map
        StorageDeleteTask deleteTask = new StorageDeleteTask(storage, mapId);

        plugin.getRenderManager().scheduleRenderTaskNext(deleteTask);
        source.sendMessage(Text.of(TextColor.GREEN, "Created new Task to delete map '" + mapId + "' from storage '" + storageId + "'"));

        return 1;
    }

}
