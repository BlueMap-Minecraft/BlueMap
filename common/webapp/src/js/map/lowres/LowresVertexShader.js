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

export const LOWRES_VERTEX_SHADER = `
#include <common>
${ShaderChunk.logdepthbuf_pars_vertex}

uniform sampler2D textureImage;
uniform vec2 tileSize;
uniform vec2 textureSize;

varying vec3 vPosition;
varying vec3 vWorldPosition;
//varying float vDistance;

float metaToHeight(vec4 meta) {
	float heightUnsigned = meta.g * 65280.0 + meta.b * 255.0;
	if (heightUnsigned >= 32768.0) {
		return -(65535.0 - heightUnsigned);
	} else {
		return heightUnsigned;	
	}
}

vec2 posToMetaUV(vec2 pos) {
	return vec2(pos.x / textureSize.x, pos.y / textureSize.y + 0.5);
}

void main() {
	vPosition = position;
	
	vec4 meta = texture(textureImage, posToMetaUV(position.xz));
	vPosition.y = metaToHeight(meta) + 1.0 - position.x * 0.0001 - position.z * 0.0002; //including small offset-tilt to prevent z-fighting
	
	vec4 worldPos = modelMatrix * vec4(vPosition, 1);
	vec4 viewPos = viewMatrix * worldPos;
	
	vWorldPosition = worldPos.xyz;
	//vDistance = -viewPos.z;
	
	gl_Position = projectionMatrix * viewPos;
		
	${ShaderChunk.logdepthbuf_vertex}
}

`;
