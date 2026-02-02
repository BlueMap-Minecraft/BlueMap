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
package de.bluecolored.bluemap.common.commands.checks;

import de.bluecolored.bluemap.common.config.BlueMapConfigManager;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.world.World;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

@Getter
@RequiredArgsConstructor
public class MapHasCorrectWorldCheck implements Check {

    private final Plugin plugin;
    private final BmMap map;
    private final ServerWorld expectedWorld;

    public String getExpectedWorldId() {
        return World.id(expectedWorld.getWorldFolder(), expectedWorld.getDimension());
    }

    @Override
    public CheckResult getResult() {
        return this.map.getWorld().getId().equals(getExpectedWorldId()) ? CheckResult.OK : CheckResult.BAD;
    }

    @Override
    public Component getFailureDescription() {
        return lines(
                format("""
                        ⚠ map % is configured for
                        world % instead of
                        world %
                        """.strip(),
                        formatMap(map).color(HIGHLIGHT_COLOR),
                        text(map.getWorld().getId()).color(HIGHLIGHT_COLOR),
                        text(getExpectedWorldId()).color(HIGHLIGHT_COLOR)),
                empty(),
                mapWorldConfigInfo());
    }

    private Component mapWorldConfigInfo() {
        return format("""
                to configure the map for your current world,
                make sure to set
                %
                in the % config file
                """.strip(),
                formatWorldConfig().color(INFO_COLOR),
                formatConfigFilePath("maps/" + map.getId()).color(HIGHLIGHT_COLOR)).color(BASE_COLOR);
    }

    private Component formatWorldConfig() {
        return format("""
                ┌
                │ world: "%"
                │ dimension: "%"
                └
                """.strip(),
                BlueMapConfigManager.formatPath(expectedWorld.getWorldFolder()),
                expectedWorld.getDimension().getFormatted());
    }

    private Component formatConfigFilePath(String name) {
        Component format = text(name + ".conf");

        if (plugin.getBlueMap().getConfig() instanceof BlueMapConfigManager configManager) {
            format = format.hoverEvent(
                    text(BlueMapConfigManager.formatPath(configManager.getConfigManager().resolveConfigFile(name))));
        }

        return format;
    }

}
