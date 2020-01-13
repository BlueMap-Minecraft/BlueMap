package de.bluecolored.bluemap.plugin;

import java.util.Iterator;
import java.util.UUID;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.bluecolored.bluemap.plugin.serverinterface.ServerEventListener;

public class MapUpdateHandler implements ServerEventListener {

	public Multimap<MapType, Vector2i> updateBuffer;
	
	public MapUpdateHandler() {
		updateBuffer = MultimapBuilder.hashKeys().hashSetValues().build();
	}
	
	@Override
	public void onWorldSaveToDisk(UUID world) {
		RenderManager renderManager = Plugin.getInstance().getRenderManager();
		
		synchronized (updateBuffer) {
			Iterator<MapType> iterator = updateBuffer.keys().iterator();
			while (iterator.hasNext()) {
				MapType map = iterator.next();
				if (map.getWorld().getUUID().equals(world)) {
					renderManager.createTickets(map, updateBuffer.get(map));
					iterator.remove();
				}
			}
			
		}
	}
	
	@Override
	public void onBlockChange(UUID world, Vector3i blockPos) {
		synchronized (updateBuffer) {
			updateBlock(world, blockPos);
		}
	}
	
	@Override
	public void onChunkFinishedGeneration(UUID world, Vector2i chunkPos) {
		int x = chunkPos.getX();
		int z = chunkPos.getY();
		
		// also update the chunks around, because they might be modified or not rendered yet due to finalizations
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				updateChunk(world, new Vector2i(x + dx, z + dz));
			}
		}
	}
	
	private void updateChunk(UUID world, Vector2i chunkPos) {
		Vector3i min = new Vector3i(chunkPos.getX() * 16, 0, chunkPos.getY() * 16);
		Vector3i max = min.add(15, 255, 15);
		
		Vector3i xmin = new Vector3i(min.getX(), 0, max.getY());
		Vector3i xmax = new Vector3i(max.getX(), 255, min.getY());
		
		//update all corners so we always update all tiles containing this chunk
		synchronized (updateBuffer) {
			updateBlock(world, min);
			updateBlock(world, max);
			updateBlock(world, xmin);
			updateBlock(world, xmax);
		}
	}
	
	private void updateBlock(UUID world, Vector3i pos){
		synchronized (updateBuffer) {
			for (MapType mapType : Plugin.getInstance().getMapTypes()) {
				if (mapType.getWorld().getUUID().equals(world)) {
					mapType.getWorld().invalidateChunkCache(mapType.getWorld().blockPosToChunkPos(pos));
					
					Vector2i tile = mapType.getTileRenderer().getHiresModelManager().posToTile(pos);
					updateBuffer.put(mapType, tile);
				}
			}
		}
	}
	
	public int getUpdateBufferCount() {
		return updateBuffer.size();
	}
	
	public void flushTileBuffer() {
		RenderManager renderManager = Plugin.getInstance().getRenderManager();
		
		synchronized (updateBuffer) {
			for (MapType map : updateBuffer.keySet()) {
				renderManager.createTickets(map, updateBuffer.get(map));
			}
			updateBuffer.clear();
		}
	}
	
}
