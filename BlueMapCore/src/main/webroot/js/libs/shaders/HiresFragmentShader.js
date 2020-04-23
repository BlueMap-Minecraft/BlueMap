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

const HIRES_FRAGMENT_SHADER = `
${ShaderChunk.logdepthbuf_pars_fragment}

uniform sampler2D texture;
uniform float sunlightStrength;
uniform float ambientLight;
uniform bool mobSpawnOverlay;

varying vec3 vPosition;
varying vec3 vWorldPosition;
varying vec3 vNormal;
varying vec2 vUv;
varying vec3 vColor;
varying float vAo;
varying float vSunlight;
varying float vBlocklight;

vec4 lerp(vec4 v1, vec4 v2, float amount){
	return v1 * (1.0 - amount) + v2 * amount; 
}
vec3 lerp(vec3 v1, vec3 v2, float amount){
	return v1 * (1.0 - amount) + v2 * amount; 
}

bool mobSpawnColor() {
	if (vBlocklight < 7.1 && vNormal.y > 0.8){
		float cross1 = vUv.x - vUv.y;
		float cross2 = vUv.x - (1.0 - vUv.y);
		return cross1 < 0.05 && cross1 > -0.05 || cross2 < 0.05 && cross2 > -0.05;
	}
	
	return false;
}

void main() {
	vec4 color = texture2D(texture, vUv);
	if (color.a == 0.0) discard;
	
	//apply vertex-color
	color.rgb *= vColor;
	
	//mob spawn overlay
	if (mobSpawnOverlay && mobSpawnColor()){
		color.rgb = lerp(vec3(1.0, 0.0, 0.0), color.rgb, 0.25);
	}

	//apply ao
	color.rgb *= vAo;
	
	//apply light
	float light = max(vSunlight * sunlightStrength, vBlocklight);
	color.rgb *= max(light / 15.0, ambientLight);
	
	gl_FragColor = color;
	
	${ShaderChunk.logdepthbuf_fragment}
}
`;

export default HIRES_FRAGMENT_SHADER;