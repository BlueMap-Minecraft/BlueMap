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
import {
	AmbientLight,
  BackSide,
  BufferGeometryLoader,
  ClampToEdgeWrapping,
  CubeGeometry,
  DirectionalLight,
	FileLoader,
	FrontSide,
	Mesh,
	MeshBasicMaterial,
	MeshLambertMaterial,
	NormalBlending,
  NearestFilter,
  PerspectiveCamera,
	Scene,
	Texture,
  TextureLoader,
	VertexColors,
	WebGLRenderer,
} from 'three';

import Compass from './modules/Compass.js';
import Info from './modules/Info.js';
import MapMenu from './modules/MapMenu.js';
import Position from './modules/Position.js';
import Settings from './modules/Settings.js';

import Controls from './Controls.js';
import TileManager from './TileManager.js';

import { stringToImage, pathFromCoords } from './utils.js';

import SKYBOX_NORTH from '../../assets/skybox/north.png';
import SKYBOX_SOUTH from '../../assets/skybox/south.png';
import SKYBOX_EAST from '../../assets/skybox/east.png';
import SKYBOX_WEST from '../../assets/skybox/west.png';
import SKYBOX_UP from '../../assets/skybox/up.png';
import SKYBOX_DOWN from '../../assets/skybox/down.png';

export default class BlueMap {
	constructor(element, dataRoot) {
		this.element = element;
		this.dataRoot = dataRoot;

		this.loadingNoticeElement = $('<div id="bluemap-loading" class="box">loading...</div>').appendTo($(this.element));

		this.fileLoader = new FileLoader();
		this.blobLoader = new FileLoader();
		this.blobLoader.setResponseType('blob');
		this.bufferGeometryLoader = new BufferGeometryLoader();

		this.initStage();
		this.locationHash = '';
		this.controls = new Controls(this.camera, this.element, this.hiresScene);

		this.loadSettings().then(async () => {
			this.lowresTileManager = new TileManager(
				this,
				this.settings[this.map]['lowres']['viewDistance'],
				this.loadLowresTile,
				this.lowresScene,
				this.settings[this.map]['lowres']['tileSize'],
				{x: 0, z: 0}
			);

			this.hiresTileManager = new TileManager(
				this,
				this.settings[this.map]['hires']['viewDistance'],
				this.loadHiresTile,
				this.hiresScene,
				this.settings[this.map]['hires']['tileSize'],
				{x: 0, z: 0}
			);

			await this.loadHiresMaterial();
			await this.loadLowresMaterial();

			this.initModules();
			this.start();
		});
	}

	initModules() {
		this.modules = {};
		this.modules.compass = new Compass(this);
		this.modules.position = new Position(this);
		this.modules.mapMenu = new MapMenu(this);
		this.modules.info = new Info(this);
		this.modules.settings = new Settings(this);
	}

	changeMap(map) {
		this.hiresTileManager.close();
		this.lowresTileManager.close();

		this.map = map;
		this.controls.resetPosition();

		this.lowresTileManager = new TileManager(
			this,
			this.settings[this.map]['lowres']['viewDistance'],
			this.loadLowresTile,
			this.lowresScene,
			this.settings[this.map]['lowres']['tileSize'],
			{x: 0, z: 0}
		);

		this.hiresTileManager = new TileManager(
			this,
			this.settings[this.map]['hires']['viewDistance'],
			this.loadHiresTile,
			this.hiresScene,
			this.settings[this.map]['hires']['tileSize'],
			{x: 0, z: 0}
		);

		this.lowresTileManager.update();
		this.hiresTileManager.update();

		document.dispatchEvent(new Event('bluemap-map-change'));
	}

	loadLocationHash() {
		let hashVars = window.location.hash.substring(1).split(':');
		if (hashVars.length >= 1){
			if (this.settings[hashVars[0]] !== undefined && this.map !== hashVars[0]){
				this.changeMap(hashVars[0]);
			}
		}
		if (hashVars.length >= 3){
			let x = parseInt(hashVars[1]);
			let z = parseInt(hashVars[2]);
			if (!isNaN(x) && !isNaN(z)){
				this.controls.targetPosition.x = x + 0.5;
				this.controls.targetPosition.z = z + 0.5;
			}
		}
		if (hashVars.length >= 6){
			let dir = parseFloat(hashVars[3]);
			let dist = parseFloat(hashVars[4]);
			let angle = parseFloat(hashVars[5]);
			if (!isNaN(dir)) this.controls.targetDirection = dir;
			if (!isNaN(dist)) this.controls.targetDistance = dist;
			if (!isNaN(angle)) this.controls.targetAngle = angle;
			this.controls.direction = this.controls.targetDirection;
			this.controls.distance = this.controls.targetDistance;
			this.controls.angle = this.controls.targetAngle;
			this.controls.targetPosition.y = this.controls.minHeight;
			this.controls.position.copy(this.controls.targetPosition);
		}
		if (hashVars.length >= 7){
			let height = parseInt(hashVars[6]);
			if (!isNaN(height)){
				this.controls.minHeight = height;
				this.controls.targetPosition.y = height;
				this.controls.position.copy(this.controls.targetPosition);
			}
		}
	}

	start() {
		this.loadingNoticeElement.remove();

		this.loadLocationHash();

		$(window).on('hashchange', () => {
			if (this.locationHash === window.location.hash) return;
			this.loadLocationHash();
		});

		this.update();
		this.render();

		this.lowresTileManager.update();
		this.hiresTileManager.update();
	}

	update = () => {
		setTimeout(this.update, 1000);

		this.lowresTileManager.setPosition(this.controls.targetPosition);
		this.hiresTileManager.setPosition(this.controls.targetPosition);

		this.locationHash =
			'#' + this.map
			+ ':' + Math.floor(this.controls.targetPosition.x)
			+ ':' + Math.floor(this.controls.targetPosition.z)
			+ ':' + Math.round(this.controls.targetDirection * 100) / 100
			+ ':' + Math.round(this.controls.targetDistance * 100) / 100
			+ ':' + Math.ceil(this.controls.targetAngle * 100) / 100
			+ ':' + Math.floor(this.controls.targetPosition.y);
		history.replaceState(undefined, undefined, this.locationHash);
	};

	render = () => {
		requestAnimationFrame(this.render);

		if (this.controls.update()) this.updateFrame = true;

		if (!this.updateFrame) return;
		this.updateFrame = false;

		document.dispatchEvent(new Event('bluemap-update-frame'));

		this.skyboxCamera.rotation.copy(this.camera.rotation);
		this.skyboxCamera.updateProjectionMatrix();

		this.renderer.clear();
		this.renderer.render(this.skyboxScene, this.skyboxCamera, this.renderer.getRenderTarget(), false);
		this.renderer.clearDepth();
		this.renderer.render(this.lowresScene, this.camera, this.renderer.getRenderTarget(), false);
		if (this.camera.position.y < 400) {
			this.renderer.clearDepth();
			this.renderer.render(this.hiresScene, this.camera, this.renderer.getRenderTarget(), false);
		}
	}

	handleContainerResize = () => {
		this.camera.aspect = this.element.clientWidth / this.element.clientHeight;
		this.camera.updateProjectionMatrix();

		this.skyboxCamera.aspect = this.element.clientWidth / this.element.clientHeight;
		this.skyboxCamera.updateProjectionMatrix();

		this.renderer.setSize(this.element.clientWidth * this.quality, this.element.clientHeight * this.quality);
		$(this.renderer.domElement)
			.css('width', this.element.clientWidth)
			.css('height', this.element.clientHeight);

		this.updateFrame = true;
	}

	async loadSettings() {
		return new Promise(resolve => {
			this.fileLoader.load(this.dataRoot + 'settings.json', settings => {
				this.settings = JSON.parse(settings);
				this.maps = [];
				for (let map in this.settings) {
					if (this.settings.hasOwnProperty(map) && this.settings[map].enabled){
						this.maps.push(map);
					}
				}

				this.maps.sort((map1, map2) => {
					var sort = this.settings[map1].ordinal - this.settings[map2].ordinal;
					if (isNaN(sort)) return 0;
					return sort;
				});

				this.map = this.maps[0];
				resolve();
			});
		});
	}

	initStage() {
		this.updateFrame = true;
		this.quality = 1;

		this.renderer = new WebGLRenderer({
			alpha: true,
			antialias: true,
			sortObjects: false,
			preserveDrawingBuffer: true,
			logarithmicDepthBuffer: true,
		});
		this.renderer.autoClear = false;

		this.camera = new PerspectiveCamera(75, this.element.scrollWidth / this.element.scrollHeight, 0.1, 10000);
		this.camera.updateProjectionMatrix();

		this.skyboxCamera = this.camera.clone();
		this.skyboxCamera.updateProjectionMatrix();

		this.skyboxScene = new Scene();
		this.skyboxScene.ambient = new AmbientLight(0xffffff, 1);
		this.skyboxScene.add(this.skyboxScene.ambient);
		this.skyboxScene.add(this.createSkybox());

		this.lowresScene = new Scene();
		this.lowresScene.ambient = new AmbientLight(0xffffff, 0.6);
		this.lowresScene.add(this.lowresScene.ambient);
		this.lowresScene.sunLight = new DirectionalLight(0xccccbb, 0.7);
		this.lowresScene.sunLight.position.set(1, 5, 3);
		this.lowresScene.add(this.lowresScene.sunLight);

		this.hiresScene = new Scene();
		this.hiresScene.ambient = new AmbientLight(0xffffff, 1);
		this.hiresScene.add(this.hiresScene.ambient);
		this.hiresScene.sunLight = new DirectionalLight(0xccccbb, 0.2);
		this.hiresScene.sunLight.position.set(1, 5, 3);
		this.hiresScene.add(this.hiresScene.sunLight);

		this.element.append(this.renderer.domElement);
		this.handleContainerResize();

		$(window).resize(this.handleContainerResize);
	}

	createSkybox() {
		let geometry = new CubeGeometry(10, 10, 10);
		let material = [
			new MeshBasicMaterial({
				map: new TextureLoader().load(SKYBOX_SOUTH),
				side: BackSide
			}),
			new MeshBasicMaterial({
				map: new TextureLoader().load(SKYBOX_NORTH),
				side: BackSide
			}),
			new MeshBasicMaterial({
				map: new TextureLoader().load(SKYBOX_UP),
				side: BackSide
			}),
			new MeshBasicMaterial({
				map: new TextureLoader().load(SKYBOX_DOWN),
				side: BackSide
			}),
			new MeshBasicMaterial({
				map: new TextureLoader().load(SKYBOX_EAST),
				side: BackSide
			}),
			new MeshBasicMaterial({
				map: new TextureLoader().load(SKYBOX_WEST),
				side: BackSide
			})
		];
		return new Mesh(geometry, material);
	}

	async loadHiresMaterial() {
		return new Promise(resolve => {
			this.fileLoader.load(this.dataRoot + 'textures.json', textures => {
				textures = JSON.parse(textures);

				let materials = [];
				for (let i = 0; i < textures['textures'].length; i++) {
					let t = textures['textures'][i];

					let material = new MeshLambertMaterial({
						transparent: t['transparent'],
						alphaTest: 0.01,
						depthWrite: true,
						depthTest: true,
						blending: NormalBlending,
						vertexColors: VertexColors,
						side: FrontSide,
						wireframe: false
					});

					let texture = new Texture();
					texture.image = stringToImage(t['texture']);

					texture.premultiplyAlpha = false;
					texture.generateMipmaps = false;
					texture.magFilter = NearestFilter;
					texture.minFilter = NearestFilter;
					texture.wrapS = ClampToEdgeWrapping;
					texture.wrapT = ClampToEdgeWrapping;
					texture.flipY = false;
					texture.needsUpdate = true;
					texture.flatShading = true;

					material.map = texture;
					material.needsUpdate = true;

					materials[i] = material;
				}

				this.hiresMaterial = materials;

				resolve();
			});
		});
	}

	async loadLowresMaterial() {
		this.lowresMaterial = new MeshLambertMaterial({
			transparent: false,
			depthWrite: true,
			depthTest: true,
			vertexColors: VertexColors,
			side: FrontSide,
			wireframe: false
		});
	}

	async loadHiresTile(tileX, tileZ) {
		let path = this.dataRoot + this.map + '/hires/';
		path += pathFromCoords(tileX, tileZ);
		path += '.json';

		return new Promise((resolve, reject) => {
			this.bufferGeometryLoader.load(path, geometry => {
				let object = new Mesh(geometry, this.hiresMaterial);

				let tileSize = this.settings[this.map]['hires']['tileSize'];
				let translate = this.settings[this.map]['hires']['translate'];
				let scale = this.settings[this.map]['hires']['scale'];
				object.position.set(tileX * tileSize.x + translate.x, 0, tileZ * tileSize.z + translate.z);
				object.scale.set(scale.x, 1, scale.z);

				resolve(object);
			}, () => {
			}, reject);
		});
	}

	async loadLowresTile(tileX, tileZ) {
		let path = this.dataRoot + this.map + '/lowres/';
		path += pathFromCoords(tileX, tileZ);
		path += '.json';

		return new Promise((reslove, reject) => {
			this.bufferGeometryLoader.load(path, geometry => {
				let object = new Mesh(geometry, this.lowresMaterial);

				let tileSize = this.settings[this.map]['lowres']['tileSize'];
				let translate = this.settings[this.map]['lowres']['translate'];
				let scale = this.settings[this.map]['lowres']['scale'];
				object.position.set(tileX * tileSize.x + translate.x, 0, tileZ * tileSize.z + translate.z);
				object.scale.set(scale.x, 1, scale.z);

				reslove(object);
			}, () => {
			}, reject);
		})
	}

	// ###### UI ######

	alert(content) {
		let alertBox = $('#alert-box');
		if (alertBox.length === 0){
			alertBox = $('<div id="alert-box"></div>').appendTo(this.element);
		}

		let displayAlert = () => {
			let alert = $(`<div class="alert box" style="display: none;"><div class="alert-close-button"></div>${content}</div>`).appendTo(alertBox);
			alert.find('.alert-close-button').click(() => {
				alert.fadeOut(200, () => alert.remove());
			});
			alert.fadeIn(200);
		};

		let oldAlerts = alertBox.find('.alert');
		if (oldAlerts.length > 0){
			alertBox.fadeOut(200, () => {
				alertBox.html('');
				alertBox.show();
				displayAlert();
			});
		} else {
			displayAlert();
		}
	}
}
