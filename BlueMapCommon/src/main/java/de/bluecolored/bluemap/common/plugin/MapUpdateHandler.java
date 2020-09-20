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

import java.util.Collection;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import de.bluecolored.bluemap.common.MapType;
import de.bluecolored.bluemap.common.RenderManager;
import de.bluecolored.bluemap.common.plugin.serverinterface.ServerEventListener;
import de.bluecolored.bluemap.core.world.World;

public class MapUpdateHandler implements ServerEventListener {

	private Plugin plugin;
	private Multimap<UUID, Vector2i> updateBuffer;
	private Timer flushTimer;
	
	public MapUpdateHandler(Plugin plugin) {
		this.plugin = plugin;
		updateBuffer = MultimapBuilder.hashKeys().hashSetValues().build();
		
		flushTimer = new Timer("MapUpdateHandlerTimer", true);
	}
	
	@Override
	public void onWorldSaveToDisk(final UUID world) {
		
		// wait 5 sec before rendering so saving has finished
		flushTimer.schedule(new TimerTask() {
			@Override public void run() { flushUpdateBufferForWorld(world); }
		}, 5000);
		
	}
	
	@Override
	public void onChunkSaveToDisk(final UUID world, final Vector2i chunkPos) {
		
		// wait 5 sec before rendering so saving has finished
		flushTimer.schedule(new TimerTask() {
			@Override public void run() { flushUpdateBufferForChunk(world, chunkPos); }
		}, 5000);
		
	}
	
	@Override
	public void onBlockChange(UUID world, Vector3i blockPos) {
		updateBlock(world, blockPos);
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
	
	private void updateChunk(UUID worldUUID, Vector2i chunkPos) {
		World world = plugin.getWorld(worldUUID);
		if (world == null) return;
		
		synchronized (updateBuffer) {
			updateBuffer.put(worldUUID, chunkPos);
		}
	}
	
	private void updateBlock(UUID worldUUID, Vector3i pos){
		World world = plugin.getWorld(worldUUID);
		if (world == null) return;
		
		synchronized (updateBuffer) {
			Vector2i chunkPos = world.blockPosToChunkPos(pos);
			updateBuffer.put(worldUUID, chunkPos);
		}
	}
	
	public int getUpdateBufferCount() {
		return updateBuffer.size();
	}
	
	public void flushUpdateBuffer() {
		RenderManager renderManager = plugin.getRenderManager();
		
		synchronized (updateBuffer) {
			for (MapType map : plugin.getMapTypes()) {
				Collection<Vector2i> chunks = updateBuffer.get(map.getWorld().getUUID());
				Collection<Vector2i> tiles = new HashSet<>(chunks.size() * 2);
				
				for (Vector2i chunk : chunks) {
					Vector3i min = new Vector3i(chunk.getX() * 16, 0, chunk.getY() * 16);
					Vector3i max = min.add(15, 255, 15);
					
					Vector3i xmin = new Vector3i(min.getX(), 0, max.getY());
					Vector3i xmax = new Vector3i(max.getX(), 255, min.getY());
					
					tiles.add(map.getTileRenderer().getHiresModelManager().posToTile(min));
					tiles.add(map.getTileRenderer().getHiresModelManager().posToTile(max));
					tiles.add(map.getTileRenderer().getHiresModelManager().posToTile(xmin));
					tiles.add(map.getTileRenderer().getHiresModelManager().posToTile(xmax));
				}
				
				//invalidate caches of updated chunks
				for (Vector2i chunk : chunks) {
					map.getWorld().invalidateChunkCache(chunk);
				}

				//create render-tickets
				renderManager.createTickets(map, tiles);
			}
			
			updateBuffer.clear();
		}
	}
	
	public void flushUpdateBufferForWorld(UUID world) {
		RenderManager renderManager = plugin.getRenderManager();
		
		synchronized (updateBuffer) {
			for (MapType map : plugin.getMapTypes()) {
				if (!map.getWorld().getUUID().equals(world)) continue;

				Collection<Vector2i> chunks = updateBuffer.get(world);
				Collection<Vector2i> tiles = new HashSet<>(chunks.size() * 2);
				
				for (Vector2i chunk : chunks) {
					Vector3i min = new Vector3i(chunk.getX() * 16, 0, chunk.getY() * 16);
					Vector3i max = min.add(15, 255, 15);
					
					Vector3i xmin = new Vector3i(min.getX(), 0, max.getY());
					Vector3i xmax = new Vector3i(max.getX(), 255, min.getY());
					
					tiles.add(map.getTileRenderer().getHiresModelManager().posToTile(min));
					tiles.add(map.getTileRenderer().getHiresModelManager().posToTile(max));
					tiles.add(map.getTileRenderer().getHiresModelManager().posToTile(xmin));
					tiles.add(map.getTileRenderer().getHiresModelManager().posToTile(xmax));
				}
				
				//invalidate caches of updated chunks
				for (Vector2i chunk : chunks) {
					map.getWorld().invalidateChunkCache(chunk);
				}
				
				//create render-tickets
				renderManager.createTickets(map, tiles);
			}
			
			updateBuffer.removeAll(world);
		}
	}
	
	public void flushUpdateBufferForChunk(UUID world, Vector2i chunkPos) {
		RenderManager renderManager = plugin.getRenderManager();
		
		synchronized (updateBuffer) {
			if (!updateBuffer.containsEntry(world, chunkPos)) return;
			
			for (MapType map : plugin.getMapTypes()) {
				if (!map.getWorld().getUUID().equals(world)) continue;

				Collection<Vector2i> tiles = new HashSet<>(4);
				
				Vector3i min = new Vector3i(chunkPos.getX() * 16, 0, chunkPos.getY() * 16);
				Vector3i max = min.add(15, 255, 15);
				
				Vector3i xmin = new Vector3i(min.getX(), 0, max.getY());
				Vector3i xmax = new Vector3i(max.getX(), 255, min.getY());
				
				tiles.add(map.getTileRenderer().getHiresModelManager().posToTile(min));
				tiles.add(map.getTileRenderer().getHiresModelManager().posToTile(max));
				tiles.add(map.getTileRenderer().getHiresModelManager().posToTile(xmin));
				tiles.add(map.getTileRenderer().getHiresModelManager().posToTile(xmax));
				
				//invalidate caches of updated chunk
				map.getWorld().invalidateChunkCache(chunkPos);

				//create render-tickets
				renderManager.createTickets(map, tiles);
			}
			
			updateBuffer.remove(world, chunkPos);
		}
	}
	
}
