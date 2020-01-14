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
import { Math as Math3 } from 'three';

import { getTopRightElement } from './Module.js';

import GEAR from '../../../assets/gear.svg';

export default class Settings {
	constructor(blueMap) {
		this.blueMap = blueMap;
		const parent = getTopRightElement(blueMap);

		$('#bluemap-settings').remove();
		this.elementMenu = $('<div id="bluemap-settings-container" style="display: none"></div>').appendTo(parent);
		this.elementSettings = $(`<div id="bluemap-settings" class="button"><img src="${GEAR}" /></div>`).appendTo(parent);
		this.elementSettings.click(this.onSettingsClick);

		/* Quality */

		this.elementQuality = $(
			'<div id="bluemap-settings-quality" class="dropdown-container"><span class="selection">Quality: <span>Normal</span></span><div class="dropdown"><ul>' +
			'<li quality="2">High</li>' +
			'<li quality="1" style="display: none">Normal</li>' +
			'<li quality="0.75">Fast</li>' +
			'</ul></div></div>'
		).prependTo(this.elementMenu);

		this.elementQuality.find('li[quality]').click(this.onQualityClick);
		this.elementRenderDistance = $('<div id="bluemap-settings-render-distance" class="dropdown-container"></div>').prependTo(this.elementMenu);

		this.init();

		$(document).on('bluemap-map-change', this.init);
	}

	init = () => {
		this.defaultHighRes = this.blueMap.hiresTileManager.viewDistance;
		this.defaultLowRes = this.blueMap.lowresTileManager.viewDistance;

		this.elementRenderDistance.html(
			'<span class="selection">View Distance: <span>' + this.blueMap.hiresTileManager.viewDistance + '</span></span>' +
			'<div class="dropdown">' +
			'<input type="range" min="0" max="100" step="1" value="' + this.renderDistanceToPct(this.blueMap.hiresTileManager.viewDistance, this.defaultHighRes) + '" />' +
			'</div>'
		);

		this.slider = this.elementRenderDistance.find('input');
		this.slider.on('change input', this.onViewDistanceSlider);
	};

	onViewDistanceSlider = () => {
		this.blueMap.hiresTileManager.viewDistance = this.pctToRenderDistance(parseFloat(this.slider.val()), this.defaultHighRes);
		this.blueMap.lowresTileManager.viewDistance = this.pctToRenderDistance(parseFloat(this.slider.val()), this.defaultLowRes);
		this.elementRenderDistance.find('.selection > span').html(Math.round(this.blueMap.hiresTileManager.viewDistance * 10) / 10);

		this.blueMap.lowresTileManager.update();
		this.blueMap.hiresTileManager.update();
	};

	onQualityClick = (event) => {
		const target = event.target
		const desc = $(target).html();
		this.blueMap.quality = parseFloat($(target).attr("quality"));

		this.elementQuality.find('li').show();
		this.elementQuality.find(`li[quality="${this.blueMap.quality}"]`).hide();

		this.elementQuality.find('.selection > span').html(desc);

		this.blueMap.handleContainerResize();
	};

	onSettingsClick = () => {
		if (this.elementMenu.css('display') === 'none'){
			this.elementSettings.addClass('active');
		} else {
			this.elementSettings.removeClass('active');
		}

		this.elementMenu.animate({
			width: 'toggle'
		}, 200);
	}

	pctToRenderDistance(value, defaultValue) {
		let max = defaultValue * 5;
		if (max > 20) max = 20;

		return Math3.mapLinear(value, 0, 100, 1, max);
	}

	renderDistanceToPct(value, defaultValue) {
		let max = defaultValue * 5;
		if (max > 20) max = 20;

		return Math3.mapLinear(value, 1, max, 0, 100);
	}
}
