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

import Element from './Element.js';

export default class Slider extends Element {

	constructor(min = 0, max = 1, step = 0.01, value, onChange, minWidth){
		super();
		this.min = min;
		this.max = max;
		this.step = step;

		if (value === undefined) value = min;
		this.value = value;

		this.onChangeListener = onChange;
		this.minWidth = minWidth;
	}

	createElement() {
		let element = super.createElement();
		element.addClass("slider");

		if (this.minWidth !== undefined){
			element.addClass("sized");
			element.css("min-width", this.minWidth);
		}

		let slider = $(`<input type="range" min="${this.min}" max="${this.max}" step="${this.step}" value="${this.value}">`).appendTo(element);
		slider.on('input change', this.onChangeEvent(slider));
		$(`<div class="label">-</div>`).appendTo(element);

		this.update();

		return element;
	}

	getValue() {
		return this.value;
	}

	update(){
		this.elements.forEach(element => {
			let label = element.find(".label");
			let slider = element.find("input");

			slider.val(this.value);
			label.html(Math.round(this.value * 100) / 100);
		});
	}

	onChangeEvent = slider => () => {
		this.value = slider.val();

		this.update();

		if (this.onChangeListener !== undefined && this.onChangeListener !== null) {
			this.onChangeListener(this);
		}
	}

}