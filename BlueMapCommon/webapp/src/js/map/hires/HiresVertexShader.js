/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the 'Software'), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
import { ShaderChunk } from 'three';

export const HIRES_VERTEX_SHADER = `
#include <common>
${ShaderChunk.logdepthbuf_pars_vertex}

attribute float ao;
attribute float sunlight;
attribute float blocklight;

varying vec3 vPosition;
varying vec3 vNormal;
varying vec2 vUv;
varying vec3 vColor;
varying float vAo;
varying float vSunlight;
varying float vBlocklight;

void main() {
	vPosition = position;
	vNormal = normal;
	vUv = uv;
	vColor = color;
	vAo = ao;
	vSunlight = sunlight;
	vBlocklight = blocklight;
	
	gl_Position = projectionMatrix * (viewMatrix * modelMatrix * vec4(position, 1));
	
	${ShaderChunk.logdepthbuf_vertex} 
}
`;
