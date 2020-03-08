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

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3f;

public class ExtendedFace extends Face {

	private float ao1 = 1f, ao2 = 1f, ao3 = 1f; // ao
	private float bl1 = 15f, bl2 = 15f, bl3 = 15f; // block-light
	private float sl1 = 15f, sl2 = 15f, sl3 = 15f; // sun-light

	public ExtendedFace(
		Vector3f p1,
		Vector3f p2,
		Vector3f p3,
		Vector2f uv1,
		Vector2f uv2,
		Vector2f uv3,
		int materialIndex
	) {
		super(p1, p2, p3, uv1, uv2, uv3, materialIndex);
	}

	public float getAo1() {
		return ao1;
	}

	public void setAo1(float ao1) {
		this.ao1 = ao1;
	}

	public float getAo2() {
		return ao2;
	}

	public void setAo2(float ao2) {
		this.ao2 = ao2;
	}

	public float getAo3() {
		return ao3;
	}

	public void setAo3(float ao3) {
		this.ao3 = ao3;
	}

	public float getBl1() {
		return bl1;
	}

	public void setBl1(float bl1) {
		this.bl1 = bl1;
	}

	public float getBl2() {
		return bl2;
	}

	public void setBl2(float bl2) {
		this.bl2 = bl2;
	}

	public float getBl3() {
		return bl3;
	}

	public void setBl3(float bl3) {
		this.bl3 = bl3;
	}

	public float getSl1() {
		return sl1;
	}

	public void setSl1(float sl1) {
		this.sl1 = sl1;
	}

	public float getSl2() {
		return sl2;
	}

	public void setSl2(float sl2) {
		this.sl2 = sl2;
	}

	public float getSl3() {
		return sl3;
	}

	public void setSl3(float sl3) {
		this.sl3 = sl3;
	}

}
