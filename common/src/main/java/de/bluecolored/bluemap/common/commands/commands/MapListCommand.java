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

import de.bluecolored.bluecommands.annotations.Command;
import de.bluecolored.bluemap.common.commands.Permission;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.rendermanager.*;
import de.bluecolored.bluemap.core.map.BmMap;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public class MapListCommand {

    private final Plugin plugin;

    @Command("maps")
    @Permission("bluemap.maps")
    public Component mapList() {
        Map<String, BmMap> maps = plugin.getBlueMap().getMaps();

        List<Component> entries = maps.values().stream()
                .sorted(Comparator.comparing(map -> map.getMapSettings().getSorting()))
                .map(this::element)
                .toList();

        return paragraph("Maps", lines(entries));
    }

    private Component element(BmMap map) {
        Component icon = ICON_UPDATED;
        LinkedList<Component> details = new LinkedList<>();

        if (!plugin.getPluginState().getMapState(map).isUpdateEnabled()) {
            icon = ICON_FROZEN;
            details.addFirst(format("is %",
                    text("frozen").color(HIGHLIGHT_COLOR)
            ));
        }

        long pendingCount = plugin.getRenderManager().getScheduledRenderTasks().stream()
                .skip(1)
                .filter(task -> task instanceof MapRenderTask mapTask && mapTask.getMap().equals(map))
                .count();
        if (pendingCount > 0) {
            icon = ICON_PENDING;
            details.addFirst(format(pendingCount == 1 ? "has % pending task" : "has % pending tasks",
                    text(pendingCount).color(HIGHLIGHT_COLOR)
            ));
        }

        RenderTask task = plugin.getRenderManager().getCurrentRenderTask();
        if (task instanceof MapRenderTask mapTask && mapTask.getMap().equals(map)) {
            icon = ICON_IN_PROGRESS;
            String format = switch (task) {
                case MapUpdateTask ignored -> "is currently being updated: %";
                case WorldRegionRenderTask ignored -> "is currently being updated: %";
                case MapPurgeTask ignored -> "is currently being purged: %";
                default -> "has a running task: %";
            };
            details.addFirst(format(format,
                    text(String.format("%.3f%%", task.estimateProgress() * 100)).color(HIGHLIGHT_COLOR)
            ).hoverEvent(HoverEvent.showText(text(task.getDescription()))));
        }

        return lines(
                icon.appendSpace().append(formatMap(map).color(HIGHLIGHT_COLOR)),
                details.isEmpty() ? null : details(BASE_COLOR, details)
        );
    }

}
