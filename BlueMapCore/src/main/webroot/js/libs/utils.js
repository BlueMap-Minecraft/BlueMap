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

import { Vector2, Vector3 } from 'three';

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

export const Vector2_ZERO = new Vector2(0, 0);
export const Vector3_ZERO = new Vector3(0, 0, 0);
