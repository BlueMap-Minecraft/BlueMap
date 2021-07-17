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
package de.bluecolored.bluemap.core.resourcepack;

import com.flowpowered.math.vector.Vector2f;
import de.bluecolored.bluemap.core.util.math.MatrixM3f;

public class TransformedBlockModelResource {

	private final Vector2f rotation;
	private final boolean uvLock;
	private final BlockModelResource model;

	private final boolean hasRotation;
	private final MatrixM3f rotationMatrix;

	public TransformedBlockModelResource(Vector2f rotation, boolean uvLock, BlockModelResource model) {
		this.model = model;
		this.rotation = rotation;
		this.uvLock = uvLock;

		this.hasRotation = !rotation.equals(Vector2f.ZERO);
		this.rotationMatrix = new MatrixM3f()
				.rotate(
						-rotation.getX(),
						-rotation.getY(),
						0
				);
	}

	public Vector2f getRotation() {
		return rotation;
	}

	public boolean hasRotation() {
		return hasRotation;
	}

	public MatrixM3f getRotationMatrix() {
		return rotationMatrix;
	}

	public boolean isUVLock() {
		return uvLock;
	}
	
	public BlockModelResource getModel() {
		return model;
	}
	
}
