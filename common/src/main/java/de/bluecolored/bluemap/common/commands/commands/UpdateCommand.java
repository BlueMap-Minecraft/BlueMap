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
package de.bluecolored.bluemap.common.commands.commands;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluecommands.annotations.Argument;
import de.bluecolored.bluecommands.annotations.Command;
import de.bluecolored.bluemap.common.commands.Permission;
import de.bluecolored.bluemap.common.commands.WithPosition;
import de.bluecolored.bluemap.common.commands.WithWorld;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.rendermanager.MapUpdateTask;
import de.bluecolored.bluemap.common.rendermanager.TileUpdateStrategy;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.world.World;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public class UpdateCommand {

    private final Plugin plugin;
    private final TileUpdateStrategy updateStrategy;

    @Command("")
    @Permission("bluemap.update")
    @WithWorld
    public boolean update(CommandSource source, World world) throws IOException {
        return update(source, world, null, null);
    }

    @Command("<radius>")
    @Permission("bluemap.update")
    @WithWorld
    @WithPosition
    public boolean update(
            CommandSource source,
            World world,
            Vector3d position,
            @Argument("radius") int radius
    ) throws IOException {
        return update(source, world, position.toVector2(true).toInt(), radius);
    }

    @Command("<x> <z> <radius>")
    @Permission("bluemap.update")
    @WithWorld
    public boolean update(
            CommandSource source,
            World world,
            @Argument("x") int x,
            @Argument("z") int z,
            @Argument("radius") int radius
    ) throws IOException {
        return update(source, world, new Vector2i(x, z), radius);
    }

    @Command("<map>")
    @Permission("bluemap.update")
    public boolean update(
            CommandSource source,
            @Argument("map") BmMap map
    ) throws IOException {
        return update(source, map.getWorld(), List.of(map), null, null);
    }

    @Command("<map> <radius>")
    @Permission("bluemap.update")
    @WithWorld
    @WithPosition
    public boolean update(
            CommandSource source,
            World world,
            Vector3d position,
            @Argument("map") BmMap map,
            @Argument("radius") int radius
    ) throws IOException {
        if (!map.getWorld().equals(world)) {
            source.sendMessage(text("The map does not belong to the same world you are currently in!")
                    .color(NEGATIVE_COLOR));
            return false;
        }

        return update(source, map.getWorld(), List.of(map), position.toVector2(true).toInt(), radius);
    }

    @Command("<map> <x> <z> <radius>")
    @Permission("bluemap.update")
    public boolean update(
            CommandSource source,
            @Argument("map") BmMap map,
            @Argument("x") int x,
            @Argument("z") int z,
            @Argument("radius") int radius
    ) throws IOException {
        return update(source, map.getWorld(), List.of(map), new Vector2i(x, z), radius);
    }

    private boolean update(
            CommandSource source, World world,
            @Nullable Vector2i center, @Nullable Integer radius
    ) throws IOException {
        // find maps for world
        List<BmMap> maps = new ArrayList<>();
        for (BmMap map : plugin.getBlueMap().getMaps().values()) {
            if (map.getWorld().equals(world)) maps.add(map);
        }

        if (maps.isEmpty()) {
            source.sendMessage(text("No map has been found for this world that could be updated!")
                    .color(NEGATIVE_COLOR));
            return false;
        }

        // sort by map-sorting
        maps.sort(Comparator.comparing(map -> map.getMapSettings().getSorting()));

        return update(source, world, maps, center, radius);
    }

    private boolean update(
            CommandSource source, World world, Collection<BmMap> maps,
            @Nullable Vector2i center, @Nullable Integer radius
    ) throws IOException {
        source.sendMessage(text("Creating update-tasks ...").color(INFO_COLOR));
        plugin.flushWorldUpdates(world);
        for (BmMap map : maps) {
            plugin.getRenderManager().scheduleRenderTask(new MapUpdateTask(map, center, radius, updateStrategy));
            source.sendMessage(format("Created new update-task for map %",
                    formatMap(map).color(HIGHLIGHT_COLOR)
            ).color(POSITIVE_COLOR));
        }
        source.sendMessage(format("Use % to see the progress",
                command("/bluemap").color(HIGHLIGHT_COLOR)
        ).color(BASE_COLOR));
        return true;
    }


}
