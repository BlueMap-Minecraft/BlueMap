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
package de.bluecolored.bluemap.common.plugin;

import java.util.Iterator;
import java.util.UUID;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.bluecolored.bluemap.common.MapType;
import de.bluecolored.bluemap.common.RenderManager;
import de.bluecolored.bluemap.common.plugin.serverinterface.PlayerInterface;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.common.plugin.text.Text;

public class MapUpdateHandler implements ServerEventListener {

	public Multimap<MapType, Vector2i> updateBuffer;
	
	public MapUpdateHandler() {
		updateBuffer = MultimapBuilder.hashKeys().hashSetValues().build();
	}
	
	@Override
	public void onWorldSaveToDisk(final UUID world) {
		RenderManager renderManager = Plugin.getInstance().getRenderManager();
		
		new Thread(() -> {
			try {
				Thread.sleep(5000); // wait 5 sec before rendering so saving has finished to avoid render-errors
				
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
			} catch (InterruptedException ignore) {} 
			
		}).start();
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

	@Override
	public void onPlayerJoin(UUID playerUuid) {
		
	}

	@Override
	public void onPlayerLeave(UUID playerUuid) {
		
	}
	
	@Override
	public void onPlayerMove(UUID playerUuid) {
		
	}

	@Override
	public void onChatMessage(Text message) {
		
	}
	
}
