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

import Element from "../ui/Element";

export default class Position extends Element {
	constructor(blueMap, axis) {
		super();
		this.blueMap = blueMap;
		this.axis = axis;

		$(document).on('bluemap-update-frame', this.update);
	}

	createElement(){
		let element = super.createElement();

		element.addClass("position");
		element.attr("data-axis", this.axis);
		let inputElement = $('<input type="number" value="0" />').appendTo(element);
		inputElement.on('input', this.onInput);
		inputElement.on('keydown', this.onKeyDown);

		return element;
	}

	onInput = event => {
		const value = Number(event.target.value);
		if (!isNaN(value)) {
			this.blueMap.controls.targetPosition[this.axis] = value;
			this.update();
		}
	};

	onKeyDown = event => {
		event.stopPropagation();
	};

	update = () => {
		const val = Math.floor(this.blueMap.controls.targetPosition[this.axis]);

		this.elements.forEach(element => {
			element.find("input").val(val);
		});
	};
}
