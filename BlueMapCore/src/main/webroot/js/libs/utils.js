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

import { Vector2, Vector3, FileLoader, BufferGeometry ,BufferAttribute } from 'three';

export const cachePreventionNr = () => {
	return Math.floor(Math.random() * 100000000);
};

export const stringToImage = string => {
	let image = document.createElementNS('http://www.w3.org/1999/xhtml', 'img');
	image.src = string;
	return image;
};

export const pathFromCoords = (x, z) => {
	let path = 'x';
	path += splitNumberToPath(x);

	path += 'z';
	path += splitNumberToPath(z);

	path = path.substring(0, path.length - 1);

	return path;
};

export const splitNumberToPath = num => {
	let path = '';

	if (num < 0) {
		num = -num;
		path += '-';
	}

	let s = parseInt(num).toString();

	for (let i = 0; i < s.length; i++) {
		path += s.charAt(i) + '/';
	}

	return path;
};

export const hashTile = (x, z) => `x${x}z${z}`;

/**
 * Adapted from https://www.w3schools.com/js/js_cookies.asp
 */
export const setCookie = (key, value, days = 360) => {
	value = JSON.stringify(value);

	let expireDate = new Date();
	expireDate.setTime(expireDate.getTime() + days * 24 * 60 * 60 * 1000);
	document.cookie = key + "=" + value + ";" + "expires=" + expireDate.toUTCString();
};

/**
 * Adapted from https://www.w3schools.com/js/js_cookies.asp
 */
export const getCookie = key => {
	let cookieString = decodeURIComponent(document.cookie);
	let cookies = cookieString.split(';');

	for(let i = 0; i < cookies.length; i++) {
		let cookie = cookies[i];

		while (cookie.charAt(0) === ' ') {
			cookie = cookie.substring(1);
		}

		if (cookie.indexOf(key + "=") === 0) {
			let value = cookie.substring(key.length + 1, cookie.length);

			try {
				value = JSON.parse(value);
			} catch (e) {}

			return value;
		}
	}

	return undefined;
};

const attributeTypes = ["position","normal","color","uv","ao","blocklight","sunlight"];

export const binaryGeometryLoader = (url, onLoad, onProgress, onError) => {
	const loader = new FileLoader();
	loader.setResponseType("arraybuffer");
	loader.load(url, function(data) {
		if (data instanceof ArrayBuffer == false) {
			return onError("not ArrayBuffer")
		}
        var dv = new DataView(data);
		var sign = "";
		for (var i = 0; i < 4; i ++) {
			sign += String.fromCharCode(dv.getUint8(i))
		}
		if (sign != "BLUE") {
			return onError("not BLUE")
		}
		var size = dv.getInt32(4);
		if (size != data.byteLength) {
			return onError("size!= " + size + ";"+data.byteLength);
		}
		const geometry = new BufferGeometry();
		var headersize = dv.getInt32(8) * 4;
		var gstart = dv.getInt32(12);
		var glength = dv.getInt32(16);
		for (var i = 20; i < headersize; i += 20) {
			var astart = dv.getInt32(i);
			var alength = dv.getInt32(i+4);
			var type = attributeTypes[dv.getInt32(i+8)];
			var normalized = dv.getInt32(i+12) != 0;
			var itemsize = dv.getInt32(i+16);
			var bufferAttribute = new BufferAttribute(new Float32Array(data, astart, alength/4), itemsize, normalized);
			geometry.attributes[type] = bufferAttribute;
		}
		for (var i = gstart; i < gstart + glength; i+=12) {
			var materialIndex = dv.getInt32(i);
			var start = dv.getInt32(i+4);
			var count = dv.getInt32(i+8);
			geometry.addGroup( start, count, materialIndex );
		}
		onLoad(geometry);
	}, onProgress, onError);
}

export const Vector2_ZERO = new Vector2(0, 0);
export const Vector3_ZERO = new Vector3(0, 0, 0);
