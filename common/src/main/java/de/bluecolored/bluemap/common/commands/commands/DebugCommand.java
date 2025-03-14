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
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluecommands.annotations.Argument;
import de.bluecolored.bluecommands.annotations.Command;
import de.bluecolored.bluemap.common.BlueMapService;
import de.bluecolored.bluemap.common.commands.*;
import de.bluecolored.bluemap.common.config.BlueMapConfigManager;
import de.bluecolored.bluemap.common.debug.StateDumper;
import de.bluecolored.bluemap.common.plugin.Plugin;
import de.bluecolored.bluemap.common.serverinterface.CommandSource;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.BmMap;
import de.bluecolored.bluemap.core.map.renderstate.TileInfoRegion;
import de.bluecolored.bluemap.core.world.Chunk;
import de.bluecolored.bluemap.core.world.ChunkConsumer;
import de.bluecolored.bluemap.core.world.LightData;
import de.bluecolored.bluemap.core.world.World;
import de.bluecolored.bluemap.core.world.mca.chunk.MCAChunk;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

import static de.bluecolored.bluemap.common.commands.TextFormat.*;
import static net.kyori.adventure.text.Component.text;

@RequiredArgsConstructor
@Command("debug")
public class DebugCommand {

    private final Plugin plugin;

    @Command("dump")
    @Permission("bluemap.debug.dump")
    @Unloaded
    public int dump(CommandSource source) {
        try {
            BlueMapService bluemap = plugin.getBlueMap();
            Path file = bluemap != null ?
                    bluemap.getConfig().getCoreConfig().getData().resolve("dump.json"):
                    Path.of("dump.json");
            StateDumper.global().dump(file);

            source.sendMessage(format("Dump \uD83D\uDCA9 created at: %",
                    text(BlueMapConfigManager.formatPath(file)).color(HIGHLIGHT_COLOR)
            ).color(POSITIVE_COLOR));
            return 1;
        } catch (IOException ex) {
            Logger.global.logError("Failed to create dump!", ex);
            source.sendMessage(text("Exception trying to create debug-dump! See console for details.").color(NEGATIVE_COLOR));
            return 0;
        }
    }

    @Command("world")
    @Permission("bluemap.debug.world")
    @WithWorld
    @WithPosition
    public Component block(CommandSource source, World world, Vector3d position) {
        return block(source, world, position.getFloorX(), (int) Math.floor(position.getY() - 0.1), position.getFloorZ());
    }

    @Command("world <map> <x> <y> <z>")
    @Permission("bluemap.debug.world")
    public Component block(CommandSource source, @Argument("map") BmMap map, @Argument("x") int x, @Argument("y") int y, @Argument("z") int z) {
        return block(source, map.getWorld(), x, y, z);
    }

    @Command("world <x> <y> <z>")
    @Permission("bluemap.debug.world")
    @WithWorld
    public Component block(CommandSource source, World world, @Argument("x") int x, @Argument("y") int y, @Argument("z") int z) {
        Vector2i chunkPos = world.getChunkGrid().getCell(new Vector2i(x, z));

        Chunk chunk = world.getChunkAtBlock(x, z);
        LightData lightData = chunk.getLightData(x, y, z, new LightData(0, 0));
        Vector3i spawnPoint = world.getSpawnPoint();

        return paragraph("World-Info (debug)", lines(
                item("position", format("( x: % | y: % | z: % )",
                        text(x).color(HIGHLIGHT_COLOR),
                        text(y).color(HIGHLIGHT_COLOR),
                        text(z).color(HIGHLIGHT_COLOR)
                )),
                item("block", text(chunk.getBlockState(x, y, z).toString()).color(HIGHLIGHT_COLOR)
                        .appendNewline()
                        .append(details(BASE_COLOR,
                                item("biome", chunk.getBiome(x, y, z).getKey().getFormatted()),
                                item("block-light", lightData.getBlockLight()),
                                item("sky-light", lightData.getSkyLight())
                        ))
                ),
                item("chunk", format("( x: % | z: % )",
                        text(chunkPos.getX()).color(HIGHLIGHT_COLOR),
                        text(chunkPos.getY()).color(HIGHLIGHT_COLOR)
                        )
                        .appendNewline()
                        .append(details(BASE_COLOR, TextFormat.stripNulls(
                                item("is generated", chunk.isGenerated()),
                                item("has lightdata", chunk.hasLightData()),
                                chunk instanceof MCAChunk mcaChunk ?
                                        item("data-version", mcaChunk.getDataVersion()) :
                                        null,
                                item("inhabited-time", chunk.getInhabitedTime())
                        )))
                ),
                item("world", text(world.getId()).color(HIGHLIGHT_COLOR)
                        .appendNewline()
                        .append(details(BASE_COLOR,
                                item("name", world.getName()),
                                item("min-y", world.getDimensionType().getMinY()),
                                item("height", world.getDimensionType().getHeight()),
                                item("spawn", format("( x: % | y: % | z: % )",
                                        text(spawnPoint.getX()).color(HIGHLIGHT_COLOR),
                                        text(spawnPoint.getY()).color(HIGHLIGHT_COLOR),
                                        text(spawnPoint.getZ()).color(HIGHLIGHT_COLOR)
                                ))
                        ))
                )
        ));
    }

    @Command("map")
    @Permission("bluemap.debug.map")
    @WithWorld
    @WithPosition
    public Component map(CommandSource source, World world, Vector3d position) {
        return map(source, world, position.getFloorX(), position.getFloorZ());
    }

    @Command("map <map>")
    @Permission("bluemap.debug.map")
    @WithWorld
    @WithPosition
    public Component map(CommandSource source, @Argument("map") BmMap map, World world, Vector3d position) {
        if (!map.getWorld().getId().equals(world.getId()))
            return format("Map % is not from your current world", formatMap(map)).color(NEGATIVE_COLOR);

        return map(source, map, position.getFloorX(), position.getFloorZ());
    }

    @Command("map <x> <z>")
    @Permission("bluemap.debug.map")
    @WithWorld
    public Component map(CommandSource source, World world, @Argument("x") int x, @Argument("z") int z) {
        BmMap map = plugin.getBlueMap().getMaps().values().stream()
                .filter(m -> m.getWorld().getId().equals(world.getId()))
                .findAny()
                .orElse(null);

        if (map == null)
            return text("No map found for your world").color(NEGATIVE_COLOR);

        return map(source, map, x, z);
    }

    @Command("map <map> <x> <z>")
    @Permission("bluemap.debug.map")
    public Component map(CommandSource source, @Argument("map") BmMap map, @Argument("x") int x, @Argument("z") int z) {
        Vector2i blockPos = new Vector2i(x, z);
        Vector2i chunkPos = map.getWorld().getChunkGrid().getCell(blockPos);
        Vector2i regionPos = map.getWorld().getRegionGrid().getCell(blockPos);
        Vector2i tilePos = map.getHiresModelManager().getTileGrid().getCell(blockPos);

        TileInfoRegion.TileInfo tileInfo = map.getMapTileState().get(tilePos.getX(), tilePos.getY());

        int lastChunkHash = map.getMapChunkState().get(chunkPos.getX(), chunkPos.getY());
        int currentChunkHash = 0;

        class FindHashConsumer implements ChunkConsumer.ListOnly<Chunk> {
            public int timestamp = 0;

            @Override
            public void accept(int chunkX, int chunkZ, int timestamp) {
                if (chunkPos.getX() == chunkX && chunkPos.getY() == chunkZ)
                    this.timestamp = timestamp;
            }
        }

        try {
            FindHashConsumer findHashConsumer = new FindHashConsumer();
            map.getWorld().getRegion(regionPos.getX(), regionPos.getY())
                    .iterateAllChunks(findHashConsumer);
            currentChunkHash = findHashConsumer.timestamp;
        } catch (IOException e) {
            Logger.global.logError("Failed to load chunk-hash.", e);
        }

        return paragraph("Map-Info (debug)", lines(
                item("position", format("( x: % | z: % )",
                        text(x).color(HIGHLIGHT_COLOR),
                        text(z).color(HIGHLIGHT_COLOR)
                )),
                item("chunk", format("( x: % | z: % )",
                                text(x >> 4).color(HIGHLIGHT_COLOR),
                                text(z >> 4).color(HIGHLIGHT_COLOR)
                        )
                                .appendNewline()
                                .append(details(BASE_COLOR,
                                        item("current hash", currentChunkHash),
                                        item("last rendered", lastChunkHash)
                                ))
                ),
                item("tile", format("( x: % | z: % )",
                                text(x >> 4).color(HIGHLIGHT_COLOR),
                                text(z >> 4).color(HIGHLIGHT_COLOR)
                        )
                                .appendNewline()
                                .append(details(BASE_COLOR,
                                        item("rendered", durationFormat(Instant.ofEpochSecond(tileInfo.getRenderTime()))).append(text(" ago")),
                                        item("state", tileInfo.getState().getKey().getFormatted())
                                ))
                )
        ));

    }

}
