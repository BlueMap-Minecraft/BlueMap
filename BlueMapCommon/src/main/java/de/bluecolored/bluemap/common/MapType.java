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
package de.bluecolored.bluemap.common;

import com.flowpowered.math.vector.Vector2i;
import com.google.common.base.Preconditions;

import de.bluecolored.bluemap.core.render.TileRenderer;
import de.bluecolored.bluemap.core.render.WorldTile;
import de.bluecolored.bluemap.core.world.World;

public class MapType {

	private final String id;
	private String name;
	private World world;
	private TileRenderer tileRenderer;
	
	public MapType(String id, String name, World world, TileRenderer tileRenderer) {
		Preconditions.checkNotNull(id);
		Preconditions.checkNotNull(name);
		Preconditions.checkNotNull(world);
		Preconditions.checkNotNull(tileRenderer);
		
		this.id = id;
		this.name = name;
		this.world = world;
		this.tileRenderer = tileRenderer;
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

	public TileRenderer getTileRenderer() {
		return tileRenderer;
	}
	
	public void renderTile(Vector2i tile) {
		getTileRenderer().render(new WorldTile(getWorld(), tile));
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof MapType) {
			MapType that = (MapType) obj;
			
			return this.id.equals(that.id);
		}
		
		return false;
	}
	
}
