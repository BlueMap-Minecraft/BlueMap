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

import de.bluecolored.bluecommands.annotations.Argument;
import de.bluecolored.bluecommands.annotations.Command;
import de.bluecolored.bluemap.common.commands.Permission;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.rendermanager.MapPurgeTask;
import de.bluecolored.bluemap.common.rendermanager.MapRenderTask;
import de.bluecolored.bluemap.common.rendermanager.MapUpdateTask;
import de.bluecolored.bluemap.common.rendermanager.RenderTask;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

import java.util.LinkedList;
import java.util.List;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.empty;

@RequiredArgsConstructor
public class PurgeCommand {

    private final Plugin plugin;

    @Command("purge <map>")
    @Permission("bluemap.purge")
    public boolean purge(CommandSource source, @Argument("map") BmMap map) {
        try {
            // start updating the map after the purge
            boolean updateMap = plugin.getPluginState().getMapState(map).isUpdateEnabled();

            // delete map
            MapPurgeTask purgeTask = new MapPurgeTask(map);
            plugin.getRenderManager().removeRenderTasksIf(task ->
                    task instanceof MapRenderTask mrt &&
                            mrt.getMap().equals(map)
            );
            plugin.getRenderManager().scheduleRenderTaskNext(purgeTask);

            List<Component> lines = new LinkedList<>();
            lines.add(format("Scheduled a new task to purge map %",
                    formatMap(map).color(HIGHLIGHT_COLOR)
            ).color(POSITIVE_COLOR));
            lines.add(format("Use % to see the progress",
                    command("/bluemap").color(HIGHLIGHT_COLOR)
            ).color(BASE_COLOR));

            if (updateMap) {
                lines.add(empty());
                lines.add(format("""
                                BlueMap will automatically start rendering the map again once the purge is done
                                If you don't want this, use % before purging
                                """.strip(),
                        command("/bluemap freeze " + map.getId()).color(HIGHLIGHT_COLOR)
                ).color(BASE_COLOR));
            }

            source.sendMessage(lines(lines));

            if (updateMap) {
                RenderTask updateTask = new MapUpdateTask(map);
                plugin.getRenderManager().scheduleRenderTask(updateTask);
            }

            return true;
        } catch (IllegalArgumentException e) {
            Logger.global.logError("Failed to purge map '" + map.getId() + "'!", e);
            source.sendMessage(format("There was an error trying to purge %, see console for details.",
                    formatMap(map).color(HIGHLIGHT_COLOR)
            ).color(NEGATIVE_COLOR));
            return false;
        }
    }

}
