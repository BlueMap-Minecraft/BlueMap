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
package de.bluecolored.bluemap.common.plugin.serverinterface;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ServerInterface {
	
	/**
	 * Registers a ServerEventListener, every method of this interface should be called on the specified events
	 */
	void registerListener(ServerEventListener listener);
	
	/**
	 * Removes all registered listeners
	 */
	void unregisterAllListeners();
	
	/**
	 * Returns an {@link UUID} for the given world.
	 * The UUID does not need to persist over multiple runtime, but has to be always the same for this runtime.
	 * 
	 * @param worldFolder The folder of the world
	 * @return The worlds {@link UUID}
	 * @throws IOException If the uuid is read from some file and there was an exception reading this file
	 */
	UUID getUUIDForWorld(File worldFolder) throws IOException;

	/**
	 * Returns the name of the world with that UUID, the name is used in commands and should therefore be unique.<br>
	 * A return-value of <code>null</code> makes bluemap load the world-name from the level.dat and dimension-folder. 
	 * 
	 * @param worldUUID the uuid of the world
	 * @return the worlds name
	 */
	default String getWorldName(UUID worldUUID) {
		return null;
	}
	
	/**
	 * Returns the Folder containing the configurations for the plugin
	 */
	File getConfigFolder();
	
	/**
	 * Gives the possibility to override the metrics-setting in the config
	 */
	default boolean isMetricsEnabled(boolean configValue) {
		return configValue;
	}
	
	/**
	 * Returns a collection of the states of players that are currently online
	 */
	Collection<Player> getOnlinePlayers();
	
	/**
	 * Returns the state of the player with that UUID if present<br>
	 * this method is only guaranteed to return a {@link PlayerState} if the player is currently online.
	 */
	Optional<Player> getPlayer(UUID uuid);
	
	
}
