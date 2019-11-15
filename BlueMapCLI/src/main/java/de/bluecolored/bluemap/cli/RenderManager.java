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
package de.bluecolored.bluemap.cli;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector2i;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.render.TileRenderer;
import de.bluecolored.bluemap.core.render.WorldTile;
import de.bluecolored.bluemap.core.world.ChunkNotGeneratedException;
import de.bluecolored.bluemap.core.world.World;

public class RenderManager extends Thread {

	private World world;
	private TileRenderer tileRenderer;
	private Deque<Vector2i> tilesToRender;
	
	private int tileCount;
	private long startTime = -1;
	private int renderedTiles = 0;
	
	private Thread[] threads;
	
	private Runnable onFinished;
	
	public RenderManager(World world, TileRenderer tileRenderer, Collection<Vector2i> tilesToRender, int threadCount) {
		this.world = world;
		this.tileRenderer = tileRenderer;
		
		//Sort the chunks to opimize the chunk-cache usage of MCAWorld and generate the world in a nicer order, so you can see the first results early in the web-map during render
		Vector2d sortGridSize = new Vector2d(20, 20).div(tileRenderer.getHiresModelManager().getTileSize().toDouble().div(16)).ceil().max(1, 1); //Find a good grid size to match the MCAWorlds chunk-cache size of 500
		ArrayList<Vector2i> sortedTiles = new ArrayList<>(tilesToRender);
		sortedTiles.sort((v1, v2) -> {
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
		
		this.tilesToRender = new ArrayDeque<>(sortedTiles);
		
		this.tileCount = this.tilesToRender.size();
		this.threads = new Thread[threadCount];
		
		
	}
	
	public synchronized void start(Runnable onFinished) {
		this.onFinished = onFinished;
		
		start();
	}
	
	@Override
	public void run() {
		this.startTime = System.currentTimeMillis();
		
		for (int i = 0; i < threads.length; i++) {
			if (threads[i] != null) threads[i].interrupt();
			
			threads[i] = new Thread(this::renderThread);
			threads[i].start();
		}
		
		long lastLogUpdate = startTime;
		long lastSave = startTime;
		
		while (!Thread.interrupted()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				break;
			}
			
			boolean stillRendering = false;
			for (Thread t : threads) {
				if (t.isAlive()) {
					stillRendering = true;
					break;
				}
			}
			if (!stillRendering) break;
			
			long now = System.currentTimeMillis();
			if (lastLogUpdate < now - 10000) { // print update all 10 seconds
				lastLogUpdate = now;
				
				long time = now - startTime;
				String durationString = DurationFormatUtils.formatDurationWords(time, true, true);
				double pct = (double)renderedTiles / (double)tileCount;
				
				long ert = (long)((time / pct) * (1d - pct));
				String ertDurationString = DurationFormatUtils.formatDurationWords(ert, true, true);
				
				Logger.global.logInfo("Rendered " + renderedTiles + " of " + tileCount + " tiles in " + durationString);
				Logger.global.logInfo(GenericMath.round(pct * 100, 3) + "% | Estimated remaining time: " + ertDurationString);
			}
			
			if (lastSave < now - 5 * 60000) { // save every 5 minutes
				lastSave = now;
				tileRenderer.save();
			}
		}
		
		tileRenderer.save();
		
		onFinished.run();
	}
	
	private void renderThread() {
		Vector2i tilePos;
		
		while (!Thread.interrupted()) {
			synchronized (tilesToRender) {
				if (tilesToRender.isEmpty()) break;
				tilePos = tilesToRender.poll();
			}
			
			WorldTile tile = new WorldTile(world, tilePos);
			try {
				tileRenderer.render(tile);
			} catch (IOException e) {
				Logger.global.logError("Failed to render tile " + tilePos, e);
			} catch (ChunkNotGeneratedException e) {}
			
			renderedTiles++;
		}
	}
	
}
