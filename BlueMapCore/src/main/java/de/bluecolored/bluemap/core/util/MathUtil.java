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

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.flowpowered.math.vector.Vector4f;

public class MathUtil {

	private MathUtil() {}
	
	public static Vector3d getSurfaceNormal(Vector3d p1, Vector3d p2, Vector3d p3){
		Vector3d u = p2.sub(p1);
		Vector3d v = p3.sub(p1);
		
		double nX = u.getY() * v.getZ() - u.getZ() * v.getY();
		double nY = u.getZ() * v.getX() - u.getX() * v.getZ();
		double nZ = u.getX() * v.getY() - u.getY() * v.getX();
		
		return new Vector3d(nX, nY, nZ);
	}

	public static Vector3f getSurfaceNormal(Vector3f p1, Vector3f p2, Vector3f p3) {
		Vector3f u = p2.sub(p1);
		Vector3f v = p3.sub(p1);
		
		float nX = u.getY() * v.getZ() - u.getZ() * v.getY();
		float nY = u.getZ() * v.getX() - u.getX() * v.getZ();
		float nZ = u.getX() * v.getY() - u.getY() * v.getX();
		
		Vector3f n = new Vector3f(nX, nY, nZ);
		n = n.normalize();
		
		return n;
	}

	public static float hashToFloat(Vector3i pos, long seed) {
		return hashToFloat(pos.getX(), pos.getY(), pos.getZ(), seed);
	}
	
	/**
	 * Adapted from https://github.com/SpongePowered/SpongeAPI/blob/ecd761a70219e467dea47a09fc310e8238e9911f/src/main/java/org/spongepowered/api/extra/skylands/SkylandsUtil.java
	 */
	public static float hashToFloat(int x, int y, int z, long seed) {
        final long hash = x * 73428767 ^ y * 9122569 ^ z * 4382893 ^ seed * 457;
        return (hash * (hash + 456149) & 0x00ffffff) / (float) 0x01000000;
    }

	public static Vector4f blendColors(Vector4f top, Vector4f bottom){
		if (top.getW() > 0 && bottom.getW() > 0){
			float a = 1 - (1 - top.getW()) * (1 - bottom.getW());
			float r = (top.getX() * top.getW() / a) + (bottom.getX() * bottom.getW() * (1 - top.getW()) / a);
			float g = (top.getY() * top.getW() / a) + (bottom.getY() * bottom.getW() * (1 - top.getW()) / a);
			float b = (top.getZ() * top.getW() / a) + (bottom.getZ() * bottom.getW() * (1 - top.getW()) / a);
			return new Vector4f(r, g, b, a);
		} else if (bottom.getW() > 0) {
			return bottom;
		} else {
			return top;
		}
	}
	
	public static Vector4f overlayColors(Vector4f top, Vector4f bottom){
		if (top.getW() > 0 && bottom.getW() > 0){
			float p = (1 - top.getW()) * bottom.getW();
			float a = p + top.getW();
			float r = (p * bottom.getX() + top.getW() * top.getX()) / a;
			float g = (p * bottom.getY() + top.getW() * top.getY()) / a;
			float b = (p * bottom.getZ() + top.getW() * top.getZ()) / a;
			return new Vector4f(r, g, b, a);
		} else if (bottom.getW() > 0) {
			return bottom;
		} else {
			return top;
		}
	}
	
}
