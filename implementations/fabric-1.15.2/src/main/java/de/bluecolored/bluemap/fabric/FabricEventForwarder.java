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
package de.bluecolored.bluemap.fabric;

import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class FabricEventForwarder {

    private final FabricMod mod;
    private final Collection<ServerEventListener> eventListeners;

    public FabricEventForwarder(FabricMod mod) {
        this.mod = mod;
        this.eventListeners = new ArrayList<>(1);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            this.onPlayerJoin(server, handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            this.onPlayerLeave(server, handler.player);
        });
    }

    public synchronized void addEventListener(ServerEventListener listener) {
        this.eventListeners.add(listener);
    }

    public synchronized void removeAllListeners() {
        this.eventListeners.clear();
    }

    public synchronized void onPlayerJoin(MinecraftServer server, ServerPlayerEntity player) {
        if (this.mod.getServer() != server) return;

        UUID uuid = player.getUuid();
        for (ServerEventListener listener : eventListeners) listener.onPlayerJoin(uuid);
    }

    public synchronized void onPlayerLeave(MinecraftServer server, ServerPlayerEntity player) {
        if (this.mod.getServer() != server) return;

        UUID uuid = player.getUuid();
        for (ServerEventListener listener : eventListeners) listener.onPlayerLeave(uuid);
    }

}
