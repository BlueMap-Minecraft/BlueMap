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
package de.bluecolored.bluemap.core.render.lowres;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.flowpowered.math.vector.Vector4f;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.render.hires.HiresModel;
import de.bluecolored.bluemap.core.threejs.BufferGeometry;
import de.bluecolored.bluemap.core.util.FileUtils;

public class LowresModelManager {
	
	private Path fileRoot;
	
	private Vector2i gridSize;
	private Vector2i pointsPerHiresTile;
	
	private Map<File, CachedModel> models;
	
	private boolean useGzip;
		
	public LowresModelManager(Path fileRoot, Vector2i gridSize, Vector2i pointsPerHiresTile, boolean useGzip) {
		this.fileRoot = fileRoot;
		
		this.gridSize = gridSize;
		this.pointsPerHiresTile = pointsPerHiresTile;
		
		models = new ConcurrentHashMap<>();
		
		this.useGzip = useGzip;
	}
	
	/**
	 * Renders all points from the given highres-model onto the lowres-grid
	 */
	public void render(HiresModel hiresModel) throws IOException {
		Vector3i min = hiresModel.getBlockMin();
		Vector3i max = hiresModel.getBlockMax();
		Vector3i size = max.sub(min).add(Vector3i.ONE);
		
		Vector2i blocksPerPoint = 
				size
				.toVector2(true)
				.div(pointsPerHiresTile);
		
		Vector2i pointMin = min
				.toVector2(true)
				.toDouble()
				.div(blocksPerPoint.toDouble())
				.floor()
				.toInt();
		
		for (int tx = 0; tx < pointsPerHiresTile.getX(); tx++){
			for (int tz = 0; tz < pointsPerHiresTile.getY(); tz++){
				
				double height = 0;
				
				Vector3d color = Vector3d.ZERO;
				double colorCount = 0;
				
				for (int x = 0; x < blocksPerPoint.getX(); x++){
					for (int z = 0; z < blocksPerPoint.getY(); z++){
						
						int rx = tx * blocksPerPoint.getX() + x + min.getX();
						int rz = tz * blocksPerPoint.getY() + z + min.getZ();
						height += hiresModel.getHeight(rx, rz);
						
						Vector4f c = hiresModel.getColor(rx, rz);
						color = color.add(c.toVector3().toDouble().mul(c.getW()));
						colorCount += c.getW();
					}
				}
				
				if (colorCount > 0) color = color.div(colorCount);
				
				int count = blocksPerPoint.getX() * blocksPerPoint.getY();
				height /= count;
				
				Vector2i point = pointMin.add(tx, tz);
				update(hiresModel.getWorld(), point, (float) height, color.toFloat());
				
			}
		}
	}
	
	/**
	 * Saves all unsaved changes to the models to disk
	 */
	public synchronized void save(){
		for (CachedModel model : models.values()){
			saveModel(model);
		}
		
		tidyUpModelCache();
	}
	
	/**
	 * Updates a point on the lowresmodel-grid
	 */
	public void update(UUID world, Vector2i point, float height, Vector3f color) throws IOException {
		Vector2i tile = pointToTile(point);
		Vector2i relPoint = getPointRelativeToTile(tile, point);
		LowresModel model = getModel(world, tile);
		model.update(relPoint, height, color);
		
		if (relPoint.getX() == 0){
			Vector2i tile2 = tile.add(-1, 0);
			Vector2i relPoint2 = getPointRelativeToTile(tile2, point);
			LowresModel model2 = getModel(world, tile2);
			model2.update(relPoint2, height, color);
		}
		
		if (relPoint.getY() == 0){
			Vector2i tile2 = tile.add(0, -1);
			Vector2i relPoint2 = getPointRelativeToTile(tile2, point);
			LowresModel model2 = getModel(world, tile2);
			model2.update(relPoint2, height, color);
		}
		
		if (relPoint.getX() == 0 && relPoint.getY() == 0){
			Vector2i tile2 = tile.add(-1, -1);
			Vector2i relPoint2 = getPointRelativeToTile(tile2, point);
			LowresModel model2 = getModel(world, tile2);
			model2.update(relPoint2, height, color);
		}
	}

	/**
	 * Returns the file for a tile
	 */
	public File getFile(Vector2i tile, boolean useGzip){
		return FileUtils.coordsToFile(fileRoot, tile, "json" + (useGzip ? ".gz" : ""));
	}
	
	private LowresModel getModel(UUID world, Vector2i tile) throws IOException {
		
		File modelFile = getFile(tile, useGzip);
		CachedModel model = models.get(modelFile);

		if (model == null){
			synchronized (this) {
				model = models.get(modelFile);
				if (model == null){
					if (modelFile.exists()){

						InputStream is = new FileInputStream(modelFile);
						if (useGzip) is = new GZIPInputStream(is);
						try {
							String json = IOUtils.toString(is, StandardCharsets.UTF_8);	
							
							try {
								model = new CachedModel(world, tile, BufferGeometry.fromJson(json));
							} catch (IllegalArgumentException | IOException ex){
								Logger.global.logError("Failed to load lowres model: " + modelFile, ex);
								//gridFile.renameTo(gridFile.toPath().getParent().resolve(gridFile.getName() + ".broken").toFile());
								modelFile.delete();
							}
						} finally {
							is.close();
						}
						
					}

					if (model == null){
						model = new CachedModel(world, tile, gridSize);
					}
					
					models.put(modelFile, model);
					
					tidyUpModelCache();
				}
			}
		}
		
		return model;
	}
	
	/**
	 * This Method tidies up the model cache:<br>
	 * it saves all modified models that have not been saved for 2 minutes and<br>
	 * saves and removes the oldest models from the cache until the cache size is 10 or less.<br>
	 * <br>
	 * This method gets automatically called if the cache grows, but if you want to ensure model will be saved after 2 minutes, you could e.g call this method every second.<br> 
	 */
	public synchronized void tidyUpModelCache() {
		List<Entry<File, CachedModel>> entries = new ArrayList<>(models.size());
		entries.addAll(models.entrySet());
		entries.sort((e1, e2) -> (int) Math.signum(e1.getValue().cacheTime - e2.getValue().cacheTime));
		
		int size = entries.size();
		for (Entry<File, CachedModel> e : entries) {
			if (size > 10) {
				saveAndRemoveModel(e.getValue());
				continue;
			}
			
			if (e.getValue().getCacheTime() > 120000) {
				saveModel(e.getValue());
			}
		}
	}
	
	private synchronized void saveAndRemoveModel(CachedModel model) {
		File modelFile = getFile(model.getTile(), useGzip);
		models.remove(modelFile);
		try {
			model.save(modelFile, false, useGzip);
			//logger.logDebug("Saved and unloaded lowres tile: " + model.getTile());
		} catch (IOException ex) {
			Logger.global.logError("Failed to save and unload lowres-model: " + modelFile, ex);
		}
	}
	
	private void saveModel(CachedModel model) {
		File modelFile = getFile(model.getTile(), useGzip);
		try {
			model.save(modelFile, false, useGzip);
			//logger.logDebug("Saved lowres tile: " + model.getTile());
		} catch (IOException ex) {
			Logger.global.logError("Failed to save lowres-model: " + modelFile, ex);
		}
		
		model.resetCacheTime();
	}
	
	private Vector2i pointToTile(Vector2i point){
		return point
				.toDouble()
				.div(gridSize.toDouble())
				.floor()
				.toInt();
	}
	
	private Vector2i getPointRelativeToTile(Vector2i tile, Vector2i point){
		return point.sub(tile.mul(gridSize));
	}
	
	public Vector2i getTileSize() {
		return gridSize;
	}
	
	public Vector2i getPointsPerHiresTile() {
		return pointsPerHiresTile;
	}
	
	private class CachedModel extends LowresModel {

		private long cacheTime;
		
		public CachedModel(UUID world, Vector2i tilePos, BufferGeometry model) {
			super(world, tilePos, model);
			
			cacheTime = System.currentTimeMillis();
		}
		
		public CachedModel(UUID world, Vector2i tilePos, Vector2i gridSize) {
			super(world, tilePos, gridSize);

			cacheTime = System.currentTimeMillis();
		}
		
		public long getCacheTime() {
			return System.currentTimeMillis() - cacheTime;
		}
		
		public void resetCacheTime() {
			cacheTime = System.currentTimeMillis();
		}
		
	}
	
}
