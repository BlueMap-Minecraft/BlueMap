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

import com.flowpowered.math.vector.*;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;

public class ConfigUtils {

	private ConfigUtils(){}
	
	public static Vector2i readVector2i(ConfigurationNode vectorNode){
		if (vectorNode.isList()){
			List<? extends ConfigurationNode> list = vectorNode.childrenList();
			return new Vector2i(
					list.get(0).getInt(),
					list.get(1).getInt()
				);
		}
		
		return new Vector2i(
				vectorNode.node("x").getInt(),
				vectorNode.node("y").getInt()
			);
	}
	
	public static Vector3i readVector3i(ConfigurationNode vectorNode){
		if (vectorNode.isList()){
			List<? extends ConfigurationNode> list = vectorNode.childrenList();
			return new Vector3i(
					list.get(0).getInt(),
					list.get(1).getInt(),
					list.get(2).getInt()
				);
		}
		
		return new Vector3i(
				vectorNode.node("x").getInt(),
				vectorNode.node("y").getInt(),
				vectorNode.node("z").getInt()
			);
	}
	
	public static Vector3f readVector3f(ConfigurationNode vectorNode){
		if (vectorNode.isList()){
			List<? extends ConfigurationNode> list = vectorNode.childrenList();
			return new Vector3f(
					list.get(0).getFloat(),
					list.get(1).getFloat(),
					list.get(2).getFloat()
				);
		}
		
		return new Vector3f(
				vectorNode.node("x").getFloat(),
				vectorNode.node("y").getFloat(),
				vectorNode.node("z").getFloat()
			);
	}
	
	public static Vector4i readVector4i(ConfigurationNode vectorNode){
		if (vectorNode.isList()){
			List<? extends ConfigurationNode> list = vectorNode.childrenList();
			return new Vector4i(
					list.get(0).getInt(),
					list.get(1).getInt(),
					list.get(2).getInt(),
					list.get(3).getInt()
				);
		}
		
		return new Vector4i(
				vectorNode.node("x").getInt(),
				vectorNode.node("y").getInt(),
				vectorNode.node("z").getInt(),
				vectorNode.node("w").getInt()
			);
	}
	
	public static Vector4f readVector4f(ConfigurationNode vectorNode){
		if (vectorNode.isList()){
			List<? extends ConfigurationNode> list = vectorNode.childrenList();
			return new Vector4f(
					list.get(0).getFloat(),
					list.get(1).getFloat(),
					list.get(2).getFloat(),
					list.get(3).getFloat()
				);
		}
		
		return new Vector4f(
				vectorNode.node("x").getFloat(),
				vectorNode.node("y").getFloat(),
				vectorNode.node("z").getFloat(),
				vectorNode.node("w").getFloat()
			);
	}
	
	public static void writeVector4f(ConfigurationNode vectorNode, Vector4f v) throws SerializationException {
		vectorNode.appendListNode().set(v.getX());
		vectorNode.appendListNode().set(v.getY());
		vectorNode.appendListNode().set(v.getZ());
		vectorNode.appendListNode().set(v.getW());
	}
	
	/**
	 * Returns an color-integer. The value can be a normal integer, an integer in String-Format, or a string in hexadecimal format prefixed with # (css-style: e.g. #f16 becomes #ff1166). 
	 * @param node The Configuration Node with the value
	 * @return The parsed Integer
	 * @throws NumberFormatException If the value is not formatted correctly or if there is no value present.
	 */
	public static int readColorInt(ConfigurationNode node) throws NumberFormatException {
		Object value = node.raw();

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
		NodePath keys = node.path();
		String[] stringKeys = new String[keys.size()];
		for (int i = 0; i < keys.size(); i++) {
			stringKeys[i] = keys.get(i).toString();
		}
		return String.join(".", stringKeys);
	}
	
}
