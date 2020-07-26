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
package de.bluecolored.bluemap.core.render.hires;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.render.RenderSettings;
import de.bluecolored.bluemap.core.render.WorldTile;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.AABB;
import de.bluecolored.bluemap.core.util.FileUtils;

public class HiresModelManager {

	private Path fileRoot;
	private HiresModelRenderer renderer;
	
	private Vector2i tileSize;
	private Vector2i gridOrigin;
	
	private boolean useGzip;
	
	public HiresModelManager(Path fileRoot, ResourcePack resourcePack, RenderSettings renderSettings, Vector2i tileSize) {
		this(fileRoot, new HiresModelRenderer(resourcePack, renderSettings), tileSize, new Vector2i(2, 2), renderSettings.useGzipCompression());
	}
	
	public HiresModelManager(Path fileRoot, HiresModelRenderer renderer, Vector2i tileSize, Vector2i gridOrigin, boolean useGzip) {
		this.fileRoot = fileRoot;
		this.renderer = renderer;
		
		this.tileSize = tileSize;
		this.gridOrigin = gridOrigin;
		
		this.useGzip = useGzip;
	}
	
	/**
	 * Renders the given world tile with the provided render-settings
	 */
	public HiresModel render(WorldTile tile) {
		HiresModel model = renderer.render(tile, getTileRegion(tile));
		save(model);
		return model;
	}
	
	private void save(final HiresModel model) {
		final byte[] modelJson = model.toBufferGeometry().toBinary();
		save(model, modelJson);
	}
	
	private void save(HiresModel model, byte[] binary){
		File file = getFile(model.getTile(), useGzip);
		
		try {
			if (!file.exists()){
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
	
			OutputStream os = new FileOutputStream(file);
			if (useGzip) os = new GZIPOutputStream(os);
			os.write(binary);
			os.close();

			//logger.logDebug("Saved hires model: " + model.getTile()); 
		} catch (IOException e){
			Logger.global.logError("Failed to save hires model: " + file, e);
		}
	}
	
	/**
	 * Returns all tiles that the provided chunks are intersecting
	 */
	public Collection<Vector2i> getTilesForChunks(Iterable<Vector2i> chunks){
		Set<Vector2i> tiles = new HashSet<>();
		for (Vector2i chunk : chunks) {
			Vector3i minBlockPos = new Vector3i(chunk.getX() * 16, 0, chunk.getY() * 16);
			
			//loop to cover the case that a tile is smaller then a chunk, should normally only add one tile (at 0, 0)
			for (int x = 0; x < 15; x += getTileSize().getX()) {
				for (int z = 0; z < 15; z += getTileSize().getY()) {
					tiles.add(posToTile(minBlockPos.add(x, 0, z)));
				}
			}
			
			tiles.add(posToTile(minBlockPos.add(0, 0, 15)));
			tiles.add(posToTile(minBlockPos.add(15, 0, 0)));
			tiles.add(posToTile(minBlockPos.add(15, 0, 15)));
		}
		
		return tiles;
	}
	
	/**
	 * Returns the region of blocks that a tile includes
	 */
	public AABB getTileRegion(WorldTile tile) {
		Vector3i min = new Vector3i(
				tile.getTile().getX() * tileSize.getX() + gridOrigin.getX(), 
				0, 
				tile.getTile().getY() * tileSize.getY() + gridOrigin.getY()
			);
		Vector3i max = min.add(
				tileSize.getX() - 1,
				255,
				tileSize.getY() - 1
			);
		return new AABB(min, max);
	}
	
	/**
	 * Returns the tile-size
	 */
	public Vector2i getTileSize() {
		return tileSize;
	}

	/**
	 * Returns the grid-origin
	 */
	public Vector2i getGridOrigin() {
		return gridOrigin;
	}
	
	/**
	 * Converts a block-position to a map-tile-coordinate
	 */
	public Vector2i posToTile(Vector3i pos){
		return posToTile(pos.toDouble());
	}

	/**
	 * Converts a block-position to a map-tile-coordinate
	 */
	public Vector2i posToTile(Vector3d pos){
		pos = pos.sub(new Vector3d(gridOrigin.getX(), 0.0, gridOrigin.getY()));
		return Vector2i.from(
				(int) Math.floor(pos.getX() / getTileSize().getX()),
				(int) Math.floor(pos.getZ() / getTileSize().getY())  
			);
	}
	
	/**
	 * Returns the file for a tile
	 */
	public File getFile(Vector2i tilePos, boolean gzip){
		return FileUtils.coordsToFile(fileRoot, tilePos, "bin" + (gzip ? ".gz" : ""));
	}
	
}
