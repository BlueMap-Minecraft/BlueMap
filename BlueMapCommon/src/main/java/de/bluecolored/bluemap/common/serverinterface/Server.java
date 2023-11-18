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

import de.bluecolored.bluemap.api.debug.DebugDump;
import de.bluecolored.bluemap.core.MinecraftVersion;
import de.bluecolored.bluemap.core.util.Tristate;
import de.bluecolored.bluemap.core.world.World;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

public interface Server {

    @DebugDump
    MinecraftVersion getMinecraftVersion();

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
     * Returns the correct {@link ServerWorld} for a {@link World} if there is any.
     */
    Optional<ServerWorld> getWorld(World world);

    /**
     * Returns the correct {@link ServerWorld} for any Object if there is any, this should return the correct ServerWorld
     * for any implementation-specific object that represent or identify a world in any way.<br>
     * Used for the API implementation.
     */
    default Optional<ServerWorld> getWorld(Object world) {
        if (world instanceof World)
            return getWorld((World) world);
        return Optional.empty();
    }

    /**
     * Returns all loaded worlds of this server.
     */
    @DebugDump
    Collection<ServerWorld> getLoadedWorlds();

    /**
     * Returns a collection of the states of players that are currently online
     */
    @DebugDump
    Collection<Player> getOnlinePlayers();

    /**
     * Registers a ServerEventListener, every method of this interface should be called on the specified events
     */
    void registerListener(ServerEventListener listener);

    /**
     * Removes all registered listeners
     */
    void unregisterAllListeners();

}
