package de.bluecolored.bluemap.plugin.serverinterface;

import java.io.File;
import java.util.concurrent.ExecutorService;

import de.bluecolored.bluemap.core.world.World;

public interface ServerInterface {
	
	void registerListener(ServerEventListener listener);
	
	void unregisterAllListeners();
	
	World createWorld(File worldFolder);
	
	ExecutorService getSyncExecutorService();
	
	ExecutorService getAsyncExecutorService();

	File getConfigFolder();
	
	boolean isMetricsEnabled(boolean configValue);
	
}
