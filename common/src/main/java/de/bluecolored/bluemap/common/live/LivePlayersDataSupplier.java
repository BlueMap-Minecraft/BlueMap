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
import de.bluecolored.bluemap.common.serverinterface.Player;
import de.bluecolored.bluemap.common.serverinterface.Server;
import de.bluecolored.bluemap.common.serverinterface.ServerWorld;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.world.World;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.function.Supplier;

public class LivePlayersDataSupplier implements Supplier<String> {

    private final Server server;
    private final World world;
    private final LivePlayerInfoTransformer playerInfoTransformer;
    private final boolean hidePlayersOnDifferentWorld;

    private transient @Nullable ServerWorld serverWorld;

    public LivePlayersDataSupplier(Server server, World world, LivePlayerInfoTransformer playerInfoTransformer, boolean hidePlayersOnDifferentWorld) {
        this.server = server;
        this.world = world;
        this.playerInfoTransformer = playerInfoTransformer;
        this.hidePlayersOnDifferentWorld = hidePlayersOnDifferentWorld;
    }

    @Override
    public String get() {
        if (serverWorld == null)
            serverWorld = server.getServerWorld(world).orElse(null);

        try (StringWriter jsonString = new StringWriter();
            JsonWriter json = new JsonWriter(jsonString)) {

            json.beginObject();
            json.name("players").beginArray();

            for (Player player : this.server.getOnlinePlayers().values()) {
                boolean isCorrectWorld = player.getWorld().equals(serverWorld);
                if (hidePlayersOnDifferentWorld && !isCorrectWorld) continue;
                LivePlayerInfo playerInfo = this.playerInfoTransformer.apply(player);
                if (playerInfo == null) continue;

                json.beginObject();

                json.name("uuid").value(playerInfo.getUuid().toString());
                json.name("name").value(playerInfo.getName());
                json.name("foreign").value(!isCorrectWorld);

                json.name("position").beginObject();
                json.name("x").value(playerInfo.getPosition().getX());
                json.name("y").value(playerInfo.getPosition().getY());
                json.name("z").value(playerInfo.getPosition().getZ());
                json.endObject();

                json.name("rotation").beginObject();
                json.name("pitch").value(playerInfo.getRotation().getX());
                json.name("yaw").value(playerInfo.getRotation().getY());
                json.name("roll").value(playerInfo.getRotation().getZ());
                json.endObject();

                json.endObject();
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
