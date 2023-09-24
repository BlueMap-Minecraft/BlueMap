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

export const LOWRES_FRAGMENT_SHADER = `
${ShaderChunk.logdepthbuf_pars_fragment}

#define PI 3.1415926535897932

#ifndef texture
	#define texture texture2D
#endif

struct TileMap {
	sampler2D map;
	float size;
	vec2 scale;
	vec2 translate;
	vec2 pos; 
};

uniform float distance;
uniform float sunlightStrength;
uniform float ambientLight;
uniform TileMap hiresTileMap;
uniform sampler2D textureImage;
uniform vec2 tileSize;
uniform vec2 textureSize;
uniform float lod;
uniform float lodScale;
uniform bool hasVoid;
uniform vec3 skyColor;

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

float metaToLight(vec4 meta) {
	return meta.r * 255.0;
}

vec2 posToColorUV(vec2 pos) {
	return vec2(pos.x / textureSize.x, min(pos.y, tileSize.y) / textureSize.y);
}

vec2 posToMetaUV(vec2 pos) {
	return vec2(pos.x / textureSize.x, pos.y / textureSize.y + 0.5);
}


void main() {
	//discard if hires tile is loaded at that position
	if (distance < 1000.0 && texture(hiresTileMap.map, ((vWorldPosition.xz - hiresTileMap.translate) / hiresTileMap.scale - hiresTileMap.pos) / hiresTileMap.size + 0.5).r > 0.75) discard;
	
	vec4 color = texture(textureImage, posToColorUV(vPosition.xz));

	vec4 meta = texture(textureImage, posToMetaUV(vPosition.xz));
	
	float height = metaToHeight(meta);
	
	float heightX = metaToHeight(texture(textureImage, posToMetaUV(vPosition.xz + vec2(1.0, 0.0))));
	float heightZ = metaToHeight(texture(textureImage, posToMetaUV(vPosition.xz + vec2(0.0, 1.0))));
	float heightDiff = ((height - heightX) + (height - heightZ)) / lodScale;
	float shade = clamp(heightDiff * 0.06, -0.2, 0.04);
	
	float ao = 0.0;
	float aoStrength = 0.0;
	if(lod == 1.0) {
		aoStrength = smoothstep(PI - 0.8, PI - 0.2, acos(-clamp(viewMatrix[1][2], 0.0, 1.0)));
		aoStrength *= 1.0 - smoothstep(200.0, 600.0, distance);
		
		if (aoStrength > 0.0) { 
			const float r = 3.0;
			const float step = 0.2;
			const float o = step / r * 0.1;
			for (float vx = -r; vx <= r; vx++) {
				for (float vz = -r; vz <= r; vz++) {
					heightDiff = height - metaToHeight(texture(textureImage, posToMetaUV(vPosition.xz + vec2(vx * step, vz * step))));
					if (heightDiff < 0.0) {
						ao -= o;            
					}
				}
			}
		}
	}
	
	color.rgb += mix(shade, shade * 0.3 + ao, aoStrength);
	
	float blockLight = metaToLight(meta);
	float light = mix(blockLight, 15.0, sunlightStrength);
	color.rgb *= mix(ambientLight, 1.0, light / 15.0);

	if (!hasVoid) {
		vec3 adjustedSkyColor = vec3(skyColor * max(sunlightStrength * sunlightStrength, ambientLight)); //calculation from SkyFragmentShader.js
		//where there's transparency, there is void that needs to be coloured
		color.rgb = mix(adjustedSkyColor, color.rgb, color.a);
	}
	color.a = 1.0; // don't display transparency

	gl_FragColor = color;
	
	${ShaderChunk.logdepthbuf_fragment}
}

`;
