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
package de.bluecolored.bluemap.forge;

import de.bluecolored.bluemap.common.serverinterface.ServerEventListener;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public class ForgeEventForwarder  {

    private final Collection<ServerEventListener> eventListeners;

    public ForgeEventForwarder() {
        this.eventListeners = new ArrayList<>(1);
    }

    public synchronized void addEventListener(ServerEventListener listener) {
        this.eventListeners.add(listener);
    }

    public synchronized void removeAllListeners() {
        this.eventListeners.clear();
    }

    @SubscribeEvent
    public synchronized void onPlayerJoin(PlayerLoggedInEvent evt) {
        UUID uuid = evt.getEntity().getUUID();
        for (ServerEventListener listener : eventListeners) listener.onPlayerJoin(uuid);
    }

    @SubscribeEvent
    public synchronized void onPlayerLeave(PlayerLoggedOutEvent evt) {
        UUID uuid = evt.getEntity().getUUID();
        for (ServerEventListener listener : eventListeners) listener.onPlayerLeave(uuid);
    }

}
