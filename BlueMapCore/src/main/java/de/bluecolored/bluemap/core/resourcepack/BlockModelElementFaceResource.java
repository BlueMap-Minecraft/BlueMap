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
import com.flowpowered.math.vector.Vector4f;

import de.bluecolored.bluemap.core.util.ConfigUtils;
import de.bluecolored.bluemap.core.util.Direction;
import ninja.leaping.configurate.ConfigurationNode;

public class BlockModelElementFaceResource {
	
	private BlockModelElementResource element;
	
	private Vector4f uv;
	private String texture;
	private String resolvedTexture;
	private Direction cullface;
	private int rotation;
	private int tintIndex;
	
	protected BlockModelElementFaceResource(BlockModelElementResource element, ConfigurationNode declaration) throws InvalidResourceDeclarationException {
		this.element = element;
		
		try {
			this.uv = getDefaultUV(declaration.getKey().toString(), element.getFrom(), element.getTo());
			
			ConfigurationNode uv = declaration.getNode("uv");
			if (!uv.isVirtual()) this.uv = ConfigUtils.readVector4f(declaration.getNode("uv"));
			
			this.texture = declaration.getNode("texture").getString();
			this.resolvedTexture = null;
			
			this.cullface = null;
			ConfigurationNode cf = declaration.getNode("cullface");
			if (!cf.isVirtual()) this.cullface = Direction.fromString(cf.getString());
			
			this.rotation = declaration.getNode("rotation").getInt(0);
			this.tintIndex = declaration.getNode("tintindex").getInt(-1);
		
		} catch (NullPointerException | IllegalArgumentException e){
			throw new InvalidResourceDeclarationException(e);
		}
	}
	
	public Vector4f getDefaultUV(String faceId, Vector3f from, Vector3f to){
		switch (faceId){
		
		case "down" :
		case "up" :
			return new Vector4f(
				from.getX(), from.getZ(),
				to.getX(),   to.getZ()
			);
			
		case "north" :
		case "south" :
			return new Vector4f(
				from.getX(), from.getY(),
				to.getX(),   to.getY()
			);

		case "west" :
		case "east" :
			return new Vector4f(
				from.getZ(), from.getY(),
				to.getZ(),   to.getY()
			);
			
		default :
			return new Vector4f(
				0, 0, 
				16, 16
			);
		
		}
	}
	
	public BlockModelElementResource getElement(){
		return element;
	}

	public Vector4f getUv() {
		return uv;
	}

	public String getTexture() {
		return texture;
	}
	
	public String getResolvedTexture() {
		if (resolvedTexture == null){
			resolvedTexture = getElement().getModel().resolveTexture(getTexture());
		}
		
		return resolvedTexture;
	}

	public boolean isCullface() {
		return cullface != null;
	}
	
	public Direction getCullface() {
		return cullface;
	}

	public int getRotation() {
		return rotation;
	}

	public boolean isTinted(){
		return tintIndex >= 0;
	}
	
	public int getTintIndex() {
		return tintIndex;
	}
	
}
