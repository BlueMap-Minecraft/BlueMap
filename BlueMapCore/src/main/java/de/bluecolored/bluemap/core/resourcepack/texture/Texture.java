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
package de.bluecolored.bluemap.core.resourcepack.texture;

import de.bluecolored.bluemap.core.util.math.Color;

public class Texture {
	
	private final int id;
	private final String path;
	private final Color color, colorPremultiplied;
	private final boolean isHalfTransparent;
	private final String texture;
	
	protected Texture(int id, String path, Color color, boolean halfTransparent, String texture) {
		this.id = id;
		this.path = path;
		this.color = new Color().set(color).straight();
		this.colorPremultiplied = new Color().set(color).premultiplied();
		this.isHalfTransparent = halfTransparent;
		this.texture = texture;
	}
	
	public int getId() {
		return id;
	}
	
	public String getPath() {
		return path;
	}
	
	/**
	 * Returns the calculated median color of the {@link Texture}.
	 * @return The median color of this {@link Texture}
	 */
	public Color getColorStraight() {
		return color;
	}

	/**
	 * Returns the calculated median color of the {@link Texture} (premultiplied).
	 * @return The (premultiplied) median color of this {@link Texture}
	 */
	public Color getColorPremultiplied() {
		return colorPremultiplied;
	}
	
	/**
	 * Returns whether the {@link Texture} has half-transparent pixels or not.
	 * @return <code>true</code> if the {@link Texture} has half-transparent pixels, <code>false</code> if not
	 */
	public boolean isHalfTransparent() {
		return isHalfTransparent;
	}
	
	public String getTexture() {
		return texture;
	}
	
	@Override
	public int hashCode() {
		return id;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Texture) {
			return ((Texture) obj).getId() == id;
		}
		
		return false;
	}
	
}
