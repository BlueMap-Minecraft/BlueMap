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
import de.bluecolored.bluemap.common.commands.Unloaded;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.core.logger.Logger;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
public class ReloadCommand {

    private final Plugin plugin;

    @Command("reload")
    @Permission("bluemap.reload")
    @Unloaded
    public boolean reload(CommandSource source) {
        return reload(source, false);
    }

    @Command("reload light")
    @Permission("bluemap.reload.light")
    @Unloaded
    public boolean reloadLight(CommandSource source) {
        return reload(source, true);
    }

    private boolean reload(CommandSource source, boolean light) {
        try {
            source.sendMessage(text("Reloading BlueMap...").color(INFO_COLOR));

            if (light) plugin.lightReload();
            else plugin.reload();

            if (plugin.isLoaded()) {
                source.sendMessage(text("BlueMap reloaded!").color(POSITIVE_COLOR));
                return true;
            } else {
                source.sendMessage(text("Could not load BlueMap! See logs or console for details!").color(NEGATIVE_COLOR));
                return false;
            }
        } catch (IOException e) {
            Logger.global.logError("Failed to reload BlueMap!", e);
            source.sendMessage(text("There was an error reloading BlueMap! See logs or console for details!").color(NEGATIVE_COLOR));
            return false;
        }
    }

}
