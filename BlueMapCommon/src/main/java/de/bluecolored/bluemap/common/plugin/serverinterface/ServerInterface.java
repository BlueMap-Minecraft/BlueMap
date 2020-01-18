package de.bluecolored.bluemap.common.plugin.serverinterface;

import java.io.File;
import java.io.IOException;
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
	 * Returns the Folder containing the configurations for the plugin
	 */
	File getConfigFolder();
	
	/**
	 * Gives the possibility to override the metrics-setting in the config
	 */
	default boolean isMetricsEnabled(boolean configValue) {
		return configValue;
	}
	
	
}
