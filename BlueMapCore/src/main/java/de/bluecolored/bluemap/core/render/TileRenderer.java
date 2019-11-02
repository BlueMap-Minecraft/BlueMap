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
package de.bluecolored.bluemap.core.render;

import java.io.IOException;

import de.bluecolored.bluemap.core.render.hires.HiresModel;
import de.bluecolored.bluemap.core.render.hires.HiresModelManager;
import de.bluecolored.bluemap.core.render.lowres.LowresModelManager;
import de.bluecolored.bluemap.core.world.ChunkNotGeneratedException;

public class TileRenderer {
	private HiresModelManager hiresModelManager;
	private LowresModelManager lowresModelManager;
	private RenderSettings renderSettings;
	
	public TileRenderer(HiresModelManager hiresModelManager, LowresModelManager lowresModelManager, RenderSettings renderSettings) {
		this.hiresModelManager = hiresModelManager;
		this.lowresModelManager = lowresModelManager;
		this.renderSettings = renderSettings.copy();
	}
	
	/**
	 * Renders the provided WorldTile
	 * @throws ChunkNotGeneratedException if that WorldTile's WorldChunk is not fully generated
	 * @throws IOException if a lowres-model that needs to be updated could not be loaded
	 */
	public void render(WorldTile tile) throws IOException, ChunkNotGeneratedException {
		HiresModel hiresModel = hiresModelManager.render(tile, renderSettings);
		lowresModelManager.render(hiresModel);
	}
	
	/**
	 * Saves changes to disk
	 */
	public void save(){
		lowresModelManager.save();
	}
	
	public HiresModelManager getHiresModelManager() {
		return hiresModelManager;
	}
	
	public LowresModelManager getLowresModelManager() {
		return lowresModelManager;
	}
	
	public RenderSettings getRenderSettings() {
		return renderSettings;
	}
	
}
