package de.bluecolored.bluemap.core.world;

import com.flowpowered.math.vector.Vector2i;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public interface Region {

	/**
	 * Returns a collection of all generated chunks.<br>
	 * <i>(Be aware that the collection is not cached and recollected each time from the world-files!)</i>
	 */
	default Collection<Vector2i> listChunks(){
		return listChunks(0);
	}

	/**
	 * Returns a collection of all chunks that have been modified at or after the specified timestamp.<br>
	 * <i>(Be aware that the collection is not cached and recollected each time from the world-files!)</i>
	 */
	Collection<Vector2i> listChunks(long modifiedSince);

	default Chunk loadChunk(int chunkX, int chunkZ) throws IOException {
		return loadChunk(chunkX, chunkZ, false);
	}

	Chunk loadChunk(int chunkX, int chunkZ, boolean ignoreMissingLightData) throws IOException;

	File getRegionFile();

}
