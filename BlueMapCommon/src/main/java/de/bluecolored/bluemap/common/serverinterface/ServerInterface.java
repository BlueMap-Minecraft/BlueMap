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
package de.bluecolored.bluemap.common.serverinterface;

import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.debug.DebugDump;
import de.bluecolored.bluemap.core.util.Tristate;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ServerInterface {

    @DebugDump
    MinecraftVersion getMinecraftVersion();

    /**
     * Registers a ServerEventListener, every method of this interface should be called on the specified events
     */
    void registerListener(ServerEventListener listener);

    /**
     * Removes all registered listeners
     */
    void unregisterAllListeners();

    default Optional<ServerWorld> getWorld(Path worldFolder) {
        Path normalizedWorldFolder = worldFolder.toAbsolutePath().normalize();
        return getLoadedWorlds().stream()
                .filter(world -> world.getSaveFolder().toAbsolutePath().normalize().equals(normalizedWorldFolder))
                .findAny();
    }

    @DebugDump
    Collection<ServerWorld> getLoadedWorlds();

    /**
     * Returns the Folder containing the configurations for the plugin
     */
    @DebugDump
    Path getConfigFolder();

    /**
     * Returns the folder that contains the mod-jars
     */
    @DebugDump
    Optional<Path> getModsFolder();

    /**
     * Gives the possibility to override the metrics-setting in the config
     */
    @DebugDump
    default Tristate isMetricsEnabled() {
        return Tristate.UNDEFINED;
    }

    /**
     * Returns a collection of the states of players that are currently online
     */
    @DebugDump
    Collection<Player> getOnlinePlayers();

    /**
     * Returns the state of the player with that UUID if present<br>
     * this method is only guaranteed to return a {@link Player} if the player is currently online.
     */
    Optional<Player> getPlayer(UUID uuid);

}
