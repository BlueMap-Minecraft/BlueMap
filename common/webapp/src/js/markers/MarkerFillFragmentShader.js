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

export const MARKER_FILL_FRAGMENT_SHADER = `
${ShaderChunk.logdepthbuf_pars_fragment}

#define FLT_MAX 3.402823466e+38

varying vec3 vPosition;
//varying vec3 vWorldPosition;
//varying vec3 vNormal;
//varying vec2 vUv;
//varying vec3 vColor;
varying float vDistance;

uniform vec3 markerColor;
uniform float markerOpacity;

uniform float fadeDistanceMax;
uniform float fadeDistanceMin;

void main() {
	vec4 color = vec4(markerColor, markerOpacity);
	
	// distance fading
	float fdMax = FLT_MAX;
	if ( fadeDistanceMax > 0.0 ) fdMax = fadeDistanceMax;
	
	float minDelta = (vDistance - fadeDistanceMin) / fadeDistanceMin;
	float maxDelta = (vDistance - fadeDistanceMax) / (fadeDistanceMax * 0.5);
	float distanceOpacity = min(
		clamp(minDelta, 0.0, 1.0),
		1.0 - clamp(maxDelta + 1.0, 0.0, 1.0)
	);
	
	color.a *= distanceOpacity;
	
	// apply vertex-color
	//color.rgb *= vColor.rgb;
	
	gl_FragColor = color;
	
	${ShaderChunk.logdepthbuf_fragment}
}
`;
