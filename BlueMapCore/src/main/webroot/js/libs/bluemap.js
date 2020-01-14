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
import $ from "jquery";
import * as THREE from "three";

import GEAR from "../../assets/gear.svg";
import COMPASS from "../../assets/compass.svg";

import SKYBOX_NORTH from "../../assets/skybox/north.png";
import SKYBOX_SOUTH from "../../assets/skybox/south.png";
import SKYBOX_EAST from "../../assets/skybox/east.png";
import SKYBOX_WEST from "../../assets/skybox/west.png";
import SKYBOX_UP from "../../assets/skybox/up.png";
import SKYBOX_DOWN from "../../assets/skybox/down.png";

const BlueMap = function (element, dataRoot) {
	this.element = element;
	this.dataRoot = dataRoot;

	this.loadingNoticeElement = $('<div id="bluemap-loading" class="box">loading...</div>').appendTo($(this.element));

	this.fileLoader = new THREE.FileLoader();
	this.blobLoader = new THREE.FileLoader();
	this.blobLoader.setResponseType("blob");
	this.bufferGeometryLoader = new THREE.BufferGeometryLoader();

	this.initStage();
	this.locationHash = "";
	this.controls = new BlueMap.Controls(this.camera, this.element, this.hiresScene);

	this.loadSettings(function () {
		this.lowresTileManager = new BlueMap.TileManager(
			this,
			this.settings[this.map]["lowres"]["viewDistance"],
			this.loadLowresTile,
			this.lowresScene,
			this.settings[this.map]["lowres"]["tileSize"],
			{x: 0, z: 0}
		);

		this.hiresTileManager = new BlueMap.TileManager(
			this,
			this.settings[this.map]["hires"]["viewDistance"],
			this.loadHiresTile,
			this.hiresScene,
			this.settings[this.map]["hires"]["tileSize"],
			{x: 0, z: 0}
		);

		this.loadHiresMaterial(function () {
			this.loadLowresMaterial(function () {
				this.initModules();
				this.start();
			});
		});
	});
};

BlueMap.prototype.initModules = function () {
	this.modules = {};

	this.modules.compass = new BlueMap.Module.Compass(this);
	this.modules.position = new BlueMap.Module.Position(this);
	this.modules.mapMenu = new BlueMap.Module.MapMenu(this);
	this.modules.info = new BlueMap.Module.Info(this);
	this.modules.settings = new BlueMap.Module.Settings(this);
};

BlueMap.prototype.changeMap = function (map) {
	this.hiresTileManager.close();
	this.lowresTileManager.close();

	this.map = map;
	this.controls.resetPosition();

	this.lowresTileManager = new BlueMap.TileManager(
		this,
		this.settings[this.map]["lowres"]["viewDistance"],
		this.loadLowresTile,
		this.lowresScene,
		this.settings[this.map]["lowres"]["tileSize"],
		{x: 0, z: 0}
	);

	this.hiresTileManager = new BlueMap.TileManager(
		this,
		this.settings[this.map]["hires"]["viewDistance"],
		this.loadHiresTile,
		this.hiresScene,
		this.settings[this.map]["hires"]["tileSize"],
		{x: 0, z: 0}
	);

	this.lowresTileManager.update();
	this.hiresTileManager.update();

	document.dispatchEvent(new Event("bluemap-map-change"));
};

BlueMap.prototype.loadLocationHash = function(){
	let hashVars = window.location.hash.substring(1).split(":");
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
};

BlueMap.prototype.start = function () {
	let scope = this;

	this.loadingNoticeElement.remove();

	this.loadLocationHash();

	$(window).on("hashchange", function(evt){
		if (scope.locationHash === window.location.hash) return;
		scope.loadLocationHash();
	});

	this.update();
	this.render();

	this.lowresTileManager.update();
	this.hiresTileManager.update();
};

BlueMap.prototype.update = function () {
	let scope = this;
	setTimeout(function () {
		scope.update()
	}, 1000);

	this.lowresTileManager.setPosition(this.controls.targetPosition);
	this.hiresTileManager.setPosition(this.controls.targetPosition);

	this.locationHash =
		"#" + this.map
		+ ":" + Math.floor(this.controls.targetPosition.x)
		+ ":" + Math.floor(this.controls.targetPosition.z)
		+ ":" + Math.round(this.controls.targetDirection * 100) / 100
		+ ":" + Math.round(this.controls.targetDistance * 100) / 100
		+ ":" + Math.ceil(this.controls.targetAngle * 100) / 100
		+ ":" + Math.floor(this.controls.targetPosition.y);
	history.replaceState(undefined, undefined, this.locationHash);
};

BlueMap.prototype.render = function () {
	let scope = this;
	requestAnimationFrame(function () {
		scope.render()
	});

	if (this.controls.update()) this.updateFrame = true;

	if (!this.updateFrame) return;
	this.updateFrame = false;

	document.dispatchEvent(new Event("bluemap-update-frame"));

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

BlueMap.prototype.handleContainerResize = function () {
	this.camera.aspect = this.element.clientWidth / this.element.clientHeight;
	this.camera.updateProjectionMatrix();

	this.skyboxCamera.aspect = this.element.clientWidth / this.element.clientHeight;
	this.skyboxCamera.updateProjectionMatrix();

	this.renderer.setSize(this.element.clientWidth * this.quality, this.element.clientHeight * this.quality);
	$(this.renderer.domElement)
		.css("width", this.element.clientWidth)
		.css("height", this.element.clientHeight);

	this.updateFrame = true;
};

BlueMap.prototype.loadSettings = function (callback) {
	let scope = this;

	this.fileLoader.load(this.dataRoot + "settings.json", function (settings) {
		scope.settings = JSON.parse(settings);

		scope.maps = [];
		for (let map in scope.settings){
			if (scope.settings.hasOwnProperty(map) && scope.settings[map].enabled){
				scope.maps.push(map);
			}
		}

		scope.maps.sort(function (map1, map2) {
			var sort = scope.settings[map1].ordinal - scope.settings[map2].ordinal;
			if (isNaN(sort)) return 0;
			return sort;
		});

		scope.map = scope.maps[0];

		callback.call(scope);
	});
};

BlueMap.prototype.initStage = function () {
	let scope = this;

	this.updateFrame = true;
	this.quality = 1;

	this.renderer = new THREE.WebGLRenderer({
		alpha: true,
		antialias: true,
		sortObjects: false,
		preserveDrawingBuffer: true,
		logarithmicDepthBuffer: true,
	});
	this.renderer.autoClear = false;

	this.camera = new THREE.PerspectiveCamera(75, this.element.scrollWidth / this.element.scrollHeight, 0.1, 10000);
	this.camera.updateProjectionMatrix();

	this.skyboxCamera = this.camera.clone();
	this.skyboxCamera.updateProjectionMatrix();

	this.skyboxScene = new THREE.Scene();
	this.skyboxScene.ambient = new THREE.AmbientLight(0xffffff, 1);
	this.skyboxScene.add(this.skyboxScene.ambient);
	this.skyboxScene.add(this.createSkybox());

	this.lowresScene = new THREE.Scene();
	this.lowresScene.ambient = new THREE.AmbientLight(0xffffff, 0.6);
	this.lowresScene.add(this.lowresScene.ambient);
	this.lowresScene.sunLight = new THREE.DirectionalLight(0xccccbb, 0.7);
	this.lowresScene.sunLight.position.set(1, 5, 3);
	this.lowresScene.add(this.lowresScene.sunLight);

	this.hiresScene = new THREE.Scene();
	this.hiresScene.ambient = new THREE.AmbientLight(0xffffff, 1);
	this.hiresScene.add(this.hiresScene.ambient);
	this.hiresScene.sunLight = new THREE.DirectionalLight(0xccccbb, 0.2);
	this.hiresScene.sunLight.position.set(1, 5, 3);
	this.hiresScene.add(this.hiresScene.sunLight);

	this.element.append(this.renderer.domElement);
	this.handleContainerResize();

	$(window).resize(function () {
		scope.handleContainerResize()
	});
};

BlueMap.prototype.createSkybox = function(){
	let geometry = new THREE.CubeGeometry(10, 10, 10);
	let material = [
		new THREE.MeshBasicMaterial({
			map: new THREE.TextureLoader().load(SKYBOX_SOUTH),
			side: THREE.BackSide
		}),
		new THREE.MeshBasicMaterial({
			map: new THREE.TextureLoader().load(SKYBOX_NORTH),
			side: THREE.BackSide
		}),
		new THREE.MeshBasicMaterial({
			map: new THREE.TextureLoader().load(SKYBOX_UP),
			side: THREE.BackSide
		}),
		new THREE.MeshBasicMaterial({
			map: new THREE.TextureLoader().load(SKYBOX_DOWN),
			side: THREE.BackSide
		}),
		new THREE.MeshBasicMaterial({
			map: new THREE.TextureLoader().load(SKYBOX_EAST),
			side: THREE.BackSide
		}),
		new THREE.MeshBasicMaterial({
			map: new THREE.TextureLoader().load(SKYBOX_WEST),
			side: THREE.BackSide
		})
	];
	return new THREE.Mesh(geometry, material);
};

BlueMap.prototype.loadHiresMaterial = function (callback) {
	let scope = this;

	this.fileLoader.load(this.dataRoot + "textures.json", function (textures) {
		textures = JSON.parse(textures);

		let materials = [];
		for (let i = 0; i < textures["textures"].length; i++) {
			let t = textures["textures"][i];

			let material = new THREE.MeshLambertMaterial({
				transparent: t["transparent"],
				alphaTest: 0.01,
				depthWrite: true,
				depthTest: true,
				blending: THREE.NormalBlending,
				vertexColors: THREE.VertexColors,
				side: THREE.FrontSide,
				wireframe: false
			});

			let texture = new THREE.Texture();
			texture.image = BlueMap.utils.stringToImage(t["texture"]);

			texture.premultiplyAlpha = false;
			texture.generateMipmaps = false;
			texture.magFilter = THREE.NearestFilter;
			texture.minFilter = THREE.NearestFilter;
			texture.wrapS = THREE.ClampToEdgeWrapping;
			texture.wrapT = THREE.ClampToEdgeWrapping;
			texture.flipY = false;
			texture.needsUpdate = true;
			texture.flatShading = true;

			material.map = texture;
			material.needsUpdate = true;

			materials[i] = material;
		}

		scope.hiresMaterial = materials;

		callback.call(scope);
	});
};

BlueMap.prototype.loadLowresMaterial = function (callback) {
	this.lowresMaterial = new THREE.MeshLambertMaterial({
		transparent: false,
		depthWrite: true,
		depthTest: true,
		vertexColors: THREE.VertexColors,
		side: THREE.FrontSide,
		wireframe: false
	});

	callback.call(this);
};

BlueMap.prototype.loadHiresTile = function (tileX, tileZ, callback, onError) {
	let scope = this;

	let path = this.dataRoot + this.map + "/hires/";
	path += BlueMap.utils.pathFromCoords(tileX, tileZ);
	path += ".json";


	this.bufferGeometryLoader.load(path, function (geometry) {
		let object = new THREE.Mesh(geometry, scope.hiresMaterial);

		let tileSize = scope.settings[scope.map]["hires"]["tileSize"];
		let translate = scope.settings[scope.map]["hires"]["translate"];
		let scale = scope.settings[scope.map]["hires"]["scale"];
		object.position.set(tileX * tileSize.x + translate.x, 0, tileZ * tileSize.z + translate.z);
		object.scale.set(scale.x, 1, scale.z);

		callback.call(scope, object);
	}, function () {

	}, function (error) {
		onError.call(scope, error);
	});
};

BlueMap.prototype.loadLowresTile = function (tileX, tileZ, callback, onError) {
	let scope = this;

	let path = this.dataRoot + this.map + "/lowres/";
	path += BlueMap.utils.pathFromCoords(tileX, tileZ);
	path += ".json";

	this.bufferGeometryLoader.load(path, function (geometry) {
		let object = new THREE.Mesh(geometry, scope.lowresMaterial);

		let tileSize = scope.settings[scope.map]["lowres"]["tileSize"];
		let translate = scope.settings[scope.map]["lowres"]["translate"];
		let scale = scope.settings[scope.map]["lowres"]["scale"];
		object.position.set(tileX * tileSize.x + translate.x, 0, tileZ * tileSize.z + translate.z);
		object.scale.set(scale.x, 1, scale.z);

		callback.call(scope, object);
	}, function () {

	}, function (error) {
		onError.call(scope, error);
	});
};


// ###### UI ######

BlueMap.prototype.alert = function (content) {
	let alertBox = $("#alert-box");
	if (alertBox.length === 0){
		alertBox = $('<div id="alert-box"></div>').appendTo(this.element);
	}

	let displayAlert = function(){
		let alert = $('<div class="alert box" style="display: none;"><div class="alert-close-button"></div>' + content + "</div>").appendTo(alertBox);
		alert.find(".alert-close-button").click(function(){
			alert.fadeOut(200, function(){
				alert.remove();
			});
		});
		alert.fadeIn(200);
	};

	let oldAlerts = alertBox.find(".alert");
	if (oldAlerts.length > 0){
		alertBox.fadeOut(200, function () {
			alertBox.html("");
			alertBox.show();
			displayAlert();
		})
	} else {
		displayAlert();
	}
};

// ###### TileManager ######
BlueMap.TileManager = function (blueMap, viewDistance, tileLoader, scene, tileSize, position) {
	this.blueMap = blueMap;
	this.viewDistance = viewDistance;
	this.tileLoader = tileLoader;
	this.scene = scene;
	this.tileSize = new THREE.Vector2(tileSize.x, tileSize.z);

	this.tile = new THREE.Vector2(position.x, position.z);
	this.lastTile = this.tile.clone();

	this.closed = false;
	this.currentlyLoading = 0;
	this.updateTimeout = null;

	this.tiles = {};
};

BlueMap.TileManager.prototype.setPosition = function (center) {
	this.tile.set(center.x, center.z).divide(this.tileSize).floor();

	if (!this.tile.equals(this.lastTile) && !this.closed) {
		this.update();
		this.lastTile.copy(this.tile);
	}
};

BlueMap.TileManager.prototype.update = function () {
	if (this.closed) return;

	//free a loader so if there was an error loading a tile we don"t get stuck forever with the blocked loading process
	this.currentlyLoading--;
	if (this.currentlyLoading < 0) this.currentlyLoading = 0;

	this.removeFarTiles();
	this.loadCloseTiles();
};

BlueMap.TileManager.prototype.removeFarTiles = function () {
	let keys = Object.keys(this.tiles);
	for (let i = 0; i < keys.length; i++) {
		if (!this.tiles.hasOwnProperty(keys[i])) continue;

		let tile = this.tiles[keys[i]];

		let vd = this.viewDistance;

		if (
			tile.x + vd < this.tile.x ||
			tile.x - vd > this.tile.x ||
			tile.z + vd < this.tile.y ||
			tile.z - vd > this.tile.y
		) {
			tile.disposeModel();
			delete this.tiles[keys[i]];
		}
	}
};

BlueMap.TileManager.prototype.removeAllTiles = function () {
	let keys = Object.keys(this.tiles);
	for (let i = 0; i < keys.length; i++) {
		if (!this.tiles.hasOwnProperty(keys[i])) continue;

		let tile = this.tiles[keys[i]];
		tile.disposeModel();
		delete this.tiles[keys[i]];
	}
};

BlueMap.TileManager.prototype.close = function () {
	this.closed = true;
	this.removeAllTiles();
};

BlueMap.TileManager.prototype.loadCloseTiles = function () {
	if (this.closed) return;

	let scope = this;

	if (this.currentlyLoading < 8) {
		if (!this.loadNextTile()) return;
	}

	if (this.updateTimeout) clearTimeout(this.updateTimeout);
	this.updateTimeout = setTimeout(function () {
		scope.loadCloseTiles()
	}, 0);
};

BlueMap.TileManager.prototype.loadNextTile = function () {
	let x = 0;
	let z = 0;
	let d = 1;
	let m = 1;

	while (m < this.viewDistance * 2) {
		while (2 * x * d < m) {
			if (this.tryLoadTile(this.tile.x + x, this.tile.y + z)) return true;
			x = x + d;
		}
		while (2 * z * d < m) {
			if (this.tryLoadTile(this.tile.x + x, this.tile.y + z)) return true;
			z = z + d;
		}
		d = -1 * d;
		m = m + 1;
	}

	return false;
};

BlueMap.TileManager.prototype.tryLoadTile = function (x, z) {
	if (this.closed) return;

	let scope = this;

	let tileHash = BlueMap.utils.hashTile(x, z);

	let tile = this.tiles[tileHash];
	if (tile !== undefined) return false;

	tile = new BlueMap.Tile(this.scene, x, z);
	tile.isLoading = true;

	this.currentlyLoading++;

	this.tiles[tileHash] = tile;

	this.tileLoader.call(this.blueMap, x, z, function (model) {
		tile.isLoading = false;

		if (tile.disposed || scope.closed) {
			model.geometry.dispose();
			tile.disposeModel();
			delete scope.tiles[tileHash];
			return;
		}

		scope.tiles[tileHash] = tile;
		tile.setModel(model);

		scope.blueMap.updateFrame = true;

		scope.currentlyLoading--;
		if (scope.currentlyLoading < 0) scope.currentlyLoading = 0;
	}, function (error) {
		tile.isLoading = false;
		tile.disposeModel();

		scope.currentlyLoading--;

		//console.log("Failed to load tile: ", x, z);
	});

	return true;
};


// ###### Tile ######
BlueMap.Tile = function (scene, x, z) {
	this.scene = scene;
	this.x = x;
	this.z = z;

	this.isLoading = false;
	this.disposed = false;

	this.model = null;
};

BlueMap.Tile.prototype.setModel = function (model) {
	this.disposeModel();

	if (model) {
		this.model = model;
		this.scene.add(model);

		//console.log("Added tile:", this.x, this.z);
	}
};

BlueMap.Tile.prototype.disposeModel = function () {
	this.disposed = true;

	if (this.model) {
		this.scene.remove(this.model);
		this.model.geometry.dispose();
		delete this.model;

		//console.log("Removed tile:", this.x, this.z);
	}
};


// ###### Controls ######

/**
 * targetHeightScene and cameraHeightScene are scenes of objects that are checked via raycasting for a height for the target and the camera
 */
BlueMap.Controls = function (camera, element, heightScene) {
	let scope = this;

	this.settings = {
		zoom: {
			min: 10,
			max: 2000,
			speed: 1.5,
			smooth: 0.2,
		},
		move: {
			speed: 1.75,
			smooth: 0.3,
			smoothY: 0.075,
		},
		tilt: {
			max: Math.PI / 2.1,
			speed: 1.5,
			smooth: 0.3,
		},
		rotate: {
			speed: 1.5,
			smooth: 0.3,
		}
	};

	this.camera = camera;
	this.element = element;
	this.heightScene = heightScene;
	this.minHeight = 0;

	this.raycaster = new THREE.Raycaster();
	this.rayDirection = new THREE.Vector3(0, -1, 0);

	this.resetPosition();

	this.mouse = new THREE.Vector2(0, 0);
	this.lastMouse = new THREE.Vector2(0, 0);
	this.deltaMouse = new THREE.Vector2(0, 0);

	//variables used to calculate with (to prevent object creation every update)
	this.orbitRot = new THREE.Euler(0, 0, 0, "YXZ");
	this.cameraPosDelta = new THREE.Vector3(0, 0, 0);
	this.moveDelta = new THREE.Vector2(0, 0);

	this.keyStates = {};

	this.KEYS = {
		LEFT: 37,
		UP: 38,
		RIGHT: 39,
		DOWN: 40,
		ORBIT: THREE.MOUSE.RIGHT,
		MOVE: THREE.MOUSE.LEFT
	};
	this.STATES = {
		NONE: -1,
		ORBIT: 0,
		MOVE: 1,
	};

	this.state = this.STATES.NONE;

	let canvas = $(this.element).find("canvas").get(0);
	window.addEventListener("contextmenu", function (e) {
		e.preventDefault();
	}, false);
	canvas.addEventListener("mousedown", function (e) {
		scope.onMouseDown(e);
	}, false);
	window.addEventListener("mousemove", function (e) {
		scope.onMouseMove(e);
	}, false);
	window.addEventListener("mouseup", function (e) {
		scope.onMouseUp(e);
	}, false);
	canvas.addEventListener("wheel", function (e) {
		scope.onMouseWheel(e);
	}, false);
	window.addEventListener("keydown", function (e) {
		scope.onKeyDown(e);
	}, false);
	window.addEventListener("keyup", function (e) {
		scope.onKeyUp(e);
	}, false);

	this.camera.position.set(0, 1000, 0);
	this.camera.lookAt(this.position);
	this.camera.updateProjectionMatrix();
};

BlueMap.Controls.prototype.resetPosition = function () {
	this.position = new THREE.Vector3(0, 70, 0);
	this.targetPosition = new THREE.Vector3(0, 70, 0);

	this.distance = 5000;
	this.targetDistance = 1000;

	this.direction = 0;
	this.targetDirection = 0;

	this.angle = 0;
	this.targetAngle = 0;
};

BlueMap.Controls.prototype.update = function () {
	this.updateMouseMoves();

	let changed = false;

	let zoomLerp = (this.distance - 100) / 200;
	if (zoomLerp < 0) zoomLerp = 0;
	if (zoomLerp > 1) zoomLerp = 1;
	this.targetPosition.y = 300 * zoomLerp + this.minHeight * (1 - zoomLerp);

	this.position.x += (this.targetPosition.x - this.position.x) * this.settings.move.smooth;
	this.position.y += (this.targetPosition.y - this.position.y) * this.settings.move.smoothY;
	this.position.z += (this.targetPosition.z - this.position.z) * this.settings.move.smooth;

	this.distance += (this.targetDistance - this.distance) * this.settings.zoom.smooth;

	let deltaDir = (this.targetDirection - this.direction) * this.settings.rotate.smooth;
	this.direction += deltaDir;
	changed = changed || Math.abs(deltaDir) > 0.001;

	let max = Math.min(this.settings.tilt.max, this.settings.tilt.max - Math.pow(((this.distance - this.settings.zoom.min) / (this.settings.zoom.max - this.settings.zoom.min)) * Math.pow(this.settings.tilt.max, 4), 1/4));
	if (this.targetAngle > max) this.targetAngle = max;
	if (this.targetAngle < 0.01) this.targetAngle = 0.001;
	let deltaAngle = (this.targetAngle - this.angle) * this.settings.tilt.smooth;
	this.angle += deltaAngle;
	changed = changed || Math.abs(deltaAngle) > 0.001;

	let last = this.camera.position.x + this.camera.position.y + this.camera.position.z;
	this.orbitRot.set(this.angle, this.direction, 0);
	this.cameraPosDelta.set(0, this.distance, 0).applyEuler(this.orbitRot);

	this.camera.position.set(this.position.x + this.cameraPosDelta.x, this.position.y + this.cameraPosDelta.y, this.position.z + this.cameraPosDelta.z);
	let move = last - (this.camera.position.x + this.camera.position.y + this.camera.position.z);

	changed = changed || Math.abs(move) > 0.001;

	if (changed) {
		this.camera.lookAt(this.position);
		this.camera.updateProjectionMatrix();

		this.updateHeights();
	}

	return changed;
};

BlueMap.Controls.prototype.updateHeights = function(){
		//TODO: this can be performance-improved by only intersecting the correct tile?

		let rayStart = new THREE.Vector3(this.targetPosition.x, 300, this.targetPosition.z);
		this.raycaster.set(rayStart, this.rayDirection);
		this.raycaster.near = 1;
		this.raycaster.far = 300;
		let intersects = this.raycaster.intersectObjects(this.heightScene.children);
		if (intersects.length > 0){
			this.minHeight = intersects[0].point.y;
			//this.targetPosition.y = this.minHeight;
		} else {
			//this.targetPosition.y = 0;
		}

		rayStart.set(this.camera.position.x, 300, this.camera.position.z);
		this.raycaster.set(rayStart, this.rayDirection);
		intersects.length = 0;
		intersects = this.raycaster.intersectObjects(this.heightScene.children);
		if (intersects.length > 0){
			if (intersects[0].point.y > this.minHeight){
				this.minHeight = intersects[0].point.y;
			}
		}
};

BlueMap.Controls.prototype.updateMouseMoves = function (e) {
	this.deltaMouse.set(this.lastMouse.x - this.mouse.x, this.lastMouse.y - this.mouse.y);

	this.moveDelta.x = 0;
	this.moveDelta.y = 0;

	if (this.keyStates[this.KEYS.UP]){
		this.moveDelta.y -= 20;
	}
	if (this.keyStates[this.KEYS.DOWN]){
		this.moveDelta.y += 20;
	}
	if (this.keyStates[this.KEYS.LEFT]){
		this.moveDelta.x -= 20;
	}
	if (this.keyStates[this.KEYS.RIGHT]){
		this.moveDelta.x += 20;
	}

	if (this.state === this.STATES.MOVE) {
		if (this.deltaMouse.x === 0 && this.deltaMouse.y === 0) return;
		this.moveDelta.copy(this.deltaMouse);
	}

	if (this.moveDelta.x !== 0 || this.moveDelta.y !== 0) {
		this.moveDelta.rotateAround(BlueMap.utils.Vector2.ZERO, -this.direction);
		this.targetPosition.set(
			this.targetPosition.x + (this.moveDelta.x * this.distance / this.element.clientHeight * this.settings.move.speed),
			this.targetPosition.y,
			this.targetPosition.z + (this.moveDelta.y * this.distance / this.element.clientHeight * this.settings.move.speed)
		);
	}

	if (this.state === this.STATES.ORBIT) {
		this.targetDirection += (this.deltaMouse.x / this.element.clientHeight * Math.PI);
		this.targetAngle += (this.deltaMouse.y / this.element.clientHeight * Math.PI);
	}

	this.lastMouse.copy(this.mouse);
};

BlueMap.Controls.prototype.onMouseWheel = function (e) {
	if (e.deltaY > 0) {
		this.targetDistance *= this.settings.zoom.speed;
	} else if (e.deltaY < 0) {
		this.targetDistance /= this.settings.zoom.speed;
	}

	if (this.targetDistance < this.settings.zoom.min) this.targetDistance = this.settings.zoom.min;
	if (this.targetDistance > this.settings.zoom.max) this.targetDistance = this.settings.zoom.max;
};

BlueMap.Controls.prototype.onMouseMove = function (e) {
	this.mouse.set(e.clientX, e.clientY);

	if (this.state !== this.STATES.NONE){
		e.preventDefault();
	}
};

BlueMap.Controls.prototype.onMouseDown = function (e) {
	if (this.state !== this.STATES.NONE) return;

	switch (e.button) {
		case this.KEYS.MOVE :
			this.state = this.STATES.MOVE;
			e.preventDefault();
			break;
		case this.KEYS.ORBIT :
			this.state = this.STATES.ORBIT;
			e.preventDefault();
			break;
	}
};

BlueMap.Controls.prototype.onMouseUp = function (e) {
	if (this.state === this.STATES.NONE) return;

	switch (e.button) {
		case this.KEYS.MOVE :
			if (this.state === this.STATES.MOVE) this.state = this.STATES.NONE;
			break;
		case this.KEYS.ORBIT :
			if (this.state === this.STATES.ORBIT) this.state = this.STATES.NONE;
			break;
	}
};

BlueMap.Controls.prototype.onKeyDown = function (e) {
	this.keyStates[e.keyCode] = true;
};

BlueMap.Controls.prototype.onKeyUp = function (e) {
	this.keyStates[e.keyCode] = false;
};

// ###### Modules ######
BlueMap.Module = {
	getTopRightElement: function(blueMap){
		let element = $("#bluemap-topright");

		if (element.length === 0){
			element = $('<div id="bluemap-topright" class="box"></div>').appendTo(blueMap.element);
		}

		return element;
	},

	getTopLeftElement: function(blueMap){
		let element = $("#bluemap-topleft");

		if (element.length === 0){
			element = $('<div id="bluemap-topleft" class="box"></div>').appendTo(blueMap.element);
		}

		return element;
	}
};

// ###### Modules.MapMenu ######
BlueMap.Module.MapMenu = function (blueMap) {
	let scope = this;

	this.bluemap = blueMap;
	let maps = this.bluemap.settings;

	$("#bluemap-mapmenu").remove();
	this.element = $('<div id="bluemap-mapmenu" class="dropdown-container"><span class="selection">' + maps[this.bluemap.map].name + "</span></div>").appendTo(BlueMap.Module.getTopLeftElement(blueMap));

	let dropdown = $('<div class="dropdown"></div>').appendTo(this.element);
	this.maplist = $("<ul></ul>").appendTo(dropdown);

	for (let mapId in maps) {
		if (!maps.hasOwnProperty(mapId)) continue;
		if (!maps.enabled) continue;

		let map = maps[mapId];
		$('<li map="" + mapId + "">' + map.name + "</li>").appendTo(this.maplist);
	}

	this.maplist.find("li[map=" + this.bluemap.map + "]").hide();
	this.maplist.find("li[map]").click(function (e) {
		let map = $(this).attr("map");
		scope.bluemap.changeMap(map);
	});

	$(document).on("bluemap-map-change", function(){
		scope.maplist.find("li").show();
		scope.maplist.find("li[map=" + scope.bluemap.map + "]").hide();

		scope.element.find(".selection").html(scope.bluemap.settings[scope.bluemap.map].name);
	});
};

// ###### Modules.Compass ######
BlueMap.Module.Compass = function (blueMap) {
	let scope = this;

	this.blueMap = blueMap;

	$("#bluemap-compass").remove();
	this.element = $(`<div id="bluemap-compass" class="button"><img id="bluemap-compass-needle" src="${COMPASS}" /></div>`).appendTo(BlueMap.Module.getTopLeftElement(blueMap));
	this.needle = $("#bluemap-compass-needle");

	$(document).on("bluemap-update-frame", function (){
		scope.needle.css("transform", "rotate(" + scope.blueMap.controls.direction + "rad)");
	});

	$(this.element).click(function(){
		scope.blueMap.controls.targetDirection = 0;
		scope.blueMap.controls.direction = scope.blueMap.controls.direction % (Math.PI * 2);
		if (scope.blueMap.controls.direction < -Math.PI) scope.blueMap.controls.direction += Math.PI * 2;
		if (scope.blueMap.controls.direction > Math.PI) scope.blueMap.controls.direction -= Math.PI * 2;
	});
};

// ###### Modules.Position ######
BlueMap.Module.Position = function (blueMap) {
	let scope = this;

	this.blueMap = blueMap;

	let parent = BlueMap.Module.getTopLeftElement(blueMap);

	$(".bluemap-position").remove();
	this.elementX = $('<div class="bluemap-position pos-x">0</div>').appendTo(parent);
	//this.elementY = $('<div class="bluemap-position pos-y">0</div>').appendTo(parent);
	this.elementZ = $('<div class="bluemap-position pos-z">0</div>').appendTo(parent);

	$(document).on("bluemap-update-frame", function (){
		scope.elementX.html(Math.floor(scope.blueMap.controls.targetPosition.x));
		//scope.elementY.html(scope.blueMap.controls.targetPosition.y === 0 ? "-" : Math.floor(scope.blueMap.controls.targetPosition.y));
		scope.elementZ.html(Math.floor(scope.blueMap.controls.targetPosition.z));
	});
};

// ###### Modules.Settings ######
BlueMap.Module.Settings = function (blueMap) {
	let scope = this;

	this.blueMap = blueMap;

	let parent = BlueMap.Module.getTopRightElement(blueMap);

	$("#bluemap-settings").remove();
	this.elementMenu = $('<div id="bluemap-settings-container" style="display: none"></div>').appendTo(parent);
	this.elementSettings = $(`<div id="bluemap-settings" class="button"><img src="${GEAR}" /></div>`).appendTo(parent);
	this.elementSettings.click(function(){
		if (scope.elementMenu.css("display") === "none"){
			scope.elementSettings.addClass("active");
		} else {
			scope.elementSettings.removeClass("active");
		}

		scope.elementMenu.animate({
			width: "toggle"
		}, 200);
	});


	/* Quality */

	this.elementQuality = $(
		'<div id="bluemap-settings-quality" class="dropdown-container"><span class="selection">Quality: <span>Normal</span></span><div class="dropdown"><ul>' +
		'<li quality="2">High</li>' +
		'<li quality="1" style="display: none">Normal</li>' +
		'<li quality="0.75">Fast</li>' +
		'</ul></div></div>'
	).prependTo(this.elementMenu);

	this.elementQuality.find("li[quality]").click(function(){
		let desc = $(this).html();
		scope.blueMap.quality = parseFloat($(this).attr("quality"));

		scope.elementQuality.find("li").show();
		scope.elementQuality.find("li[quality=\"" + scope.blueMap.quality + "\"]").hide();

		scope.elementQuality.find(".selection > span").html(desc);

		scope.blueMap.handleContainerResize();
	});


	/* Render Distance */

	this.pctToRenderDistance = function ( value , defaultValue ) {
		let max = defaultValue * 5;
		if (max > 20) max = 20;

		return THREE.Math.mapLinear(value, 0, 100, 1, max);
	};

	this.renderDistanceToPct = function ( value , defaultValue ) {
		let max = defaultValue * 5;
		if (max > 20) max = 20;

		return THREE.Math.mapLinear(value, 1, max, 0, 100);
	};

	this.init = function(){
		scope.defaultHighRes = scope.blueMap.hiresTileManager.viewDistance;
		scope.defaultLowRes = scope.blueMap.lowresTileManager.viewDistance;

		scope.elementRenderDistance.html(
			'<span class="selection">View Distance: <span>' + scope.blueMap.hiresTileManager.viewDistance + '</span></span>' +
			'<div class="dropdown">' +
			'<input type="range" min="0" max="100" step="1" value="' + scope.renderDistanceToPct(scope.blueMap.hiresTileManager.viewDistance, scope.defaultHighRes) + '" />' +
			'</div>'
		);

		let slider = scope.elementRenderDistance.find("input");
		slider.on("change input", function(e){
			scope.blueMap.hiresTileManager.viewDistance = scope.pctToRenderDistance(parseFloat(slider.val()), scope.defaultHighRes);
			scope.blueMap.lowresTileManager.viewDistance = scope.pctToRenderDistance(parseFloat(slider.val()), scope.defaultLowRes);
			scope.elementRenderDistance.find(".selection > span").html(Math.round(scope.blueMap.hiresTileManager.viewDistance * 10) / 10);

			scope.blueMap.lowresTileManager.update();
			scope.blueMap.hiresTileManager.update();
		});
	};

	this.elementRenderDistance = $(
		'<div id="bluemap-settings-render-distance" class="dropdown-container">' +
		'</div>'
	).prependTo(this.elementMenu);

	scope.init();

	$(document).on("bluemap-map-change", this.init);
};

// ###### Modules.Info ######
BlueMap.Module.Info = function (blueMap) {
	let scope = this;

	this.blueMap = blueMap;

	let parent = BlueMap.Module.getTopRightElement(blueMap);

	$("#bluemap-info").remove();
	this.elementInfo = $('<div id="bluemap-info" class="button"></div>').appendTo(parent);

	this.elementInfo.click(function(){
		scope.blueMap.alert(
			'<h1>Info</h1>' +
			'Visit BlueMap on <a href="https://github.com/BlueMap-Minecraft">GitHub</a>!<br>' +
			'BlueMap works best with <a href="https://www.google.com/chrome/">Chrome</a>.<br>' +
			'<h2>Controls</h2>' +
			'Leftclick-drag with your mouse or use the arrow-keys to navigate.<br>' +
			'Rightclick-drag with your mouse to rotate your view.<br>' +
			'Scroll to zoom.<br>'
		);
	});
};

// ###### Utils ######
BlueMap.utils = {};

BlueMap.utils.stringToImage = function (string) {
	let image = document.createElementNS("http://www.w3.org/1999/xhtml", "img");
	image.src = string;
	return image;
};

BlueMap.utils.pathFromCoords = function (x, z) {
	let path = "x";
	path += BlueMap.utils.splitNumberToPath(x);

	path += "z";
	path += BlueMap.utils.splitNumberToPath(z);

	path = path.substring(0, path.length - 1);

	return path;
};

BlueMap.utils.splitNumberToPath = function (num) {
	let path = "";

	if (num < 0) {
		num = -num;
		path += "-";
	}

	let s = num.toString();

	for (let i = 0; i < s.length; i++) {
		path += s.charAt(i) + "/";
	}

	return path;
};

BlueMap.utils.hashTile = function (x, z) {
	return "x" + x + "z" + z;
};

BlueMap.utils.Vector2 = {};
BlueMap.utils.Vector2.ZERO = new THREE.Vector2(0, 0);
BlueMap.utils.Vector3 = {};
BlueMap.utils.Vector3.ZERO = new THREE.Vector3(0, 0);

export default BlueMap;
