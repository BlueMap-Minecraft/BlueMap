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
package de.bluecolored.bluemap.core.util;

import java.util.List;

import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.flowpowered.math.vector.Vector4f;
import com.flowpowered.math.vector.Vector4i;

import ninja.leaping.configurate.ConfigurationNode;

public class ConfigUtils {

	private ConfigUtils(){}
	
	public static Vector2i readVector2i(ConfigurationNode vectorNode){
		if (vectorNode.hasListChildren()){
			List<? extends ConfigurationNode> list = vectorNode.getChildrenList();
			return new Vector2i(
					list.get(0).getInt(),
					list.get(1).getInt()
				);
		}
		
		return new Vector2i(
				vectorNode.getNode("x").getInt(),
				vectorNode.getNode("y").getInt()
			);
	}
	
	public static Vector3i readVector3i(ConfigurationNode vectorNode){
		if (vectorNode.hasListChildren()){
			List<? extends ConfigurationNode> list = vectorNode.getChildrenList();
			return new Vector3i(
					list.get(0).getInt(),
					list.get(1).getInt(),
					list.get(2).getInt()
				);
		}
		
		return new Vector3i(
				vectorNode.getNode("x").getInt(),
				vectorNode.getNode("y").getInt(),
				vectorNode.getNode("z").getInt()
			);
	}
	
	public static Vector3f readVector3f(ConfigurationNode vectorNode){
		if (vectorNode.hasListChildren()){
			List<? extends ConfigurationNode> list = vectorNode.getChildrenList();
			return new Vector3f(
					list.get(0).getFloat(),
					list.get(1).getFloat(),
					list.get(2).getFloat()
				);
		}
		
		return new Vector3f(
				vectorNode.getNode("x").getFloat(),
				vectorNode.getNode("y").getFloat(),
				vectorNode.getNode("z").getFloat()
			);
	}
	
	public static Vector4i readVector4i(ConfigurationNode vectorNode){
		if (vectorNode.hasListChildren()){
			List<? extends ConfigurationNode> list = vectorNode.getChildrenList();
			return new Vector4i(
					list.get(0).getInt(),
					list.get(1).getInt(),
					list.get(2).getInt(),
					list.get(3).getInt()
				);
		}
		
		return new Vector4i(
				vectorNode.getNode("x").getInt(),
				vectorNode.getNode("y").getInt(),
				vectorNode.getNode("z").getInt(),
				vectorNode.getNode("w").getInt()
			);
	}
	
	public static Vector4f readVector4f(ConfigurationNode vectorNode){
		if (vectorNode.hasListChildren()){
			List<? extends ConfigurationNode> list = vectorNode.getChildrenList();
			return new Vector4f(
					list.get(0).getFloat(),
					list.get(1).getFloat(),
					list.get(2).getFloat(),
					list.get(3).getFloat()
				);
		}
		
		return new Vector4f(
				vectorNode.getNode("x").getFloat(),
				vectorNode.getNode("y").getFloat(),
				vectorNode.getNode("z").getFloat(),
				vectorNode.getNode("w").getFloat()
			);
	}
	
	public static void writeVector4f(ConfigurationNode vectorNode, Vector4f v){
		vectorNode.getAppendedNode().setValue(v.getX());
		vectorNode.getAppendedNode().setValue(v.getY());
		vectorNode.getAppendedNode().setValue(v.getZ());
		vectorNode.getAppendedNode().setValue(v.getW());
	}
	
}
