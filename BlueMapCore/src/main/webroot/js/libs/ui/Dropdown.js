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

export default class Dropdown extends Element {

	constructor(onChange, minWidth) {
		super();
		this.minWidth = minWidth;
		this.value = null;
		this.options = [];
		this.onChange = onChange;

		$(window).on('click', this.closeAll);
	}

	addOption(value, label, select) {
		this.options.push({
			value: value,
			label: label,
			select: select
		});

		if (this.value === null || select){
			this.select(value);
		}
	}

	createElement(){
		let element = super.createElement();

		element.addClass("dropdown");
		let headerElement = $('<div class="header"></div>').appendTo(element);
		let selectElement = $('<div class="select" style="display: none"></div>').appendTo(element);

		headerElement.click(this.toggleEvent(element));

		if (this.minWidth !== undefined){
			this.element.addClass("sized");
			this.element.css("min-width", this.minWidth);
		}

		this.options.forEach(option => {
			let optionElement = $(`<div class="ui-element option" data-value="${option.value}">${option.label}</div>`).appendTo(selectElement);
			optionElement.on('click', this.selectButtonEvent(option.value, optionElement));

			if (this.value === option.value){
				optionElement.addClass('selected');
				headerElement.html('');
				headerElement.append(optionElement.clone().off());
			}
		});

		return element;
	}

	toggleEvent = element => event => {
		let select = element.find(".select");
		let open = select.css("display") !== "none";

		this.closeAll();

		if (!open) {
			select.stop(true).slideDown(200);
			element.addClass("open");
			event.stopPropagation();
		}
	};

	closeAll = () => {
		this.elements.forEach(element => {
			element.removeClass("open");
			element.find(".select:not(:hidden)").stop(true).slideUp(200);
		});
	};

	select = value => {
		this.value = value;

		this.elements.forEach(element => {
			let selectElement = element.find(".select");
			selectElement.find('.selected').removeClass('selected');

			let option = selectElement.find(`.option[data-value='${value}']`);
			option.addClass('selected');

			let headerElement = element.find(".header");
			headerElement.html('');
			headerElement.append(option.clone().off());
		});
	};

	selectButtonEvent = (value, option) => event => {
		this.select(value);

		//close
		option.parents(".select").slideUp(200);

		if (event !== undefined) event.stopPropagation();

		if (this.onChange !== undefined && this.onChange !== null){
			this.onChange(this.value, this);
		}
	};

}