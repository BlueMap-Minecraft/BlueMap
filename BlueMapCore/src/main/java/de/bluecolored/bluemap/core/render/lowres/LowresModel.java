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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;

import de.bluecolored.bluemap.core.threejs.BufferGeometry;
import de.bluecolored.bluemap.core.util.FileUtils;
import de.bluecolored.bluemap.core.util.MathUtils;
import de.bluecolored.bluemap.core.util.ModelUtils;

public class LowresModel {
	
	private UUID world;
	private Vector2i tilePos;
	private BufferGeometry model;
	
	private Map<Vector2i, LowresPoint> changes;
	
	private boolean hasUnsavedChanges;
	
	private final Object 
		fileLock = new Object(), 
		modelLock = new Object();
	
	public LowresModel(UUID world, Vector2i tilePos, Vector2i gridSize) {
		this(
			world,
			tilePos, 
			ModelUtils.makeGrid(gridSize).toBufferGeometry()
		);
	}
	
	public LowresModel(UUID world, Vector2i tilePos, BufferGeometry model) {
		this.world = world;
		this.tilePos = tilePos;
		this.model = model;
		
		this.changes = new ConcurrentHashMap<>();
		
		this.hasUnsavedChanges = true;
	}
	
	/**
	 * Searches for all vertices at that point on the grid-model and change the height and color.<br>
	 * <br>
	 * <i>
	 * Implementation note:<br>
	 * The vertex x, z -coords are rounded, so we can compare them using == without worrying about floating point rounding differences.<br>
	 * </i>
	 */
	public void update(Vector2i point, float height, Vector3f color){
		changes.put(point, new LowresPoint(height, color));
		this.hasUnsavedChanges = true;
	}
	
	/**
	 * Saves this model to its file
	 * @param force if this is false, the model is only saved if it has any changes
	 */
	public void save(File file, boolean force) throws IOException {
		if (!force && !hasUnsavedChanges) return;
		this.hasUnsavedChanges = false;

		flush();
		
		String json;
		synchronized (modelLock) {
			json = model.toJson();
		}
		
		synchronized (fileLock) {
			if (!file.exists()){
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			
			try {
				FileUtils.waitForFile(file, 10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new IOException("Failed to get write-access to file: " + file, e);
			}

			FileOutputStream fos = new FileOutputStream(file);
			GZIPOutputStream zos = new GZIPOutputStream(fos);
			OutputStreamWriter osw = new OutputStreamWriter(zos, StandardCharsets.UTF_8);
			try (
				PrintWriter pw = new PrintWriter(osw);
			){
				pw.print(json);
			}
		
		}
	}
	
	public void flush(){
		if (changes.isEmpty()) return;

		synchronized (modelLock) {
			if (changes.isEmpty()) return;
			
			Map<Vector2i, LowresPoint> points = changes;
			changes = new HashMap<>();
			
			int vertexCount = model.position.length / 3;
			
			for (int i = 0; i < vertexCount; i++){
				int j = i * 3;
				int px = Math.round(model.position[j + 0]);
				int pz = Math.round(model.position[j + 2]);
				
				Vector2i p = new Vector2i(px, pz);
				
				LowresPoint lrp = points.get(p);
				if (lrp == null) continue;
	
				model.position[j + 1] = lrp.height;
				
				model.color[j + 0] = lrp.color.getX();
				model.color[j + 1] = lrp.color.getY();
				model.color[j + 2] = lrp.color.getZ();
				
				//recalculate normals
				int f = Math.floorDiv(i, 3) * 3 * 3;
				Vector3f p1 = new Vector3f(model.position[f + 0], model.position[f + 1], model.position[f + 2]);
				Vector3f p2 = new Vector3f(model.position[f + 3], model.position[f + 4], model.position[f + 5]);
				Vector3f p3 = new Vector3f(model.position[f + 6], model.position[f + 7], model.position[f + 8]);
				
				Vector3f n = MathUtils.getSurfaceNormal(p1, p2, p3);
				
				model.normal[f + 0] = n.getX();  model.normal[f + 1] = n.getY();  model.normal[f + 2] = n.getZ();
				model.normal[f + 3] = n.getX();  model.normal[f + 4] = n.getY();  model.normal[f + 5] = n.getZ();
				model.normal[f + 6] = n.getX();  model.normal[f + 7] = n.getY();  model.normal[f + 8] = n.getZ();
			}
		}
	}
	
	public BufferGeometry getBufferGeometry(){
		flush();
		return model;
	}
	
	public UUID getWorld(){
		return world;
	}
	
	public Vector2i getTile(){
		return tilePos;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(world, tilePos);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LowresModel){
			LowresModel other = (LowresModel) obj;
			if (!other.world.equals(world)) return false;
			if (other.tilePos.equals(tilePos)) return true;
		}
		
		return false;
	}
	
	/**
	 * a point on this lowres-model-grid
	 */
	public class LowresPoint {
		private float height;
		private Vector3f color;
		
		public LowresPoint(float height, Vector3f color) {
			this.height = height;
			this.color = color;
		}
		
		public LowresPoint add(LowresPoint other){
			float newHeight = height + other.height;
			Vector3f newColor = color.add(other.color);
			return new LowresPoint(newHeight, newColor);
		}
		
		public LowresPoint div(float divisor){
			float newHeight = height / divisor;
			Vector3f newColor = color.div(divisor);
			return new LowresPoint(newHeight, newColor);
		}
	}
	
}
