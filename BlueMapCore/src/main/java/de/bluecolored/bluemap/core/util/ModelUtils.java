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

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;

import de.bluecolored.bluemap.core.model.Face;
import de.bluecolored.bluemap.core.model.Model;

@Deprecated //TODO
public class ModelUtils {

	private ModelUtils() {}
	
	/**
	 * Creates a plane-grid with alternating face-rotations.
	 */
	public static Model<Face> makeGrid(Vector2i gridSize){
		Model<Face> m = new Model<Face>();
		
		float y = 0;
		
		for (int x = 0; x < gridSize.getX(); x++){
			for (int z = 0; z < gridSize.getY(); z++){
				
				Vector3f[] p = new Vector3f[]{
					new Vector3f(x    , y, z + 1),
					new Vector3f(x + 1, y, z + 1),
					new Vector3f(x + 1, y, z    ),
					new Vector3f(x    , y, z    ),
				};
				
				Vector2f[] uv = new Vector2f[]{
						new Vector2f(0, 1),
						new Vector2f(1, 1),
						new Vector2f(1, 0),
						new Vector2f(0, 0),
					};
				
				Face f1, f2;
				if (x % 2 == z % 2){
					f1 = new Face(p[0], p[1], p[2], uv[0], uv[1], uv[2], -1);
					f2 = new Face(p[0], p[2], p[3], uv[0], uv[2], uv[3], -1);
				} else {
					f1 = new Face(p[0], p[1], p[3], uv[0], uv[1], uv[3], -1);
					f2 = new Face(p[1], p[2], p[3], uv[1], uv[2], uv[3], -1);
				}
				
				Vector3f color = Vector3f.ZERO;
				
				f1.setC1(color);
				f1.setC2(color);
				f1.setC3(color);

				f2.setC1(color);
				f2.setC2(color);
				f2.setC3(color);
				
				m.addFace(f1);
				m.addFace(f2);
			}
		}
		
		return m;
	}
	
}
