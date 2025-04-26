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
package de.bluecolored.bluemap.common.commands;

import com.flowpowered.math.vector.Vector3d;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import de.bluecolored.bluecommands.*;
import de.bluecolored.bluemap.common.commands.arguments.MapBackedArgumentParser;
import de.bluecolored.bluemap.common.commands.arguments.StringSetArgumentParser;
import de.bluecolored.bluemap.common.commands.commands.*;
import de.bluecolored.bluemap.common.config.storage.StorageConfig;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.rendermanager.RenderTask;
import de.bluecolored.bluemap.common.rendermanager.TileUpdateStrategy;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.world.World;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.text;

public class Commands {

    private static final Cache<String, RenderTask> REF_TO_RENDERTASK = Caffeine.newBuilder()
            .weakValues()
            .build();
    private static final LoadingCache<RenderTask, String> RENDERTASK_TO_REF = Caffeine.newBuilder()
            .weakKeys()
            .build(Commands::safeRandomRef);

    public static de.bluecolored.bluecommands.Command<CommandSource, Object> create(Plugin plugin) {
        BlueCommands<CommandSource> builder = new BlueCommands<>();

        builder.setArgumentParserForArgumentType(BmMap.class, new MapBackedArgumentParser<>("map", () ->
                plugin.isLoaded() ? plugin.getBlueMap().getMaps() : Map.of()));
        builder.setArgumentParserForArgumentType(StorageConfig.class, new MapBackedArgumentParser<>("storage", () ->
                plugin.isLoaded() ? plugin.getBlueMap().getConfig().getStorageConfigs() : Map.of()));
        builder.setArgumentParserForArgumentType(RenderTask.class, new MapBackedArgumentParser<>("render-task", REF_TO_RENDERTASK.asMap()));

        builder.setArgumentParserForId("storage-id", new StringSetArgumentParser("storage", () ->
                plugin.isLoaded() ? plugin.getBlueMap().getConfig().getStorageConfigs().keySet() : Set.of()));

        builder.setContextResolverForType(ServerWorld.class, c -> c.getWorld().orElse(null));
        builder.setContextResolverForType(World.class, c -> plugin.isLoaded() ? c.getWorld().map(plugin::getWorld).orElse(null) : null);
        builder.setContextResolverForType(Vector3d.class, c -> c.getPosition().orElse(null));

        builder.setAnnotationContextPredicate(Permission.class, (permission, commandSource) ->
                permission == null || commandSource.hasPermission(permission.value())
        );
        builder.setAnnotationContextPredicate(WithWorld.class, (withWorld, commandSource) ->
                withWorld == null || plugin.isLoaded() && commandSource.getWorld().map(plugin::getWorld).isPresent()
        );
        builder.setAnnotationContextPredicate(WithPosition.class, (withPosition, commandSource) ->
                withPosition == null || commandSource.getPosition().isPresent()
        );

        de.bluecolored.bluecommands.Command<CommandSource, Object> commands = new LiteralCommand<>("bluemap");

        // register commands
        Stream.of(
                new DebugCommand(plugin),
                new FreezeCommand(plugin),
                new HelpCommand(plugin),
                new MapListCommand(plugin),
                new PurgeCommand(plugin),
                new ReloadCommand(plugin),
                new StartCommand(plugin),
                new StatusCommand(plugin),
                new StopCommand(plugin),
                new StoragesCommand(plugin),
                new TasksCommand(plugin),
                new TroubleshootCommand(plugin),
                new UnfreezeCommand(plugin),
                new VersionCommand(plugin)
        )
                .map(builder::createCommand)
                .forEach(commands::addSubCommand);

        // register an update-command for each update-strategy
        Map.of(
                "update", TileUpdateStrategy.FORCE_NONE,
                "fix-edges", TileUpdateStrategy.FORCE_EDGE,
                "force-update", TileUpdateStrategy.FORCE_ALL
        ).forEach((updateLiteral, strategy) -> {
            Command<CommandSource, Object> updateCommand = new LiteralCommand<>(updateLiteral);
            updateCommand.addSubCommand(builder.createCommand(new UpdateCommand(plugin, strategy)));
            commands.addSubCommand(updateCommand);
        });

        return commands;
    }

    public static String getRefForTask(RenderTask task) {
        return RENDERTASK_TO_REF.get(task);
    }

    public static @Nullable RenderTask getTaskForRef(String ref) {
        return REF_TO_RENDERTASK.getIfPresent(ref);
    }

    public static boolean checkExecutablePreconditions(Plugin plugin, CommandSource context, CommandExecutable<CommandSource, Object> executable) {
        if (executable instanceof MethodCommandExecutable<CommandSource> methodExecutable) {

            // check if plugin needs to be loaded
            if (methodExecutable.getMethod().getAnnotation(Unloaded.class) == null) {
                return Commands.checkPluginLoaded(plugin, context);
            }

        }

        return true;
    }

    public static boolean checkPluginLoaded(Plugin plugin, CommandSource context){
        if (!plugin.isLoaded()) {
            if (plugin.isLoading()) {
                context.sendMessage(lines(
                        text("⌛ BlueMap is still loading!").color(INFO_COLOR),
                        text("Please try again in a few seconds.").color(BASE_COLOR)
                ));
            } else {
                context.sendMessage(lines(
                        text("❌ BlueMap is not loaded!").color(NEGATIVE_COLOR),
                        format("Check your server-console for errors or warnings and try using %.",
                                command("/bluemap reload").color(HIGHLIGHT_COLOR)
                        ).color(BASE_COLOR)
                ));
            }
            return false;
        }

        return true;
    }

    private static synchronized String safeRandomRef(RenderTask task) {
        String ref = randomRef();
        while (REF_TO_RENDERTASK.asMap().putIfAbsent(ref, task) != null) ref = randomRef();
        return ref;
    }

    private static String randomRef() {
        StringBuilder ref = new StringBuilder(Integer.toString(Math.abs(new Random().nextInt()), 16));
        while (ref.length() < 4) ref.insert(0, "0");
        return ref.subSequence(0, 4).toString();
    }

}
