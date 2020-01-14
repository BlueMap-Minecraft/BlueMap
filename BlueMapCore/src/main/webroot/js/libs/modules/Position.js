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
import $ from 'jquery';

import { getTopLeftElement } from './Module.js';

export default class Position {
	constructor(blueMap) {
		this.blueMap = blueMap;
		const parent = getTopLeftElement(blueMap);

		$('.bluemap-position').remove();
		this.elementX = $('<div class="bluemap-position pos-x">0</div>').appendTo(parent);
		//this.elementY = $('<div class="bluemap-position pos-y">0</div>').appendTo(parent);
		this.elementZ = $('<div class="bluemap-position pos-z">0</div>').appendTo(parent);

		$(document).on('bluemap-update-frame', this.onBlueMapUpdateFrame);
	}

	onBlueMapUpdateFrame = () => {
		this.elementX.html(Math.floor(this.blueMap.controls.targetPosition.x));
		//this.elementY.html(this.blueMap.controls.targetPosition.y === 0 ? '-' : Math.floor(this.blueMap.controls.targetPosition.y));
		this.elementZ.html(Math.floor(this.blueMap.controls.targetPosition.z));
	}
}
