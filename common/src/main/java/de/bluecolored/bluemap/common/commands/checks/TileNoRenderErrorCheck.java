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
public class TileNoRenderErrorCheck implements Check {

    private final BmMap map;
    private final Vector2i position;
    private final Vector2i tile;
    private final TileInfoRegion.TileInfo tileInfo;

    public TileNoRenderErrorCheck(BmMap map, Vector2i position) {
        this.map = map;
        this.position = position;
        this.tile = map.getHiresModelManager().getTileGrid().getCell(position);
        this.tileInfo = map.getMapTileState().get(tile.getX(), tile.getY());
    }

    @Override
    public CheckResult getResult() {
        return tileInfo.getState() != TileState.RENDER_ERROR ?
                CheckResult.OK : CheckResult.BAD;
    }

    @Override
    public Component getFailureDescription() {
        LocalDateTime failureTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(tileInfo.getRenderTime()), ZoneId.systemDefault());
        return lines(
                format("""
                        ⚠ there was an error while rendering
                        around (x:%, z:%) for map %
                        """.strip(),
                        text(position.getX()).color(HIGHLIGHT_COLOR),
                        text(position.getY()).color(HIGHLIGHT_COLOR),
                        formatMap(map).color(HIGHLIGHT_COLOR)
                ),
                empty(),
                format("""
                        check your server-logs for errors
                        around %

                        if the problem persists, you can visit bluemaps % for help
                        """.strip(),
                        text(DATE_TIME_FORMAT.format(failureTime)).color(HIGHLIGHT_COLOR),
                        text("discord")
                                .hoverEvent(text(DISCORD_LINK))
                                .clickEvent(ClickEvent.openUrl(DISCORD_LINK))
                                .color(HIGHLIGHT_COLOR)
                ).color(BASE_COLOR)
        );
    }

}
