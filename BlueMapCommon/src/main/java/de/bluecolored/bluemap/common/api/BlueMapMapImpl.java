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

import com.flowpowered.math.vector.Vector2i;

import de.bluecolored.bluemap.api.renderer.BlueMapMap;
import de.bluecolored.bluemap.common.MapType;

public class BlueMapMapImpl implements BlueMapMap {
	
	private BlueMapAPIImpl api;
	private MapType delegate;

	protected BlueMapMapImpl(BlueMapAPIImpl api, MapType delegate) {
		this.api = api;
		this.delegate = delegate;
	}
	
	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public String getName() {
		return delegate.getName();
	}

	@Override
	public BlueMapWorldImpl getWorld() {
		return api.getWorldForUuid(delegate.getWorld().getUUID());
	}

	@Override
	public Vector2i getTileSize() {
		return delegate.getTileRenderer().getHiresModelManager().getTileSize();
	}

	@Override
	public Vector2i getTileOffset() {
		return delegate.getTileRenderer().getHiresModelManager().getGridOrigin();
	}

	public MapType getMapType() {
		return delegate;
	}
	
}
