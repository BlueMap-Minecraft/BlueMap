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
import de.bluecolored.bluemap.common.config.ConfigurationException;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.core.BlueMap;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public class VersionCommand {

    private final Plugin plugin;

    @Command("version")
    @Permission("bluemap.version")
    public Component version() throws ConfigurationException {
        return paragraph("Version",
                lines(
                        format("Version: %", text(BlueMap.VERSION).color(INFO_COLOR)).color(BASE_COLOR),
                        details(BASE_COLOR,
                                text(BlueMap.GIT_HASH),
                                format("Implementation: %", text(plugin.getImplementationType()).color(HIGHLIGHT_COLOR)),
                                format("Minecraft: %", text(plugin.getBlueMap().getOrLoadMinecraftVersion().getId()).color(HIGHLIGHT_COLOR))
                        ),
                        format("Available Processors: %", text(Runtime.getRuntime().availableProcessors()).color(HIGHLIGHT_COLOR)).color(BASE_COLOR),
                        format("Available Memory: %", text(Math.round(Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0 / 102.4) / 10.0 + " GiB").color(HIGHLIGHT_COLOR)).color(BASE_COLOR)
                ).color(BASE_COLOR)
        );
    }

}
