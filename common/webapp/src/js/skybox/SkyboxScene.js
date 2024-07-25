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
import {BackSide, Mesh, Scene, ShaderMaterial, SphereGeometry} from 'three';

import {SKY_FRAGMENT_SHADER} from './SkyFragmentShader';
import {SKY_VERTEX_SHADER} from './SkyVertexShader';

export class SkyboxScene extends Scene {

	constructor(uniforms) {
		super();

		this.matrixWorldAutoUpdate  = false;

		Object.defineProperty(this, 'isSkyboxScene', {value: true});

		let geometry = new SphereGeometry(1, 40, 5);
		let material = new ShaderMaterial({
			uniforms: uniforms,
			vertexShader: SKY_VERTEX_SHADER,
			fragmentShader: SKY_FRAGMENT_SHADER,
			side: BackSide
		});
		let skybox = new Mesh(geometry, material);

		this.add(skybox);
	}

}