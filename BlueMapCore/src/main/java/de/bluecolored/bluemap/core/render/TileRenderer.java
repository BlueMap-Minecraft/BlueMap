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
import de.bluecolored.bluemap.core.util.AABB;

public class TileRenderer {
	private HiresModelManager hiresModelManager;
	private LowresModelManager lowresModelManager;
	
	public TileRenderer(HiresModelManager hiresModelManager, LowresModelManager lowresModelManager) {
		this.hiresModelManager = hiresModelManager;
		this.lowresModelManager = lowresModelManager;
	}
	
	/**
	 * Renders the provided WorldTile (only) if the world is generated
	 * @throws IOException If an IO-Exception occurs during the render
	 */
	public void render(WorldTile tile) throws IOException {
		//check if the region is generated before rendering, don't render if it's not generated 
		AABB area = hiresModelManager.getTileRegion(tile);
		if (!tile.getWorld().isAreaGenerated(area)) return;
		
		HiresModel hiresModel = hiresModelManager.render(tile);
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
	
}
