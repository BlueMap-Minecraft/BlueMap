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
import de.bluecolored.bluemap.common.rendermanager.MapRenderTask;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.core.map.BmMap;
import lombok.RequiredArgsConstructor;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public class FreezeCommand {

    private final Plugin plugin;

    @Command("freeze <map>")
    @Permission("bluemap.freeze")
    public void freeze(CommandSource source, @Argument("map") BmMap map) {
        plugin.getPluginState().getMapState(map).setUpdateEnabled(false);
        plugin.stopWatchingMap(map);
        plugin.getRenderManager().removeRenderTasksIf(task ->
                task instanceof MapRenderTask mrt &&
                        mrt.getMap().equals(map)
        );
        source.sendMessage(format("""
                % Map % is now % and will no longer automatically update
                Any currently scheduled updates for this map have been cancelled
                """.strip(),
                ICON_FROZEN,
                formatMap(map).color(HIGHLIGHT_COLOR),
                text("frozen").color(FROZEN_COLOR)
        ).color(BASE_COLOR));
        plugin.save();
    }

}
