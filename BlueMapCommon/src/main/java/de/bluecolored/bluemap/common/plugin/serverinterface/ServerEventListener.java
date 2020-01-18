package de.bluecolored.bluemap.common.plugin.serverinterface;

import java.util.UUID;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

public interface ServerEventListener {

	void onWorldSaveToDisk(UUID world);
	
	void onBlockChange(UUID world, Vector3i blockPos);
	
	void onChunkFinishedGeneration(UUID world, Vector2i chunkPos);
	
}
