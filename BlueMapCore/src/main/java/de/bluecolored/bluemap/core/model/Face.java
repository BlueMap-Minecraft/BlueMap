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
package de.bluecolored.bluemap.core.model;

import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.matrix.Matrix3f;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3f;

@Deprecated
public class Face {

	private final VectorM3f p1, p2, p3; // points
	private final VectorM3f n1, n2, n3; // normals
	private Vector3f c1, c2, c3; // vertex-colors
	private Vector2f uv1, uv2, uv3; // texture UV
	private int materialIndex;
	private boolean normalizedNormals;

	public Face(Vector3f p1, Vector3f p2, Vector3f p3, Vector2f uv1, Vector2f uv2, Vector2f uv3, int materialIndex) {
		this.p1 = new VectorM3f(p1);
		this.p2 = new VectorM3f(p2);
		this.p3 = new VectorM3f(p3);

		this.uv1 = uv1;
		this.uv2 = uv2;
		this.uv3 = uv3;

		this.materialIndex = materialIndex;

		this.n1 = calculateSurfaceNormal(new VectorM3f(0, 0, 0));
		this.n2 = new VectorM3f(n1);
		this.n3 = new VectorM3f(n1);
		this.normalizedNormals = true;

		Vector3f color = Vector3f.ONE;
		this.c1 = color;
		this.c2 = color;
		this.c3 = color;
	}

	public void rotate(Quaternionf rotation) {
		p1.rotate(rotation);
		p2.rotate(rotation);
		p3.rotate(rotation);

		n1.rotate(rotation);
		n2.rotate(rotation);
		n3.rotate(rotation);
	}

	public void transform(Matrix3f transformation) {
		MatrixM3f mtransform = new MatrixM3f(transformation);

		p1.transform(mtransform);
		p2.transform(mtransform);
		p3.transform(mtransform);

		n1.transform(mtransform);
		n2.transform(mtransform);
		n3.transform(mtransform);

		normalizedNormals = false;
	}

	public void translate(Vector3f translation) {
		p1.add(translation);
		p2.add(translation);
		p3.add(translation);
	}

	public Vector3f getP1() {
		return p1.toVector3f();
	}

	public void setP1(Vector3f p1) {
		this.p1.set(p1);
	}

	public Vector3f getP2() {
		return p2.toVector3f();
	}

	public void setP2(Vector3f p2) {
		this.p2.set(p2);
	}

	public Vector3f getP3() {
		return p3.toVector3f();
	}

	public void setP3(Vector3f p3) {
		this.p3.set(p3);
	}

	public Vector3f getN1() {
		normalizeNormals();
		return n1.toVector3f();
	}

	public void setN1(Vector3f n1) {
		this.n1.set(n1);
		normalizedNormals = false;
	}

	public Vector3f getN2() {
		normalizeNormals();
		return n2.toVector3f();
	}

	public void setN2(Vector3f n2) {
		this.n2.set(n2);
		normalizedNormals = false;
	}

	public Vector3f getN3() {
		normalizeNormals();
		return n3.toVector3f();
	}

	public void setN3(Vector3f n3) {
		this.n3.set(n3);
		normalizedNormals = false;
	}

	public Vector3f getC1() {
		return c1;
	}

	public void setC1(Vector3f c1) {
		this.c1 = c1;
	}

	public Vector3f getC2() {
		return c2;
	}

	public void setC2(Vector3f c2) {
		this.c2 = c2;
	}

	public Vector3f getC3() {
		return c3;
	}

	public void setC3(Vector3f c3) {
		this.c3 = c3;
	}

	public Vector2f getUv1() {
		return uv1;
	}

	public void setUv1(Vector2f uv1) {
		this.uv1 = uv1;
	}

	public Vector2f getUv2() {
		return uv2;
	}

	public void setUv2(Vector2f uv2) {
		this.uv2 = uv2;
	}

	public Vector2f getUv3() {
		return uv3;
	}

	public void setUv3(Vector2f uv3) {
		this.uv3 = uv3;
	}

	public int getMaterialIndex() {
		return materialIndex;
	}

	public void setMaterialIndex(int materialIndex) {
		this.materialIndex = materialIndex;
	}

	private void normalizeNormals() {
		if (normalizedNormals) return;

		n1.normalize();
		n2.normalize();
		n3.normalize();

		normalizedNormals = true;
	}

	private VectorM3f calculateSurfaceNormal(
			VectorM3f target
	){
		double
				p1x = p1.x, p1y = p1.y, p1z = p1.z,
				p2x = p2.x, p2y = p2.y, p2z = p2.z,
				p3x = p3.x, p3y = p3.y, p3z = p3.z;

		p2x -= p1x; p2y -= p1y; p2z -= p1z;
		p3x -= p1x; p3y -= p1y; p3z -= p1z;

		p1x = p2y * p3z - p2z * p3y;
		p1y = p2z * p3x - p2x * p3z;
		p1z = p2x * p3y - p2y * p3x;

		double length = Math.sqrt(p1x * p1x + p1y * p1y + p1z * p1z);
		p1x /= length;
		p1y /= length;
		p1z /= length;

		target.x = (float) p1x;
		target.y = (float) p1y;
		target.z = (float) p1z;

		return target;
	}

}
