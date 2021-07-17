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
package de.bluecolored.bluemap.core.map.hires;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.util.AtomicFileHelper;
import de.bluecolored.bluemap.core.util.FileUtils;
import de.bluecolored.bluemap.core.world.Grid;
import de.bluecolored.bluemap.core.world.World;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

public class HiresModelManager {

	private final Path fileRoot;
	private final HiresModelRenderer renderer;
	private final Grid tileGrid;
	private final boolean useGzip;

	public HiresModelManager(Path fileRoot, ResourcePack resourcePack, RenderSettings renderSettings, Grid tileGrid) {
		this(fileRoot, new HiresModelRenderer(resourcePack, renderSettings), tileGrid, renderSettings.useGzipCompression());
	}

	public HiresModelManager(Path fileRoot, HiresModelRenderer renderer, Grid tileGrid, boolean useGzip) {
		this.fileRoot = fileRoot;
		this.renderer = renderer;

		this.tileGrid = tileGrid;
		
		this.useGzip = useGzip;
	}
	
	/**
	 * Renders the given world tile with the provided render-settings
	 */
	public HiresTileMeta render(World world, Vector2i tile) {
		Vector2i tileMin = tileGrid.getCellMin(tile);
		Vector2i tileMax = tileGrid.getCellMax(tile);

		Vector3i modelMin = new Vector3i(tileMin.getX(), Integer.MIN_VALUE, tileMin.getY());
		Vector3i modelMax = new Vector3i(tileMax.getX(), Integer.MAX_VALUE, tileMax.getY());

		HiresTileModel model = HiresTileModel.claimInstance();

		HiresTileMeta tileMeta = renderer.render(world, modelMin, modelMax, model);
		save(model, tile);

		HiresTileModel.recycleInstance(model);

		return tileMeta;
	}
	
	private void save(final HiresTileModel model, Vector2i tile) {
		File file = getFile(tile, useGzip);

		OutputStream os = null;
		try {
			os = AtomicFileHelper.createFilepartOutputStream(file);
			os = new BufferedOutputStream(os);
			if (useGzip) os = new GZIPOutputStream(os);

			model.writeBufferGeometryJson(os);
		} catch (IOException e){
			Logger.global.logError("Failed to save hires model: " + file, e);
		} finally {
			try {
				if (os != null) {
					os.close();
				}
			} catch (IOException e) {
				Logger.global.logError("Failed to close file: " + file, e);
			}
		}
	}
	
	/**
	 * Returns the tile-grid
	 */
	public Grid getTileGrid() {
		return tileGrid;
	}
	
	/**
	 * Returns the file for a tile
	 */
	public File getFile(Vector2i tilePos, boolean gzip){
		return FileUtils.coordsToFile(fileRoot, tilePos, "json" + (gzip ? ".gz" : ""));
	}
	
}
