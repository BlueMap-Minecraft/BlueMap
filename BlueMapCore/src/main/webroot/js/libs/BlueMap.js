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
	BackSide,
	BufferGeometryLoader,
	ClampToEdgeWrapping,
	SphereGeometry,
	FileLoader,
	FrontSide,
	Mesh,
	NearestFilter,
	NearestMipMapLinearFilter,
	PerspectiveCamera,
	Scene,
	ShaderMaterial,
	Texture,
	VertexColors,
	WebGLRenderer,
	Vector3,
} from 'three';

import UI from './ui/UI.js';

import Controls from './Controls.js';
import TileManager from './TileManager.js';

import HIRES_VERTEX_SHADER from './shaders/HiresVertexShader.js';
import HIRES_FRAGMENT_SHADER from './shaders/HiresFragmentShader.js';
import LOWRES_VERTEX_SHADER from './shaders/LowresVertexShader.js';
import LOWRES_FRAGMENT_SHADER from './shaders/LowresFragmentShader.js';
import SKY_VERTEX_SHADER from './shaders/SkyVertexShader.js';
import SKY_FRAGMENT_SHADER from './shaders/SkyFragmentShader.js';

import { stringToImage, pathFromCoords } from './utils.js';
import {getCookie, setCookie} from "./utils";

export default class BlueMap {
	constructor(element, dataRoot) {
		this.element = $('<div class="bluemap-container"></div>').appendTo(element)[0];
		this.dataRoot = dataRoot;

		this.hiresViewDistance = 160;
		this.lowresViewDistance = 3200;
		this.targetSunLightStrength = 1;
		this.sunLightStrength = {
			value: this.targetSunLightStrength
		};
		this.mobSpawnOverlay = {
			value: false
		};
		this.ambientLight = {
			value: 0
		};
		this.skyColor = {
			value: new Vector3(0, 0, 0)
		};
		this.debugInfo = false;

		this.ui = new UI(this);

		this.loadingNoticeElement = $('<div>loading...</div>').appendTo($(this.element));
		window.onerror = this.onLoadError;

		this.fileLoader = new FileLoader();
		this.blobLoader = new FileLoader();
		this.blobLoader.setResponseType('blob');
		this.bufferGeometryLoader = new BufferGeometryLoader();

		this.initStage();
		this.locationHash = '';
		this.controls = new Controls(this.camera, this.element, this.hiresScene);

		this.loadSettings().then(async () => {
			await this.loadHiresMaterial();
			await this.loadLowresMaterial();

			this.loadUserSettings();
			this.handleContainerResize();

			this.changeMap(this.maps[0]);

			this.ui.load();
			this.start();
		}).catch(error => {
			this.onLoadError(error.toString());
		});
	}

	changeMap(map) {
		if (this.map === map) return;

		if (this.hiresTileManager !== undefined) this.hiresTileManager.close();
		if (this.lowresTileManager !== undefined) this.lowresTileManager.close();

		this.map = map;

		let startPos = {
			x: this.settings.maps[this.map]["startPos"]["x"],
			z: this.settings.maps[this.map]["startPos"]["z"]
		};

		this.ambientLight.value = this.settings.maps[this.map]["ambientLight"];
		this.skyColor.value.set(
			this.settings.maps[this.map]["skyColor"].r,
			this.settings.maps[this.map]["skyColor"].g,
			this.settings.maps[this.map]["skyColor"].b
		);

		this.controls.setTileSize(this.settings.maps[this.map]['hires']['tileSize']);
		this.controls.resetPosition();
		this.controls.targetPosition.set(startPos.x, this.controls.targetPosition.y, startPos.z);
		this.controls.position.copy(this.controls.targetPosition);

		this.lowresTileManager = new TileManager(
			this,
			this.lowresViewDistance,
			this.loadLowresTile,
			this.lowresScene,
			this.settings.maps[this.map]['lowres']['tileSize'],
			this.settings.maps[this.map]['lowres']['translate'],
			startPos
		);

		this.hiresTileManager = new TileManager(
			this,
			this.hiresViewDistance,
			this.loadHiresTile,
			this.hiresScene,
			this.settings.maps[this.map]['hires']['tileSize'],
			this.settings.maps[this.map]['hires']['translate'],
			startPos
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

		this.saveUserSettings();

		this.lowresTileManager.setPosition(this.controls.targetPosition);
		if (this.camera.position.y < 400) {
			this.hiresTileManager.setPosition(this.controls.targetPosition);
		}

		this.locationHash =
				'#' + this.map
				+ ':' + Math.floor(this.controls.targetPosition.x)
				+ ':' + Math.floor(this.controls.targetPosition.z)
				+ ':' + Math.round(this.controls.targetDirection * 100) / 100
				+ ':' + Math.round(this.controls.targetDistance * 100) / 100
				+ ':' + Math.ceil(this.controls.targetAngle * 100) / 100
				+ ':' + Math.floor(this.controls.targetPosition.y);
		// only update hash when changed
		if (window.location.hash !== this.locationHash) {
			history.replaceState(undefined, undefined, this.locationHash);
		}
	};

	render = () => {
		requestAnimationFrame(this.render);

		//update controls
		if (this.controls.update()) this.updateFrame = true;

		//update lighting
		let targetLight = this.targetSunLightStrength;
		if (this.camera.position.y > 400){
			targetLight = Math.max(targetLight, 0.5);
		}
		if (Math.abs(targetLight - this.sunLightStrength.value) > 0.01) {
			this.sunLightStrength.value += (targetLight - this.sunLightStrength.value) * 0.1;
			this.updateFrame = true;
		}

		//don't render if nothing has changed
		if (!this.updateFrame) return;
		this.updateFrame = false;

		//render event
		document.dispatchEvent(new Event('bluemap-update-frame'));

		//render
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
	};

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
	};

	async loadSettings() {
		return new Promise(resolve => {
			this.fileLoader.load(this.dataRoot + 'settings.json', settings => {
				this.settings = JSON.parse(settings);
				this.maps = [];
				for (let map in this.settings.maps) {
					if (this.settings["maps"].hasOwnProperty(map) && this.settings.maps[map].enabled){
						this.maps.push(map);
					}
				}

				this.maps.sort((map1, map2) => {
					var sort = this.settings.maps[map1].ordinal - this.settings.maps[map2].ordinal;
					if (isNaN(sort)) return 0;
					return sort;
				});

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
		this.skyboxScene.add(this.createSkybox());

		this.lowresScene = new Scene();
		this.hiresScene = new Scene();

		$(this.renderer.domElement).addClass("map-canvas").appendTo(this.element);
		this.handleContainerResize();

		$(window).resize(this.handleContainerResize);
	}

	loadUserSettings(){
		if (!this.settings["useCookies"]) return;

		this.mobSpawnOverlay.value = this.loadUserSetting("mobSpawnOverlay", this.mobSpawnOverlay.value);
		this.targetSunLightStrength = this.loadUserSetting("sunLightStrength", this.targetSunLightStrength);
		this.quality = this.loadUserSetting("renderQuality", this.quality);
		this.hiresViewDistance = this.loadUserSetting("hiresViewDistance", this.hiresViewDistance);
		this.lowresViewDistance = this.loadUserSetting("lowresViewDistance", this.lowresViewDistance);
		this.debugInfo = this.loadUserSetting("debugInfo", this.debugInfo);
	}

	saveUserSettings(){
		if (!this.settings["useCookies"]) return;

		if (this.savedUserSettings === undefined) this.savedUserSettings = {};

		this.saveUserSetting("mobSpawnOverlay", this.mobSpawnOverlay.value);
		this.saveUserSetting("sunLightStrength", this.targetSunLightStrength);
		this.saveUserSetting("renderQuality", this.quality);
		this.saveUserSetting("hiresViewDistance", this.hiresViewDistance);
		this.saveUserSetting("lowresViewDistance", this.lowresViewDistance);
		this.saveUserSetting("debugInfo", this.debugInfo);
	}

	loadUserSetting(key, defaultValue){
		let value = getCookie("bluemap-" + key);

		if (value === undefined) return defaultValue;
		return value;
	}

	saveUserSetting(key, value){
		if (this.savedUserSettings[key] !== value){
			this.savedUserSettings[key] = value;
			setCookie("bluemap-" + key, value);
		}
	}

	createSkybox() {
		let geometry = new SphereGeometry(10, 10, 10);
		let material = new ShaderMaterial({
			uniforms: {
				sunlightStrength: this.sunLightStrength,
				ambientLight: this.ambientLight,
				skyColor: this.skyColor,
			},
			vertexShader: SKY_VERTEX_SHADER,
			fragmentShader: SKY_FRAGMENT_SHADER,
			side: BackSide
		});
		return new Mesh(geometry, material);
	}

	async loadHiresMaterial() {
		return new Promise(resolve => {
			this.fileLoader.load(this.dataRoot + 'textures.json', textures => {
				textures = JSON.parse(textures);
				let materials = [];
				for (let i = 0; i < textures['textures'].length; i++) {
					let t = textures['textures'][i];

					let opaque = t['color'][3] === 1;
					let transparent = t['transparent'];

					let texture = new Texture();
					texture.image = stringToImage(t['texture']);

					texture.anisotropy = 1;
					texture.generateMipmaps = opaque || transparent;
					texture.magFilter = NearestFilter;
					texture.minFilter = texture.generateMipmaps ? NearestMipMapLinearFilter : NearestFilter;
					texture.wrapS = ClampToEdgeWrapping;
					texture.wrapT = ClampToEdgeWrapping;
					texture.flipY = false;
					texture.flatShading = true;
					texture.needsUpdate = true;

					let uniforms = {
						texture: {
							type: 't',
							value: texture
						},
						sunlightStrength: this.sunLightStrength,
						mobSpawnOverlay: this.mobSpawnOverlay,
						ambientLight: this.ambientLight,
					};

					let material = new ShaderMaterial({
						uniforms: uniforms,
						vertexShader: HIRES_VERTEX_SHADER,
						fragmentShader: HIRES_FRAGMENT_SHADER,
						transparent: transparent,
						depthWrite: true,
						depthTest: true,
						vertexColors: VertexColors,
						side: FrontSide,
						wireframe: false,
					});

					material.needsUpdate = true;
					materials[i] = material;
				}

				this.hiresMaterial = materials;
				resolve();
			});
		});
	}

	async loadLowresMaterial() {
		this.lowresMaterial = new ShaderMaterial({
			uniforms: {
				sunlightStrength: this.sunLightStrength,
				ambientLight: this.ambientLight,
			},
			vertexShader: LOWRES_VERTEX_SHADER,
			fragmentShader: LOWRES_FRAGMENT_SHADER,
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

				let tileSize = this.settings.maps[this.map]['hires']['tileSize'];
				let translate = this.settings.maps[this.map]['hires']['translate'];
				let scale = this.settings.maps[this.map]['hires']['scale'];
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

				let tileSize = this.settings.maps[this.map]['lowres']['tileSize'];
				let translate = this.settings.maps[this.map]['lowres']['translate'];
				let scale = this.settings.maps[this.map]['lowres']['scale'];
				object.position.set(tileX * tileSize.x + translate.x, 0, tileZ * tileSize.z + translate.z);
				object.scale.set(scale.x, 1, scale.z);

				reslove(object);
			}, () => {
			}, reject);
		})
	}

	onLoadError = (message, url, line, col) => {
		this.loadingNoticeElement.remove();

		this.toggleAlert(undefined, `
		<div style="max-width: 50rem">
			<h1>Error</h1>
			<p style="color: red; font-family: monospace">${message}</p>
		</div>
		`);
	};

	toggleAlert(id, content) {
		let alertBox = $(this.element).find('.alert-box');
		if (alertBox.length === 0){
			alertBox = $('<div class="alert-box"></div>').appendTo(this.ui.hud);
		}

		let displayAlert = () => {
			let alert = $(`<div class="alert" data-alert-id="${id}" style="display: none;"><div class="close-button"></div>${content}</div>`).appendTo(alertBox);
			alert.find('.close-button').click(() => {
				alert.stop().fadeOut(200, () => alert.remove());
			});
			alert.stop().fadeIn(200);
		};

		if (id !== undefined) {
			let sameAlert = alertBox.find(`.alert[data-alert-id=${id}]`);
			if (sameAlert.length > 0) {
				alertBox.stop().fadeOut(200, () => {
					alertBox.html('');
					alertBox.show();
				});
				return;
			}
		}

		let oldAlerts = alertBox.find('.alert');
		if (oldAlerts.length > 0){
			alertBox.stop().fadeOut(200, () => {
				alertBox.html('');
				alertBox.show();
				displayAlert();
			});
			return;
		}

		displayAlert();
	}

}
