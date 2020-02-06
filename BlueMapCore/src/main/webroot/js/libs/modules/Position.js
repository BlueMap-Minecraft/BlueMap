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

		$('.bluemap-position').remove();
		this.elements = [
			this.createPositionElement('x'),
			null,//this.elementY = this.createPositionElement('y');
			this.createPositionElement('z'),
		];

		$(document).on('bluemap-update-frame', this.onBlueMapUpdateFrame);
	}

	/** Creates the position display */
	createPositionElement(type) {
		const parent = getTopLeftElement(this.blueMap);
		const element = $(`<div class="bluemap-position" data-pos="${type}"><input type="number" value="0" /></div>`)
			.appendTo(parent)
			.children()
			.first();
		element.on('input', this.onInput(type));
		element.on('keydown', this.onKeyDown);
		return element;
	}

	onInput = type => event => {
		const value = Number(event.target.value);
		if (!isNaN(value)) {
			this.blueMap.controls.targetPosition[type] = value;
		}
	};

	onKeyDown = event => {
		event.stopPropagation();
	};

	onBlueMapUpdateFrame = () => {
		const { x, y, z } = this.blueMap.controls.targetPosition;
		const values = [ z, y, x ];
		for (let element of this.elements) {
			const value = Math.floor(values.pop());
			if (element) {
				element.val(value);
			}
		}
	};
}
