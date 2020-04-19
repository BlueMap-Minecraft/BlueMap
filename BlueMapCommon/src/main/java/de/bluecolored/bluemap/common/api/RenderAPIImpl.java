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
package de.bluecolored.bluemap.common.api;

import java.util.UUID;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3i;

import de.bluecolored.bluemap.api.renderer.BlueMapMap;
import de.bluecolored.bluemap.api.renderer.RenderAPI;
import de.bluecolored.bluemap.common.RenderManager;

public class RenderAPIImpl implements RenderAPI {

	private BlueMapAPIImpl api;
	private RenderManager renderManager;
	
	protected RenderAPIImpl(BlueMapAPIImpl api, RenderManager renderManager) {
		this.api = api;
		this.renderManager = renderManager;
	}

	@Override
	public void render(UUID world, Vector3i blockPosition) {
		render(api.getWorldForUuid(world), blockPosition);
	}

	@Override
	public void render(String mapId, Vector3i blockPosition) {
		render(api.getMapForId(mapId), blockPosition);
	}

	@Override
	public void render(String mapId, Vector2i tile) {
		render(api.getMapForId(mapId), tile);
	}

	@Override
	public void render(BlueMapMap map, Vector2i tile) {
		BlueMapMapImpl cmap;
		if (map instanceof BlueMapMapImpl) {
			cmap = (BlueMapMapImpl) map;
		} else {
			cmap = api.getMapForId(map.getId());
		}
		
		renderManager.createTicket(cmap.getMapType(), tile);
	}

	@Override
	public int renderQueueSize() {
		return renderManager.getQueueSize();
	}

	@Override
	public int renderThreadCount() {
		return renderManager.getRenderThreadCount();
	}

	@Override
	public boolean isRunning() {
		return renderManager.isRunning();
	}

	@Override
	public void start() {
		if (!isRunning()) renderManager.start();
	}

	@Override
	public void pause() {
		renderManager.stop();
	}
	
}
