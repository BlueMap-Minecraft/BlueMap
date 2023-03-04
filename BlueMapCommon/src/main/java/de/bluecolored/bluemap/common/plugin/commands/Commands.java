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
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.plugin.PluginState;
import de.bluecolored.bluemap.common.plugin.text.Text;
import de.bluecolored.bluemap.common.plugin.text.TextColor;
import de.bluecolored.bluemap.common.plugin.text.TextFormat;
import de.bluecolored.bluemap.common.rendermanager.MapPurgeTask;
import de.bluecolored.bluemap.common.rendermanager.MapUpdateTask;
import de.bluecolored.bluemap.common.rendermanager.RenderTask;
import de.bluecolored.bluemap.common.rendermanager.WorldRegionRenderTask;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.core.BlueMap;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.debug.StateDumper;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.map.MapRenderState;
import de.bluecolored.bluemap.core.world.Block;
import de.bluecolored.bluemap.core.world.World;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class Commands<S> {

    public static final String DEFAULT_MARKER_SET_ID = "markers";

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
        LiteralCommandNode<S> baseCommand =
                literal("bluemap")
                .requires(requirementsUnloaded("bluemap.status"))
                .executes(this::statusCommand)
                .build();

        LiteralCommandNode<S> versionCommand =
                literal("version")
                .requires(requirementsUnloaded("bluemap.version"))
                .executes(this::versionCommand)
                .build();

        LiteralCommandNode<S> helpCommand =
                literal("help")
                .requires(requirementsUnloaded("bluemap.help"))
                .executes(this::helpCommand)
                .build();

        LiteralCommandNode<S> reloadCommand =
                literal("reload")
                .requires(requirementsUnloaded("bluemap.reload"))
                .executes(context -> this.reloadCommand(context, false))

                .then(literal("light")
                        .executes(context -> this.reloadCommand(context, true)))

                .build();

        LiteralCommandNode<S> debugCommand =
                literal("debug")
                .requires(requirementsUnloaded("bluemap.debug"))

                .then(literal("block")
                        .requires(requirements("bluemap.debug"))
                        .executes(this::debugBlockCommand)

                        .then(argument("world", StringArgumentType.string()).suggests(new WorldSuggestionProvider<>(plugin))
                                .then(argument("x", DoubleArgumentType.doubleArg())
                                        .then(argument("y", DoubleArgumentType.doubleArg())
                                                .then(argument("z", DoubleArgumentType.doubleArg())
                                                        .executes(this::debugBlockCommand))))))

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

        LiteralCommandNode<S> stopCommand =
                literal("stop")
                .requires(requirements("bluemap.stop"))
                .executes(this::stopCommand)
                .build();

        LiteralCommandNode<S> startCommand =
                literal("start")
                .requires(requirements("bluemap.start"))
                .executes(this::startCommand)
                .build();

        LiteralCommandNode<S> freezeCommand =
                literal("freeze")
                .requires(requirements("bluemap.freeze"))
                .then(argument("map", StringArgumentType.string()).suggests(new MapSuggestionProvider<>(plugin))
                        .executes(this::freezeCommand))
                        .build();

        LiteralCommandNode<S> unfreezeCommand =
                literal("unfreeze")
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

        LiteralCommandNode<S> updateCommand =
                addRenderArguments(
                        literal("update")
                        .requires(requirements("bluemap.update")),
                        this::updateCommand
                ).build();

        LiteralCommandNode<S> purgeCommand =
                literal("purge")
                        .requires(requirements("bluemap.purge"))
                        .then(argument("map", StringArgumentType.string()).suggests(new MapSuggestionProvider<>(plugin))
                                .executes(this::purgeCommand))
                        .build();

        LiteralCommandNode<S> cancelCommand =
                literal("cancel")
                .requires(requirements("bluemap.cancel"))
                .executes(this::cancelCommand)
                .then(argument("task-ref", StringArgumentType.string()).suggests(new TaskRefSuggestionProvider<>(helper))
                        .executes(this::cancelCommand))
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
        baseCommand.addChild(updateCommand);
        baseCommand.addChild(cancelCommand);
        baseCommand.addChild(purgeCommand);
        baseCommand.addChild(worldsCommand);
        baseCommand.addChild(mapsCommand);
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

    private Optional<World> parseWorld(String worldName) {
        for (World world : plugin.getWorlds().values()) {
            if (world.getName().equalsIgnoreCase(worldName)) {
                return Optional.of(world);
            }
        }

        return Optional.empty();
    }

    private Optional<BmMap> parseMap(String mapId) {
        for (BmMap map : plugin.getMaps().values()) {
            if (map.getId().equalsIgnoreCase(mapId)) {
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

        source.sendMessages(helper.createStatusMessage());
        return 1;
    }

    public int versionCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        int renderThreadCount = 0;
        if (plugin.isLoaded()) {
            renderThreadCount = plugin.getRenderManager().getWorkerThreadCount();
        }

        MinecraftVersion minecraftVersion = plugin.getServerInterface().getMinecraftVersion();

        source.sendMessage(Text.of(TextFormat.BOLD, TextColor.BLUE, "Version: ", TextColor.WHITE, BlueMap.VERSION));
        source.sendMessage(Text.of(TextColor.GRAY, "Commit: ", TextColor.WHITE, BlueMap.GIT_HASH));
        source.sendMessage(Text.of(TextColor.GRAY, "Implementation: ", TextColor.WHITE, plugin.getImplementationType()));
        source.sendMessage(Text.of(
                TextColor.GRAY, "Minecraft compatibility: ", TextColor.WHITE, minecraftVersion.getVersionString(),
                TextColor.GRAY, " (" + minecraftVersion.getResource().getVersion().getVersionString() + ")"
                ));
        source.sendMessage(Text.of(TextColor.GRAY, "Render-threads: ", TextColor.WHITE, renderThreadCount));
        source.sendMessage(Text.of(TextColor.GRAY, "Available processors: ", TextColor.WHITE, Runtime.getRuntime().availableProcessors()));
        source.sendMessage(Text.of(TextColor.GRAY, "Available memory: ", TextColor.WHITE, (Runtime.getRuntime().maxMemory() / 1024L / 1024L) + " MiB"));

        if (minecraftVersion.isAtLeast(new MinecraftVersion(1, 15))) {
            String clipboardValue =
                    "Version: " + BlueMap.VERSION + "\n" +
                    "Commit: " + BlueMap.GIT_HASH + "\n" +
                    "Implementation: " + plugin.getImplementationType() + "\n" +
                    "Minecraft compatibility: " + minecraftVersion.getVersionString() + " (" + minecraftVersion.getResource().getVersion().getVersionString() + ")\n" +
                    "Render-threads: " + renderThreadCount + "\n" +
                    "Available processors: " + Runtime.getRuntime().availableProcessors() + "\n" +
                    "Available memory: " + Runtime.getRuntime().maxMemory() / 1024L / 1024L + " MiB";
            source.sendMessage(Text.of(TextColor.DARK_GRAY, "[copy to clipboard]")
                    .setClickAction(Text.ClickAction.COPY_TO_CLIPBOARD, clipboardValue)
                    .setHoverText(Text.of(TextColor.GRAY, "click to copy the above text .. ", TextFormat.ITALIC, TextColor.GRAY, "duh!")));
        }

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

        for (World world : plugin.getWorlds().values()) {
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
                source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.worldHelperHover(), " with this name: ", TextColor.WHITE, worldName.get()));
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
            Block<?> block = new Block<>(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
            Block<?> blockBelow = new Block<>(null, 0, 0, 0).copy(block, 0, -1, 0);

            // populate lazy-loaded values
            block.getBlockState();
            block.getBiomeId();
            block.getLightData();

            blockBelow.getBlockState();
            blockBelow.getBiomeId();
            blockBelow.getLightData();

            source.sendMessages(Arrays.asList(
                    Text.of(TextColor.GOLD, "Block at you: ", TextColor.WHITE, block),
                    Text.of(TextColor.GOLD, "Block below you: ", TextColor.WHITE, blockBelow)
                ));
        }, "BlueMap-Plugin-DebugBlockCommand").start();

        return 1;
    }

    public int debugDumpCommand(CommandContext<S> context) {
        final CommandSource source = commandSourceInterface.apply(context.getSource());

        try {
            Path file = plugin.getConfigs().getCoreConfig().getData().resolve("dump.json");
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

                plugin.getRenderManager().start(plugin.getConfigs().getCoreConfig().resolveRenderThreadCount());
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
            source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.mapHelperHover(), " with this name: ", TextColor.WHITE, mapString));
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
            source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.mapHelperHover(), " with this name: ", TextColor.WHITE, mapString));
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
        return updateCommand(context, true);
    }

    public int updateCommand(CommandContext<S> context) {
        return updateCommand(context, false);
    }

    public int updateCommand(CommandContext<S> context, boolean force) {
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
                    source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.worldHelperHover(), " or ", helper.mapHelperHover(), " with this name: ", TextColor.WHITE, worldOrMap.get()));
                    return 0;
                }
            } else {
                mapToRender = null;
            }
        } else {
            worldToRender = source.getWorld().orElse(null);
            mapToRender = null;

            if (worldToRender == null) {
                source.sendMessage(Text.of(TextColor.RED, "Can't detect a world from this command-source, you'll have to define a world or a map to update!").setHoverText(Text.of(TextColor.GRAY, "/bluemap " + (force ? "force-update" : "update") + " <world|map>")));
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
                    source.sendMessage(Text.of(TextColor.RED, "Can't detect a position from this command-source, you'll have to define x,z coordinates to update with a radius!").setHoverText(Text.of(TextColor.GRAY, "/bluemap " + (force ? "force-update" : "update") + " <x> <z> " + radius)));
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
                    var world = plugin.getServerInterface().getWorld(worldToRender.getSaveFolder()).orElse(null);
                    if (world != null) world.persistWorldChanges();

                    for (BmMap map : plugin.getMaps().values()) {
                        if (map.getWorld().getSaveFolder().equals(worldToRender.getSaveFolder())) maps.add(map);
                    }
                } else {
                    var world = plugin.getServerInterface().getWorld(mapToRender.getWorld().getSaveFolder()).orElse(null);
                    if (world != null) world.persistWorldChanges();

                    maps.add(mapToRender);
                }

                if (maps.isEmpty()) {
                    source.sendMessage(Text.of(TextColor.RED, "No map has been found for this world that could be updated!"));
                    return;
                }

                for (BmMap map : maps) {
                    MapUpdateTask updateTask = new MapUpdateTask(map, center, radius);
                    plugin.getRenderManager().scheduleRenderTask(updateTask);

                    if (force) {
                        MapRenderState state = map.getRenderState();
                        updateTask.getRegions().forEach(region -> state.setRenderTime(region, -1));
                    }

                    source.sendMessage(Text.of(TextColor.GREEN, "Created new Update-Task for map '" + map.getId() + "' ", TextColor.GRAY, "(" + updateTask.getRegions().size() + " regions, ~" + updateTask.getRegions().size() * 1024L + " chunks)"));
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
            source.sendMessage(Text.of(TextColor.RED, "There is no ", helper.mapHelperHover(), " with this name: ", TextColor.WHITE, mapString));
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
        for (var entry : plugin.getWorlds().entrySet()) {
            source.sendMessage(Text.of(TextColor.GRAY, " - ", TextColor.WHITE, entry.getValue().getName()).setHoverText(Text.of(entry.getValue().getSaveFolder(), TextColor.GRAY, " (" + entry.getKey() + ")")));
        }

        return 1;
    }

    public int mapsCommand(CommandContext<S> context) {
        CommandSource source = commandSourceInterface.apply(context.getSource());

        source.sendMessage(Text.of(TextColor.BLUE, "Maps loaded by BlueMap:"));
        for (BmMap map : plugin.getMaps().values()) {
            boolean unfrozen = plugin.getPluginState().getMapState(map).isUpdateEnabled();
            if (unfrozen) {
                source.sendMessage(Text.of(
                        TextColor.GRAY, " - ",
                        TextColor.WHITE, map.getId(),
                        TextColor.GRAY, " (" + map.getName() + ")"
                ).setHoverText(Text.of(TextColor.WHITE, "World: ", TextColor.GRAY, map.getWorld().getName())));
            } else {
                source.sendMessage(Text.of(
                        TextColor.GRAY, " - ",
                        TextColor.WHITE, map.getId(),
                        TextColor.GRAY, " (" + map.getName() + ") - ",
                        TextColor.AQUA, TextFormat.ITALIC, "frozen!"
                ).setHoverText(Text.of(TextColor.WHITE, "World: ", TextColor.GRAY, map.getWorld().getName())));
            }
        }

        return 1;
    }

}
