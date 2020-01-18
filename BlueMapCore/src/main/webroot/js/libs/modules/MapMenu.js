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

export default class MapMenu {
	constructor(blueMap) {
		this.bluemap = blueMap;
		const maps = this.bluemap.settings;

		$('#bluemap-mapmenu').remove();
		this.element = $(`<div id="bluemap-mapmenu" class="dropdown-container"><span class="selection">${maps[this.bluemap.map].name}</span></div>`).appendTo(getTopLeftElement(blueMap));

		const dropdown = $('<div class="dropdown"></div>').appendTo(this.element);
		this.maplist = $('<ul></ul>').appendTo(dropdown);

		for (let mapId in maps) {
			if (!maps.hasOwnProperty(mapId)) continue;
			const map = maps[mapId];
			if (!map.enabled) continue;

			$(`<li map="${mapId}">${map.name}</li>`).appendTo(this.maplist);
		}

		this.maplist.find('li[map=' + this.bluemap.map + ']').hide();
		this.maplist.find('li[map]').click(this.onMapClick);
		$(document).on('bluemap-map-change', this.onBlueMapMapChange);
	}

	onMapClick = event => {
		const map = $(event.target).attr('map');
		this.bluemap.changeMap(map);
	};

	onBlueMapMapChange = () => {
		this.maplist.find('li').show();
		this.maplist.find('li[map=' + this.bluemap.map + ']').hide();
		this.element.find('.selection').html(this.bluemap.settings[this.bluemap.map].name);
	};
}
