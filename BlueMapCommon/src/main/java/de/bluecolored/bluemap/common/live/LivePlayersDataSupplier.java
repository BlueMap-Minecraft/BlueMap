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
package de.bluecolored.bluemap.common.live;

import com.google.gson.stream.JsonWriter;
import de.bluecolored.bluemap.common.config.PluginConfig;
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.serverinterface.ServerInterface;
import de.bluecolored.bluemap.core.logger.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class LivePlayersDataSupplier implements Supplier<String> {

    private final ServerInterface server;
    private final PluginConfig config;
    @Nullable private final String worldId;
    private final Predicate<UUID> playerFilter;

    public LivePlayersDataSupplier(ServerInterface server, PluginConfig config, @Nullable String worldId, Predicate<UUID> playerFilter) {
        this.server = server;
        this.config = config;
        this.worldId = worldId;
        this.playerFilter = playerFilter;
    }

    @Override
    public String get() {
        try (StringWriter jsonString = new StringWriter();
            JsonWriter json = new JsonWriter(jsonString)) {

            json.beginObject();
            json.name("players").beginArray();

            if (config.isLivePlayerMarkers()) {
                for (Player player : this.server.getOnlinePlayers()) {
                    if (!player.isOnline()) continue;

                    boolean isCorrectWorld = player.getWorld().equals(this.worldId);

                    if (config.isHideInvisible() && player.isInvisible()) continue;
                    if (config.isHideVanished() && player.isVanished()) continue;
                    if (config.isHideSneaking() && player.isSneaking()) continue;
                    if (config.getHiddenGameModes().contains(player.getGamemode().getId())) continue;
                    if (config.isHideDifferentWorld() && !isCorrectWorld) continue;
                    if (
                            player.getSkyLight() < config.getHideBelowSkyLight() &&
                            player.getBlockLight() < config.getHideBelowBlockLight()
                    ) continue;
                    if (!this.playerFilter.test(player.getUuid())) continue;

                    json.beginObject();
                    json.name("uuid").value(player.getUuid().toString());
                    json.name("name").value(player.getName().toPlainString());
                    json.name("foreign").value(!isCorrectWorld);

                    json.name("position").beginObject();
                    json.name("x").value(player.getPosition().getX());
                    json.name("y").value(player.getPosition().getY());
                    json.name("z").value(player.getPosition().getZ());
                    json.endObject();

                    json.name("rotation").beginObject();
                    json.name("pitch").value(player.getRotation().getX());
                    json.name("yaw").value(player.getRotation().getY());
                    json.name("roll").value(player.getRotation().getZ());
                    json.endObject();

                    json.endObject();
                }
            }

            json.endArray();
            json.endObject();

            json.flush();
            return jsonString.toString();
        } catch (IOException ex) {
            Logger.global.logError("Failed to write live/players json!", ex);
            return "BlueMap - Exception handling this request";
        }
    }

}
