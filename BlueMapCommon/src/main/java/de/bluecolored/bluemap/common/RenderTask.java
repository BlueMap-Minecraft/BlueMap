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
package de.bluecolored.bluemap.common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;

public class RenderTask {

	private final UUID uuid;
	private String name;
	
	private final MapType mapType;
	private Deque<Vector2i> renderTiles;

	private long firstTileTime;
	private long additionalRunTime;
	private int renderedTiles;
	
	public RenderTask(String name, MapType mapType) {
		this.uuid = UUID.randomUUID();
		this.name = name;
		this.mapType = mapType;
		this.renderTiles = new ArrayDeque<>();
		this.firstTileTime = -1;
		this.additionalRunTime = 0;
		this.renderedTiles = 0;
	}
	
	public void optimizeQueue() {
		//Find a good grid size to match the MCAWorlds chunk-cache size of 500
		Vector2d sortGridSize = new Vector2d(20, 20).div(mapType.getTileRenderer().getHiresModelManager().getTileSize().toDouble().div(16)).ceil().max(1, 1);
		
		synchronized (renderTiles) {
			ArrayList<Vector2i> tileList = new ArrayList<>(renderTiles);
			tileList.sort((v1, v2) -> {
				Vector2i v1SortGridPos = v1.toDouble().div(sortGridSize).floor().toInt();
				Vector2i v2SortGridPos = v2.toDouble().div(sortGridSize).floor().toInt();
				
				if (v1SortGridPos != v2SortGridPos){
					int v1Dist = v1SortGridPos.distanceSquared(Vector2i.ZERO);
					int v2Dist = v2SortGridPos.distanceSquared(Vector2i.ZERO);
					
					if (v1Dist < v2Dist) return -1;
					if (v1Dist > v2Dist) return 1;

					if (v1SortGridPos.getY() < v2SortGridPos.getY()) return -1;
					if (v1SortGridPos.getY() > v2SortGridPos.getY()) return 1;
					if (v1SortGridPos.getX() < v2SortGridPos.getX()) return -1;
					if (v1SortGridPos.getX() > v2SortGridPos.getX()) return 1;
				}
				
				if (v1.getY() < v2.getY()) return -1;
				if (v1.getY() > v2.getY()) return 1;
				if (v1.getX() < v2.getX()) return -1;
				if (v1.getX() > v2.getX()) return 1;
				
				return 0;
			});
			
			renderTiles.clear();
			for (Vector2i tile : tileList) {
				renderTiles.add(tile);
			}
		}
	}
	
	public void addTile(Vector2i tile) {
		synchronized (renderTiles) {
			renderTiles.add(tile);
		}
	}
	
	public void addTiles(Collection<Vector2i> tiles) {
		synchronized (renderTiles) {
			renderTiles.addAll(tiles);
		}
	}
	
	public RenderTicket poll() {
		synchronized (renderTiles) {
			Vector2i tile = renderTiles.poll();
			if (tile != null) {
				renderedTiles++;
				if (firstTileTime < 0) firstTileTime = System.currentTimeMillis();
				return new RenderTicket(mapType, tile);
			} else {
				return null;
			}
		}
	}
	
	/**
	 * Pauses the render-time counter.
	 * So if the rendering gets paused, the statistics remain correct.
	 * It will resume as soon as a new ticket gets polled
	 */
	public void pause() {
		if (firstTileTime < 0) return;
		
		synchronized (renderTiles) {
			additionalRunTime += System.currentTimeMillis() - firstTileTime;
			firstTileTime = -1;	
		}
	}
	
	public long getActiveTime() {
		if (firstTileTime < 0) return additionalRunTime;
		return (System.currentTimeMillis() - firstTileTime) + additionalRunTime;
	}
	
	public UUID getUuid() {
		return uuid;
	}
	
	public String getName() {
		return name;
	}
	
	public MapType getMapType() {
		return mapType;
	}
	
	public int getRenderedTileCount() {
		return renderedTiles;
	}
	
	public int getRemainingTileCount() {
		return renderTiles.size();
	}
	
	public boolean isFinished() {
		return renderTiles.isEmpty();
	}
	
	public void write(DataOutputStream out) throws IOException {
		synchronized (renderTiles) {
			pause();
			
			out.writeUTF(name);
			out.writeUTF(mapType.getId());
			
			out.writeLong(additionalRunTime);
			out.writeInt(renderedTiles);
		
			out.writeInt(renderTiles.size());
			for (Vector2i tile : renderTiles) {
				out.writeInt(tile.getX());
				out.writeInt(tile.getY());
			}
		}
	}
	
	public static RenderTask read(DataInputStream in, Collection<MapType> mapTypes) throws IOException {
		String name = in.readUTF();
		String mapId = in.readUTF();
		
		MapType mapType = null;
		for (MapType map : mapTypes) {
			if (map.getId().equals(mapId)) {
				mapType = map;
				break;
			}
		}
		if (mapType == null) throw new IOException("Map type with id '" + mapId + "' does not exist!");
		
		RenderTask task = new RenderTask(name, mapType);
		
		task.additionalRunTime = in.readLong();
		task.renderedTiles = in.readInt();
		
		int tileCount = in.readInt();
		List<Vector2i> tiles = new ArrayList<>();
		for (int i = 0; i < tileCount; i++) {
			int x = in.readInt();
			int y = in.readInt();
			Vector2i tile = new Vector2i(x, y);
			tiles.add(tile);
		}
		
		task.addTiles(tiles);
		
		return task;
	}
	
}
