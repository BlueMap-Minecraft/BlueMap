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

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.rendermanager.RenderTask;
import de.bluecolored.bluemap.common.rendermanager.WorldRegionRenderTask;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.map.renderstate.TileInfoRegion;
import de.bluecolored.bluemap.core.map.renderstate.TileState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

@Getter
@RequiredArgsConstructor
public class TileIsUpdatedCheck implements Check {

    private final Plugin plugin;
    private final BmMap map;
    private final Vector2i position;
    private final Vector2i region;

    public TileIsUpdatedCheck(Plugin plugin, BmMap map, Vector2i position) {
        this.plugin = plugin;
        this.map = map;
        this.position = position;
        this.region = map.getWorld().getRegionGrid().getCell(position);
    }

    @Override
    public CheckResult getResult() {
        WorldRegionRenderTask regionRenderTask = new WorldRegionRenderTask(map, region);

        RenderTask current = plugin.getRenderManager().getCurrentRenderTask();
        if (current == null) return CheckResult.OK;

        return !current.contains(regionRenderTask) &&
                !plugin.getRenderManager().containsRenderTask(regionRenderTask) ?
                CheckResult.OK : CheckResult.BAD;
    }

    @Override
    public Component getFailureDescription() {
        return lines(
                format("""
                        ⚠ the region around (x:%, z:%) has pending
                        updates for map %
                        """.strip(),
                        position.getX(), position.getY(),
                        formatMap(map).color(HIGHLIGHT_COLOR)
                ),
                empty(),
                format("""
                        wait until the map finished updating
                        you can use % to see the update progress
                        """.strip(),
                        command("/bluemap").color(HIGHLIGHT_COLOR)
                ).color(BASE_COLOR)
        );
    }

}
