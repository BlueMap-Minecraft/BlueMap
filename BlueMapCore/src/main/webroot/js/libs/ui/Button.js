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

export default class Button extends Element {

	constructor(label, onClick, icon){
		super();

		this.label = label;
		this.onClickListener = onClick;
		this.icon = icon;
	}

	createElement() {
		let element = super.createElement();

		element.addClass("button");
		element.click(this.onClickEvent);

		if (this.label !== undefined) {
			$(`<div class="label">${this.label}</div>`).appendTo(element);
		}

		if (this.icon !== undefined){
			element.addClass("icon");
			$(`<img src="${this.icon}" />`).appendTo(element);
		}

		return element;
	}

	onClickEvent = () => {
		if (this.onClickListener !== undefined && this.onClickListener !== null) {
			this.onClickListener(this);
		}
	}

}