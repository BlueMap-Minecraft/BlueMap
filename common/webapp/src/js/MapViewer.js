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
import {Color, PerspectiveCamera, Raycaster, Scene, Vector2, Vector3, WebGLRenderer} from "three";
import {Map} from "./map/Map";
import {SkyboxScene} from "./skybox/SkyboxScene";
import {ControlsManager} from "./controls/ControlsManager";
import Stats from "./util/Stats";
import {alert, dispatchEvent, elementOffset, generateCacheHash, htmlToElement, softClamp} from "./util/Utils";
import {TileManager} from "./map/TileManager";
import {HIRES_VERTEX_SHADER} from "./map/hires/HiresVertexShader";
import {HIRES_FRAGMENT_SHADER} from "./map/hires/HiresFragmentShader";
import {LOWRES_VERTEX_SHADER} from "./map/lowres/LowresVertexShader";
import {LOWRES_FRAGMENT_SHADER} from "./map/lowres/LowresFragmentShader";
import {CombinedCamera} from "./util/CombinedCamera";
import {CSS2DRenderer} from "./util/CSS2DRenderer";
import {MarkerSet} from "./markers/MarkerSet";
import {reactive} from "vue";

export class MapViewer {

	/**
	 * @param element {Element}
	 * @param events {EventTarget}
	 */
	constructor(element, events = element) {
		Object.defineProperty( this, 'isMapViewer', { value: true } );

		this.rootElement = element;
		this.events = events;

		this.data = reactive({
			map: null,
			mapState: "unloaded",
			camera: null,
			controlsManager: null,
			uniforms: {
				distance: { value: 0 },
				sunlightStrength: { value: 1 },
				ambientLight: { value: 0 },
				skyColor: { value: new Color(0.5, 0.5, 1) },
				voidColor: { value: new Color(0, 0, 0) },
				chunkBorders: { value: false },
				hiresTileMap: {
					value: {
						map: null,
						size: TileManager.tileMapSize,
						scale: new Vector2(1, 1),
						translate: new Vector2(),
						pos: new Vector2(),
					}
				}
			},
			superSampling: 1,
			loadedCenter: new Vector2(0, 0),
			loadedHiresViewDistance: 200,
			loadedLowresViewDistance: 2000,
		});

		this.tileCacheHash = generateCacheHash();

		this.stats = new Stats();
		this.stats.hide();

		// renderer
		this.renderer = new WebGLRenderer({
			antialias: true,
			sortObjects: true,
			preserveDrawingBuffer: true,
			logarithmicDepthBuffer: true,
		});
		this.renderer.autoClear = false;
		this.renderer.uniforms = this.data.uniforms;

		// CSS2D renderer
		this.css2dRenderer = new CSS2DRenderer(this.events);

		this.skyboxScene = new SkyboxScene(this.data.uniforms);

		this.camera = new CombinedCamera(75, 1, 0.1, 10000, 0);
		this.skyboxCamera = new PerspectiveCamera(75, 1, 0.1, 10000);
		this.skyboxCamera.updateProjectionMatrix();

		this.controlsManager = new ControlsManager(this, this.camera);

		this.raycaster = new Raycaster();
		this.raycaster.layers.enableAll();
		this.raycaster.params.Line2 = {threshold: 20}

		/** @type {Map} */
		this.map = null;

		this.markers = new MarkerSet("bm-root");

		this.lastFrame = 0;
		this.lastRedrawChange = 0;
		events.addEventListener("bluemapCameraMoved", this.redraw)
		events.addEventListener("bluemapTileLoaded", this.redraw)

		// initialize
		this.initializeRootElement();

		// handle window resizes
		window.addEventListener("resize", this.handleContainerResize);

		// start render-loop
		requestAnimationFrame(this.renderLoop);
	}

	/**
	 * Initializes the root-element
	 */
	initializeRootElement() {
		this.rootElement.innerHTML = "";

		let outerDiv = htmlToElement(`<div style="position: relative; width: 100%; height: 100%; overflow: hidden;"></div>`);
		this.rootElement.appendChild(outerDiv);

		// 3d-canvas
		outerDiv.appendChild(this.renderer.domElement);

		// html-markers
		this.css2dRenderer.domElement.style.position = 'absolute';
		this.css2dRenderer.domElement.style.top = '0';
		this.css2dRenderer.domElement.style.left = '0';
		this.css2dRenderer.domElement.style.pointerEvents = 'none';
		outerDiv.appendChild(this.css2dRenderer.domElement);

		// performance monitor
		outerDiv.appendChild(this.stats.dom);

		this.handleContainerResize();
	}

	/**
	 * Updates the render-resolution and aspect ratio based on the size of the root-element
	 */
	handleContainerResize = () => {
		this.renderer.setSize(this.rootElement.clientWidth, this.rootElement.clientHeight);
		this.renderer.setPixelRatio(window.devicePixelRatio * this.superSampling);

		this.css2dRenderer.setSize(this.rootElement.clientWidth, this.rootElement.clientHeight);

		this.camera.aspect = this.rootElement.clientWidth / this.rootElement.clientHeight;
		this.camera.updateProjectionMatrix();

		this.redraw();
	};

	/**
	 * Triggers an interaction on the screen (map), e.g. a mouse-click.
	 *
	 * This will first attempt to invoke the onClick() method on the Object3D (e.g. Markers) that has been clicked.
	 * And if none of those consumed the event, it will fire a <code>bluemapMapInteraction</code> event.
	 *
	 * @param screenPosition {Vector2} - Clicked position on the screen (usually event.x, event.y)
	 * @param data {object} - Custom event data that will be added to the interaction-event
	 */
	handleMapInteraction(screenPosition, data = {}) {
		let rootOffset = elementOffset(this.rootElement);
		let normalizedScreenPos = new Vector2(
			((screenPosition.x - rootOffset.left) / this.rootElement.clientWidth) * 2 - 1,
			-((screenPosition.y - rootOffset.top) / this.rootElement.clientHeight) * 2 + 1
		);

		if (this.map && this.map.isLoaded){
			this.camera.updateMatrixWorld()
			this.raycaster.setFromCamera(normalizedScreenPos, this.camera);

			// check Object3D interactions

			// make sure the scene is at 0,0 (render() might have shifted it)
			let sPos = this.map.hiresTileManager.scene.position;
			sPos.x = 0; sPos.z = 0;
			this.map.hiresTileManager.scene.updateMatrixWorld();

			const intersectScenes = [this.map.hiresTileManager.scene, this.markers];
			for (let i = 0; i < this.map.lowresTileManager.length; i++) {

				// make sure the scene is at 0,0 (render() might have shifted it)
				let sPos = this.map.lowresTileManager[i].scene.position;
				sPos.x = 0; sPos.z = 0;
				this.map.lowresTileManager[i].scene.updateMatrixWorld();

				intersectScenes.push(this.map.lowresTileManager[i].scene);
			}

			let intersects = this.raycaster.intersectObjects(intersectScenes, true);
			let hit = null;
			let lowresHits = [];
			let hiresHit = null;
			let covered = false;

			for (let i = 0; i < intersects.length; i++) {
				if (intersects[i].object){
					let object = intersects[i].object;

					// check if deeply-visible
					let parent = object;
					let visible = parent.visible;
					while (visible && parent.parent){
						parent = parent.parent;
						visible = parent.visible;
					}

					if (visible) {
						if (!hit) hit = intersects[i];

						// find root-scene
						let parentRoot = object;
						while(parentRoot.parent) parentRoot = parentRoot.parent;

						for (let l = 0; l < this.map.lowresTileManager.length; l++) {
							if (parentRoot === this.map.lowresTileManager[l].sceneParent) {
								if (!lowresHits[l]) lowresHits[l] = intersects[i];
							}
						}

						if (parentRoot === this.map.hiresTileManager.sceneParent) {
							if (!hiresHit) hiresHit = intersects[i];
						}

						if (!covered || (object.material && !object.material.depthTest)) {
							if (object.onClick && object.onClick({
								data: data,
								intersection: intersects[i]
							})) return;
						}

						if (parentRoot !== this.map.lowresTileManager[0].sceneParent) {
							covered = true;
						}
					}
				}
			}

			// fire event
			dispatchEvent(this.events, "bluemapMapInteraction", {
				data: data,
				hit: hit,
				hiresHit: hiresHit,
				lowresHits: lowresHits,
				intersections: intersects,
				ray: this.raycaster.ray
			});
		}
	}

	/**
	 * Call to wake up the render-loop and render on high-fps for a while
	 */
	redraw = () => {
		this.lastRedrawChange = Date.now();
	}

	/**
	 * @private
	 * The render-loop to update and possibly render a new frame.
	 * @param now {number} the current time in milliseconds
	 */
	renderLoop = (now) => {
		requestAnimationFrame(this.renderLoop);

		// calculate delta time
		if (this.lastFrame <= 0) this.lastFrame = now;
		let delta = now - this.lastFrame;

		// update stats
		this.stats.begin();

		// update controls
		if (this.map != null) {
			this.controlsManager.update(delta, this.map);
		}

		// render
		if (delta >= 50 || Date.now() - this.lastRedrawChange < 1000) {
			this.lastFrame = now;
			this.render(delta);
		}

		// update stats
		this.stats.update();
	};

	/**
	 * @private
	 * Renders a frame
	 * @param delta {number}
	 */
	render(delta) {
		dispatchEvent(this.events, "bluemapRenderFrame", {
			delta: delta,
		});

		// render
		this.renderer.clear();

		// prepare skybox camera
		this.skyboxCamera.rotation.copy(this.camera.rotation);

		// render skybox
		this.renderer.render(this.skyboxScene, this.skyboxCamera);
		this.renderer.clearDepth();

		if (this.map && this.map.isLoaded) {

			this.map.animations.forEach(animation => animation.step(delta))

			// shift whole scene including camera towards 0,0 to tackle shader-precision issues
			const s = 10000;
			const sX = Math.round(this.camera.position.x / s) * s;
			const sZ = Math.round(this.camera.position.z / s) * s;
			this.camera.position.x -= sX;
			this.camera.position.z -= sZ;

			// update uniforms
			this.data.uniforms.distance.value = this.controlsManager.distance;
			this.data.uniforms.hiresTileMap.value.pos.copy(this.map.hiresTileManager.centerTile);
			this.data.uniforms.hiresTileMap.value.translate.set(
				this.map.data.hires.translate.x - sX,
				this.map.data.hires.translate.z - sZ
			);

			// prepare camera for lowres
			const cameraFar = this.camera.far;
			if (this.controlsManager.distance < 1000) {
				this.camera.far = 1000000; // disable far clipping for lowres
			}
			this.camera.updateProjectionMatrix();

			// render lowres
			const highestLod = this.map.lowresTileManager.length - 1;
			for (let i = this.map.lowresTileManager.length - 1; i >= 0; i--) {
				if (i === highestLod || this.controlsManager.distance < 1000 * Math.pow(this.map.data.lowres.lodFactor, i + 1)) {
					let scenePos = this.map.lowresTileManager[i].scene.position;
					scenePos.x = -sX;
					scenePos.z = -sZ;

					if (i === 0) {
						this.camera.far = cameraFar; // reset far clipping for the highest lowres lod to make depth-tests possible
						this.camera.updateProjectionMatrix();
					}

					this.renderer.render(this.map.lowresTileManager[i].sceneParent, this.camera);

					if (i !== 0) this.renderer.clearDepth(); // clear depth-buffer for all lowres except the highest
				}
			}

			this.camera.far = cameraFar; // reset far clipping

			// render hires
			if (this.controlsManager.distance < 1000) {
				this.camera.updateProjectionMatrix();
				let scenePos = this.map.hiresTileManager.scene.position;
				scenePos.x = -sX;
				scenePos.z = -sZ;
				this.renderer.render(this.map.hiresTileManager.sceneParent, this.camera);
			}

			// shift back
			this.camera.position.x += sX;
			this.camera.position.z += sZ;

		}

		// render markers
		this.renderer.render(this.markers, this.camera);
		this.css2dRenderer.render(this.markers, this.camera);
	}

	/**
	 * Changes / Sets the map that will be loaded and displayed
	 * @param map {Map}
	 * @returns Promise<void>
	 */
	switchMap(map = null) {
		if (this.map && this.map.isMap) this.map.unload();
		this.data.mapState = "loading";

		this.map = map;

		if (this.map && this.map.isMap) {
			return map.load(HIRES_VERTEX_SHADER, HIRES_FRAGMENT_SHADER, LOWRES_VERTEX_SHADER, LOWRES_FRAGMENT_SHADER, this.data.uniforms, this.tileCacheHash)
				.then(() => {
					for (let texture of this.map.loadedTextures){
						this.renderer.initTexture(texture);
					}

					this.data.uniforms.distance.value = this.controlsManager.distance;
					this.data.uniforms.skyColor.value = map.data.skyColor;
					this.data.uniforms.voidColor.value = map.data.voidColor;
					this.data.uniforms.ambientLight.value = map.data.ambientLight;
					this.data.uniforms.sunlightStrength.value = map.data.skyLight;
					this.data.uniforms.hiresTileMap.value.map = map.hiresTileManager.tileMap.texture;
					this.data.uniforms.hiresTileMap.value.scale.set(map.data.hires.tileSize.x, map.data.hires.tileSize.z);
					this.data.uniforms.hiresTileMap.value.translate.set(map.data.hires.translate.x, map.data.hires.translate.z);

					setTimeout(this.updateLoadedMapArea);

					this.data.mapState = "loaded";

					dispatchEvent(this.events, "bluemapMapChanged", {
						map: map
					});
				})
				.catch(error => {
					this.data.mapState = "errored";
					this.map = null;
					throw error;
				});
		} else {
			return Promise.resolve();
		}
	}

	/**
	 * Loads the given area on the map (and unloads everything outside that area)
	 * @param centerX {number}
	 * @param centerZ {number}
	 * @param hiresViewDistance {number?}
	 * @param lowresViewDistance {number?}
	 */
	loadMapArea(centerX, centerZ, hiresViewDistance = -1, lowresViewDistance = -1) {
		this.data.loadedCenter.set(centerX, centerZ);
		if (hiresViewDistance >= 0) this.data.loadedHiresViewDistance = hiresViewDistance;
		if (lowresViewDistance >= 0) this.data.loadedLowresViewDistance = lowresViewDistance;

		this.updateLoadedMapArea();
	}

	updateLoadedMapArea = () => {
		if (!this.map) return;
		if (this.controlsManager.distance < 1000) {
			this.map.loadMapArea(this.data.loadedCenter.x, this.data.loadedCenter.y, this.data.loadedHiresViewDistance, this.data.loadedLowresViewDistance);
		} else {
			this.map.loadMapArea(this.data.loadedCenter.x, this.data.loadedCenter.y, 0, this.data.loadedLowresViewDistance);
		}
	}

	clearTileCache(newTileCacheHash) {
		if (!newTileCacheHash) newTileCacheHash = generateCacheHash();

		this.tileCacheHash = newTileCacheHash;
		if (this.map) {
			for (let i = 0; i < this.map.lowresTileManager.length; i++) {
				this.map.lowresTileManager[i].tileLoader.tileCacheHash = this.tileCacheHash;
			}
			this.map.hiresTileManager.tileLoader.tileCacheHash = this.tileCacheHash;
		}
	}

	/**
	 * @returns {number}
	 */
	get superSampling() {
		return this.data.superSampling;
	}

	/**
	 * @param value {number}
	 */
	set superSampling(value) {
		this.data.superSampling = value;
		this.handleContainerResize();
	}

	/**
	 * @returns {CombinedCamera}
	 */
	get camera() {
		return this._camera;
	}

	/**
	 * @param value {CombinedCamera}
	 */
	set camera(value) {
		this._camera = value;
		this.data.camera = value.data;
	}

	/**
	 * @returns {ControlsManager}
	 */
	get controlsManager() {
		return this._controlsManager;
	}

	/**
	 * @param value {ControlsManager}
	 */
	set controlsManager(value) {
		this._controlsManager = value;
		this.data.controlsManager = value.data;
	}

	/**
	 * @returns {Map}
	 */
	get map() {
		return this._map;
	}

	/**
	 * @param value {Map}
	 */
	set map(value) {
		this._map = value;
		if (value) this.data.map = value.data;
	}

}
