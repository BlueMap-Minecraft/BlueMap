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
import net.kyori.adventure.text.Component;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

@Getter
public class WorldHasMapsCheck implements Check {

    private final ServerWorld world;
    private final BmMap[] maps;

    public WorldHasMapsCheck(Plugin plugin, ServerWorld world) {
        this.world = world;
        this.maps = plugin.getBlueMap().getMaps().values().stream()
                .filter(m -> m.getWorld().getId().equals(getWorldId()))
                .toArray(BmMap[]::new);
    }

    public String getWorldId() {
        return World.id(world.getWorldFolder(), world.getDimension());
    }

    @Override
    public CheckResult getResult() {
        return this.maps.length > 0 ? CheckResult.OK : CheckResult.BAD;
    }

    @Override
    public Component getFailureDescription() {
        return lines(
                text("⚠ there are no maps configured for"),
                text("your current world"),
                empty(),
                mapWorldConfigInfo()
        );
    }

    private Component mapWorldConfigInfo() {
        return format("""
                to configure a map for your current world,
                make sure to set
                %
                in the maps config file
                """.strip(),
                formatWorldConfig().color(INFO_COLOR)
        ).color(BASE_COLOR);
    }

    private Component formatWorldConfig() {
        return format("""
                ┌
                │ world: "%"
                │ dimension: "%"
                └
                """.strip(),
                BlueMapConfigManager.formatPath(world.getWorldFolder()),
                world.getDimension().getFormatted()
        );
    }

}
