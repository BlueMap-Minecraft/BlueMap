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

import de.bluecolored.bluemap.core.util.MathUtils;

public class Face {

	private Vector3f p1, p2, p3; // points
	private Vector3f n1, n2, n3; // normals
	private Vector3f c1, c2, c3; // vertex-colors
	private Vector2f uv1, uv2, uv3; // texture UV
	private int materialIndex;
	private boolean normalizedNormals;

	public Face(Vector3f p1, Vector3f p2, Vector3f p3, Vector2f uv1, Vector2f uv2, Vector2f uv3, int materialIndex) {
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;

		this.uv1 = uv1;
		this.uv2 = uv2;
		this.uv3 = uv3;

		this.materialIndex = materialIndex;

		Vector3f faceNormal = getFaceNormal();
		this.n1 = faceNormal;
		this.n2 = faceNormal;
		this.n3 = faceNormal;
		this.normalizedNormals = true;

		Vector3f color = Vector3f.ONE;
		this.c1 = color;
		this.c2 = color;
		this.c3 = color;
	}

	public void rotate(Quaternionf rotation) {
		p1 = rotation.rotate(p1);
		p2 = rotation.rotate(p2);
		p3 = rotation.rotate(p3);

		n1 = rotation.rotate(n1);
		n2 = rotation.rotate(n2);
		n3 = rotation.rotate(n3);
	}

	public void transform(Matrix3f transformation) {
		p1 = transformation.transform(p1);
		p2 = transformation.transform(p2);
		p3 = transformation.transform(p3);

		n1 = transformation.transform(n1);
		n2 = transformation.transform(n2);
		n3 = transformation.transform(n3);

		normalizedNormals = false;
	}

	public void translate(Vector3f translation) {
		p1 = translation.add(p1);
		p2 = translation.add(p2);
		p3 = translation.add(p3);
	}

	public Vector3f getP1() {
		return p1;
	}

	public void setP1(Vector3f p1) {
		this.p1 = p1;
	}

	public Vector3f getP2() {
		return p2;
	}

	public void setP2(Vector3f p2) {
		this.p2 = p2;
	}

	public Vector3f getP3() {
		return p3;
	}

	public void setP3(Vector3f p3) {
		this.p3 = p3;
	}

	public Vector3f getN1() {
		normlizeNormals();
		return n1;
	}

	public void setN1(Vector3f n1) {
		this.n1 = n1;
		normalizedNormals = false;
	}

	public Vector3f getN2() {
		normlizeNormals();
		return n2;
	}

	public void setN2(Vector3f n2) {
		this.n2 = n2;
		normalizedNormals = false;
	}

	public Vector3f getN3() {
		normlizeNormals();
		return n3;
	}

	public void setN3(Vector3f n3) {
		this.n3 = n3;
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

	public Vector3f getFaceNormal() {
		return MathUtils.getSurfaceNormal(p1, p2, p3);
	}

	private void normlizeNormals() {
		if (normalizedNormals) return;

		n1 = n1.normalize();
		n2 = n2.normalize();
		n3 = n3.normalize();

		normalizedNormals = true;
	}

}
