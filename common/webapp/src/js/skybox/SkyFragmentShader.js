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
export const SKY_FRAGMENT_SHADER = `
uniform float sunlightStrength;
uniform float ambientLight;
uniform vec3 skyColor;
uniform vec3 voidColor;

varying vec3 vPosition;

vec3 adjustColor(vec3 color) {
	return vec3(color * max(sunlightStrength * sunlightStrength, ambientLight));
}

void main() {
	float horizonWidth = 0.005;
	float horizonHeight = 0.0;
	
	vec3 adjustedSkyColor = adjustColor(skyColor);
	vec3 adjustedVoidColor = adjustColor(voidColor);
	float voidMultiplier = (clamp(vPosition.y - horizonHeight, -horizonWidth, horizonWidth) + horizonWidth) / (horizonWidth * 2.0);
	vec3 color = mix(adjustedVoidColor, adjustedSkyColor, voidMultiplier);

	gl_FragColor = vec4(color, 1.0);
}
`;
