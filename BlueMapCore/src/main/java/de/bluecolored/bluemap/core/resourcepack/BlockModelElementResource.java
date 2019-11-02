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

import com.flowpowered.math.vector.Vector3f;

import de.bluecolored.bluemap.core.util.Axis;
import de.bluecolored.bluemap.core.util.ConfigUtil;
import ninja.leaping.configurate.ConfigurationNode;

public class BlockModelElementResource {
	
	private BlockModelResource model;

	private Vector3f from, to;
	
	private Vector3f rotOrigin;
	private Axis rotAxis;
	private float rotAngle;
	private boolean rotRescale;
	
	private boolean shade;
	
	private BlockModelElementFaceResource down, up, north, south, west, east;
	
	protected BlockModelElementResource(BlockModelResource model, ConfigurationNode declaration) throws InvalidResourceDeclarationException {
		this.model = model;
		
		try {
			this.from = ConfigUtil.readVector3f(declaration.getNode("from"));
			this.to = ConfigUtil.readVector3f(declaration.getNode("to"));
			
			this.rotAngle = 0f;
			ConfigurationNode rotation = declaration.getNode("rotation");
			if (!rotation.isVirtual()){
				this.rotOrigin = ConfigUtil.readVector3f(rotation.getNode("origin"));
				this.rotAxis = Axis.fromString(rotation.getNode("axis").getString());
				this.rotAngle = rotation.getNode("angle").getFloat();
				this.rotRescale = rotation.getNode("rescale").getBoolean(false);
			}
			
			this.shade = declaration.getNode("shade").getBoolean(true);
			
			ConfigurationNode faces = declaration.getNode("faces");
			this.down = loadFace(faces.getNode("down"));
			this.up = loadFace(faces.getNode("up"));
			this.north = loadFace(faces.getNode("north"));
			this.south = loadFace(faces.getNode("south"));
			this.west = loadFace(faces.getNode("west"));
			this.east = loadFace(faces.getNode("east"));
			
		} catch (NullPointerException e){
			throw new InvalidResourceDeclarationException(e);
		}
	}
	
	private BlockModelElementFaceResource loadFace(ConfigurationNode faceNode) throws InvalidResourceDeclarationException {
		if (faceNode.isVirtual()) return null;
		return new BlockModelElementFaceResource(this, faceNode);
	}
	
	public BlockModelResource getModel(){
		return model;
	}

	public Vector3f getFrom() {
		return from;
	}

	public Vector3f getTo() {
		return to;
	}

	public boolean isRotation(){
		return rotAngle != 0f;
	}
	
	public Vector3f getRotationOrigin() {
		return rotOrigin;
	}

	public Axis getRotationAxis() {
		return rotAxis;
	}

	public float getRotationAngle() {
		return rotAngle;
	}

	public boolean isRotationRescale() {
		return rotRescale;
	}

	public boolean isShade() {
		return shade;
	}

	public BlockModelElementFaceResource getDownFace() {
		return down;
	}

	public BlockModelElementFaceResource getUpFace() {
		return up;
	}

	public BlockModelElementFaceResource getNorthFace() {
		return north;
	}

	public BlockModelElementFaceResource getSouthFace() {
		return south;
	}

	public BlockModelElementFaceResource getWestFace() {
		return west;
	}

	public BlockModelElementFaceResource getEastFace() {
		return east;
	}
	
}
