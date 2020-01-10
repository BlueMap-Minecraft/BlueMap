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
	
	/**
	 * Returns an color-integer. The value can be a normal integer, an integer in String-Format, or a string in hexadecimal format prefixed with # (css-style: e.g. #f16 becomes #ff1166). 
	 * @param node The Configuration Node with the value
	 * @return The parsed Integer
	 * @throws NumberFormatException If the value is not formatted correctly or if there is no value present.
	 */
	public static int readColorInt(ConfigurationNode node) throws NumberFormatException {
		Object value = node.getValue();

		if (value == null) throw new NumberFormatException("No value!");
		
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}

		String val = value.toString();

		if (val.charAt(0) == '#') {
			val = val.substring(1);
			if (val.length() == 3) val = "f" + val;
			if (val.length() == 4) val = "" + val.charAt(0) + val.charAt(0) + val.charAt(1) + val.charAt(1) + val.charAt(2) + val.charAt(2) + val.charAt(3) + val.charAt(3);
			if (val.length() == 6) val = "ff" + val;
			return Integer.parseUnsignedInt(val, 16);
		}
		
		return Integer.parseInt(val);
	}
	
	public static String nodePathToString(ConfigurationNode node) {
		Object[] keys = node.getPath();
		String[] stringKeys = new String[keys.length];
		for (int i = 0; i < keys.length; i++) {
			stringKeys[i] = keys[i].toString();
		}
		return String.join(".", stringKeys);
	}
	
}
