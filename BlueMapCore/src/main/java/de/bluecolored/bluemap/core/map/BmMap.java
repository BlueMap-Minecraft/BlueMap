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
package de.bluecolored.bluemap.core.map;

import com.flowpowered.math.vector.Vector2i;
import de.bluecolored.bluemap.core.logger.Logger;
import de.bluecolored.bluemap.core.map.hires.HiresModel;
import de.bluecolored.bluemap.core.map.hires.HiresModelManager;
import de.bluecolored.bluemap.core.map.lowres.LowresModelManager;
import de.bluecolored.bluemap.core.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.world.Grid;
import de.bluecolored.bluemap.core.world.World;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Predicate;

public class BmMap {

	private final String id;
	private final String name;
	private final World world;
	private final Path fileRoot;

	private final MapRenderState renderState;

	private final HiresModelManager hiresModelManager;
	private final LowresModelManager lowresModelManager;

	private Predicate<Vector2i> tileFilter;

	public BmMap(String id, String name, World world, Path fileRoot, ResourcePack resourcePack, MapSettings settings) throws IOException {
		this.id = Objects.requireNonNull(id);
		this.name = Objects.requireNonNull(name);
		this.world = Objects.requireNonNull(world);
		this.fileRoot = Objects.requireNonNull(fileRoot);

		Objects.requireNonNull(resourcePack);
		Objects.requireNonNull(settings);

		this.renderState = new MapRenderState();

		File rstateFile = getRenderStateFile();
		if (rstateFile.exists()) {
			this.renderState.load(rstateFile);
		}

		this.hiresModelManager = new HiresModelManager(
				fileRoot.resolve("hires"),
				resourcePack,
				settings,
				new Grid(settings.getHiresTileSize(), 2)
		);

		this.lowresModelManager = new LowresModelManager(
				fileRoot.resolve("lowres"),
				new Vector2i(settings.getLowresPointsPerLowresTile(), settings.getLowresPointsPerLowresTile()),
				new Vector2i(settings.getLowresPointsPerHiresTile(), settings.getLowresPointsPerHiresTile()),
				settings.useGzipCompression()
		);

		this.tileFilter = t -> true;
	}

	public void renderTile(Vector2i tile) {
		if (!tileFilter.test(tile)) return;

		HiresModel hiresModel = hiresModelManager.render(world, tile);
		lowresModelManager.render(hiresModel);
	}

	public synchronized void save() {
		lowresModelManager.save();

		try {
			this.renderState.save(getRenderStateFile());
		} catch (IOException ex){
			Logger.global.logError("Failed to save render-state for map: '" + this.id + "'!", ex);
		}
	}

	public File getRenderStateFile() {
		return fileRoot.resolve(".rstate").toFile();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public World getWorld() {
		return world;
	}

	public Path getFileRoot() {
		return fileRoot;
	}

	public MapRenderState getRenderState() {
		return renderState;
	}

	public HiresModelManager getHiresModelManager() {
		return hiresModelManager;
	}

	public LowresModelManager getLowresModelManager() {
		return lowresModelManager;
	}

	public Predicate<Vector2i> getTileFilter() {
		return tileFilter;
	}

	public void setTileFilter(Predicate<Vector2i> tileFilter) {
		this.tileFilter = tileFilter;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BmMap) {
			BmMap that = (BmMap) obj;
			
			return this.id.equals(that.id);
		}
		
		return false;
	}
	
}
