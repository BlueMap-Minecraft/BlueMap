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

import Toolbar from './Toolbar.js';
import Menu from './Menu.js';
import Dropdown from "./Dropdown";
import Separator from "./Separator";
import Label from "./Label";
import MenuButton from './MenuButton.js';
import Compass from "./Compass";
import Position from "./Position";
import Button from "./Button";
import Slider from "./Slider";
import ToggleButton from "./ToggleButton";
import MapSelection from "./MapSeletion";

import NIGHT from '../../../assets/night.svg';
import HudInfo from "../hud/HudInfo";
import MarkerManager from "../hud/MarkerManager";

import {cachePreventionNr} from "../utils";

export default class UI {

	constructor(blueMap) {
		this.blueMap = blueMap;
		this.element = $('<div class="ui"></div>').appendTo(this.blueMap.element);

		this.menu = new Menu();
		this.menu.element.appendTo(this.element);

		this.hud = $('<div class="hud"></div>').appendTo(this.element);

		this.toolbar = new Toolbar();
		this.toolbar.element.appendTo(this.hud);

		//modules
		this.hudInfo = new HudInfo(this.blueMap);
		this.markers = new MarkerManager(this.blueMap, this);
	}

	async load() {
		//elements
		let menuButton = new MenuButton(this.menu);
		let mapSelect = new MapSelection(this.blueMap);
		let nightButton = new ToggleButton("night", blueMap.targetSunLightStrength < 1, button => {
			this.blueMap.targetSunLightStrength = button.isSelected() ? 0.1 : 1;
		}, NIGHT);
		let posX = new Position(this.blueMap, 'x');
		let posZ = new Position(this.blueMap, 'z');
		let compass = new Compass(this.blueMap);

		let mobSpawnOverlay = new ToggleButton("mob-spawn (experimental)", blueMap.mobSpawnOverlay.value, button => {
			this.blueMap.mobSpawnOverlay.value = button.isSelected();
			this.blueMap.updateFrame = true;
		});

		let quality = new Dropdown(value => {
			this.blueMap.quality = parseFloat(value);
			this.blueMap.handleContainerResize();
		});
		quality.addOption("2", "high", this.blueMap.quality === 2);
		quality.addOption("1", "normal", this.blueMap.quality === 1);
		quality.addOption("0.5", "low", this.blueMap.quality === 0.5);
		let hiresSlider = new Slider(32, 480, 1, this.blueMap.hiresViewDistance, v => {
			this.blueMap.hiresViewDistance = v.getValue();
			this.blueMap.hiresTileManager.setViewDistance(this.blueMap.hiresViewDistance);
			this.blueMap.hiresTileManager.update();
		});
		let lowresSlider = new Slider(480, 6400, 1, this.blueMap.lowresViewDistance, v => {
			this.blueMap.lowresViewDistance = v.getValue();
			this.blueMap.lowresTileManager.setViewDistance(this.blueMap.lowresViewDistance);
			this.blueMap.lowresTileManager.update();
		});
		let extendedZoom = new ToggleButton("extended zoom", this.blueMap.controls.settings.zoom.max > 2000, button => {
			this.blueMap.controls.settings.zoom.max = button.isSelected() ? 8000 : 2000;
			this.blueMap.controls.targetDistance = Math.min(this.blueMap.controls.targetDistance, this.blueMap.controls.settings.zoom.max);
		});
		let debugInfo = new ToggleButton("debug-info", this.blueMap.debugInfo, button => {
			this.blueMap.debugInfo = button.isSelected();
		});

		let clearCache = new Button("clear tile cache", button => {
			this.blueMap.cacheSuffix = cachePreventionNr();
			this.blueMap.reloadMap();
		});

		//toolbar
		this.toolbar.addElement(menuButton);
		this.toolbar.addElement(mapSelect);
		this.toolbar.addElement(new Separator(), true);
		this.toolbar.addElement(nightButton, true);
		this.toolbar.addElement(new Separator(true));
		this.toolbar.addElement(posX);
		this.toolbar.addElement(posZ);
		this.toolbar.addElement(compass);
		this.toolbar.update();

		//menu
		this.menu.addElement(nightButton);
		//this.menu.addElement(mobSpawnOverlay);

		await this.markers.readyPromise;
		this.markers.addMenuElements(this.menu);

		this.menu.addElement(new Separator());
		this.menu.addElement(new Label('render quality:'));
		this.menu.addElement(quality);
		this.menu.addElement(new Label('hires render-distance (blocks):'));
		this.menu.addElement(hiresSlider);
		this.menu.addElement(new Label('lowres render-distance (blocks):'));
		this.menu.addElement(lowresSlider);
		this.menu.addElement(extendedZoom);
		this.menu.addElement(new Separator());
		this.menu.addElement(clearCache);
		this.menu.addElement(debugInfo);
		this.menu.update();
	}

}