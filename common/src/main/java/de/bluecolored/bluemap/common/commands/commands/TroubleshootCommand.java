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
import de.bluecolored.bluemap.common.commands.checks.*;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.map.BmMap;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public class TroubleshootCommand {

    private final Plugin plugin;

    @Command("troubleshoot")
    @Permission("bluemap.troubleshoot")
    public Component troubleshoot(ServerWorld world, Vector3d position) {
        return runChecks(() -> {

            BmMap[] maps;

            if (world != null) {
                // check world has maps
                WorldHasMapsCheck worldHasMapsCheck = new WorldHasMapsCheck(plugin, world);
                worldHasMapsCheck.test();

                // check all maps of world
                maps = worldHasMapsCheck.getMaps();
            } else {
                // check all maps
                maps = plugin.getBlueMap().getMaps().values().toArray(BmMap[]::new);
            }

            Vector2i position2i = position != null ? position.toVector2(true).toInt() : null;
            troubleshoot(maps, position2i);

        });
    }

    @Command("troubleshoot <map>")
    @Permission("bluemap.troubleshoot")
    public Component troubleshoot(ServerWorld world, Vector3d position, @Argument("map") BmMap map) {
        return runChecks(() -> {

            if (world != null) {
                // check map has correct world
                new MapHasCorrectWorldCheck(plugin, map, world).test();
            }

            Vector2i position2i = position != null ? position.toVector2(true).toInt() : null;
            troubleshoot(new BmMap[]{ map }, position2i);

        });
    }

    @Command("troubleshoot <map> <x> <z>")
    @Permission("bluemap.troubleshoot")
    public Component troubleshoot(@Argument("map") BmMap map, @Argument("x") int x, @Argument("z") int z) {
        return runChecks(() -> {
            Vector2i position2i = new Vector2i(x, z);
            troubleshoot(new BmMap[]{ map }, position2i);
        });
    }

    private void troubleshoot(BmMap[] maps, @Nullable Vector2i position) throws Check.CheckFailedException {
        // check render-threads running
        new RenderThreadsRunningCheck(plugin).test();

        if (position != null) {
            // check for tile problems
            for (BmMap map : maps) {
                troubleshootTileAt(map, position);
            }

            // check tile updates
            for (BmMap map : maps) {
                new TileIsUpdatedCheck(plugin, map, position).test();
            }
        }

        // check maps are updated
        for (BmMap map : maps) {
            new MapIsUpdatedCheck(plugin, map).test();
        }

        // check maps are not frozen
        for (BmMap map : maps) {
            new MapIsNotFrozenCheck(plugin, map).test();
        }

        if (position != null) {
            // check map-boundaries
            for (BmMap map : maps) {
                new TileInsideBoundsCheck(plugin, map, position).test();
            }
        }
    }

    private void troubleshootTileAt(BmMap map, Vector2i position) throws Check.CheckFailedException {
        Check[] regionChecks = new Check[]{
                new TileNoRenderErrorCheck(map, position),
                new TileNoChunkErrorCheck(map, position),
                new TileHasLightDataCheck(map, position)
        };

        // does the tile have problems
        boolean anyFails = Arrays.stream(regionChecks)
                .anyMatch(Check::failed);

        if (anyFails) {
            // if there are problems with the tile, check if tile is up-to-date first
            new TileIsUpdatedCheck(plugin, map, position).test();
            new MapIsNotFrozenCheck(plugin, map).test();

            // if tile is updated then the other checks are valid
            for (Check check : regionChecks) check.test();
        }
    }

    private Component runChecks(CheckRun run) {
        try {
            run.test();
            return result(text("✔ no issues found").color(POSITIVE_COLOR));
        } catch (Check.CheckFailedException ex) {
            return result(ex.getCheck().getFailureDescription().color(WARNING_COLOR));
        }
    }

    private Component result(Component result) {
        return paragraph("Troubleshooting", result);
    }

    private interface CheckRun {
        void test() throws Check.CheckFailedException;
    }

}
