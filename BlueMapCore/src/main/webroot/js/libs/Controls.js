/*
 * This file is part of BlueMap, licensed under the MIT License (MIT).
 *
 * Copyright (c) Blue (Lukas Rieger) <https://bluecolored.de>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the 'Software'), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Softwarevent.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARevent.
 */
import $ from 'jquery';
import {
	Euler,
	Raycaster,
	Vector2,
	Vector3,
	MOUSE
} from 'three';

import { Vector2_ZERO } from './utils.js';

export default class Controls {
	static KEYS = {
		LEFT: 37,
		UP: 38,
		RIGHT: 39,
		DOWN: 40,
		ORBIT: MOUSE.RIGHT,
		MOVE: MOUSE.LEFT
	};
	static STATES = {
		NONE: -1,
		ORBIT: 0,
		MOVE: 1,
	};

	/**
	 * targetHeightScene and cameraHeightScene are scenes of objects that are checked via raycasting for a height for the target and the camera
	 */
	constructor(camera, element, heightScene) {
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

		this.raycaster = new Raycaster();
		this.rayDirection = new Vector3(0, -1, 0);

		this.resetPosition();

		this.mouse = new Vector2(0, 0);
		this.lastMouse = new Vector2(0, 0);
		this.deltaMouse = new Vector2(0, 0);

		//variables used to calculate with (to prevent object creation every update)
		this.orbitRot = new Euler(0, 0, 0, 'YXZ');
		this.cameraPosDelta = new Vector3(0, 0, 0);
		this.moveDelta = new Vector2(0, 0);

		this.keyStates = {}
		this.state = Controls.STATES.NONE;

		let canvas = $(this.element).find('canvas').get(0);
		window.addEventListener('contextmenu', event => {
			event.preventDefault();
		}, false);
		window.addEventListener('mousemove', this.onMouseMove, false);
		canvas.addEventListener('mousedown', this.onMouseDown, false);
		canvas.addEventListener('mouseup', this.onMouseUp, false);
		canvas.addEventListener('wheel', this.onMouseWheel, { passive: true });
		window.addEventListener('keydown', this.onKeyDown, false);
		window.addEventListener('keyup', this.onKeyUp, false);

		this.camera.position.set(0, 1000, 0);
		this.camera.lookAt(this.position);
		this.camera.updateProjectionMatrix();
	}

	resetPosition() {
		this.position = new Vector3(0, 70, 0);
		this.targetPosition = new Vector3(0, 70, 0);

		this.distance = 5000;
		this.targetDistance = 1000;

		this.direction = 0;
		this.targetDirection = 0;

		this.angle = 0;
		this.targetAngle = 0;
	}

	update() {
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
	}

	updateHeights() {
			//TODO: this can be performance-improved by only intersecting the correct tile?

			let rayStart = new Vector3(this.targetPosition.x, 300, this.targetPosition.z);
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

	updateMouseMoves = () => {
		this.deltaMouse.set(this.lastMouse.x - this.mouse.x, this.lastMouse.y - this.mouse.y);

		this.moveDelta.x = 0;
		this.moveDelta.y = 0;

		if (this.keyStates[Controls.KEYS.UP]){
			this.moveDelta.y -= 20;
		}
		if (this.keyStates[Controls.KEYS.DOWN]){
			this.moveDelta.y += 20;
		}
		if (this.keyStates[Controls.KEYS.LEFT]){
			this.moveDelta.x -= 20;
		}
		if (this.keyStates[Controls.KEYS.RIGHT]){
			this.moveDelta.x += 20;
		}

		if (this.state === Controls.STATES.MOVE) {
			if (this.deltaMouse.x === 0 && this.deltaMouse.y === 0) return;
			this.moveDelta.copy(this.deltaMouse);
		}

		if (this.moveDelta.x !== 0 || this.moveDelta.y !== 0) {
			this.moveDelta.rotateAround(Vector2_ZERO, -this.direction);
			this.targetPosition.set(
				this.targetPosition.x + (this.moveDelta.x * this.distance / this.element.clientHeight * this.settings.move.speed),
				this.targetPosition.y,
				this.targetPosition.z + (this.moveDelta.y * this.distance / this.element.clientHeight * this.settings.move.speed)
			);
		}

		if (this.state === Controls.STATES.ORBIT) {
			this.targetDirection += (this.deltaMouse.x / this.element.clientHeight * Math.PI);
			this.targetAngle += (this.deltaMouse.y / this.element.clientHeight * Math.PI);
		}

		this.lastMouse.copy(this.mouse);
	};

	onMouseWheel = event => {
		if (event.deltaY > 0) {
			this.targetDistance *= this.settings.zoom.speed;
		} else if (event.deltaY < 0) {
			this.targetDistance /= this.settings.zoom.speed;
		}

		if (this.targetDistance < this.settings.zoom.min) this.targetDistance = this.settings.zoom.min;
		if (this.targetDistance > this.settings.zoom.max) this.targetDistance = this.settings.zoom.max;
	}

	onMouseMove = event => {
		this.mouse.set(event.clientX, event.clientY);

		if (this.state !== Controls.STATES.NONE){
			event.preventDefault();
		}
	}

	onMouseDown = event => {
		if (this.state !== Controls.STATES.NONE) return;

		switch (event.button) {
			case Controls.KEYS.MOVE :
				this.state = Controls.STATES.MOVE;
				event.preventDefault();
				break;
			case Controls.KEYS.ORBIT :
				this.state = Controls.STATES.ORBIT;
				event.preventDefault();
				break;
		}
	}

	onMouseUp = event => {
		if (this.state === Controls.STATES.NONE) return;

		switch (event.button) {
			case Controls.KEYS.MOVE :
				if (this.state === Controls.STATES.MOVE) this.state = Controls.STATES.NONE;
				break;
			case Controls.KEYS.ORBIT :
				if (this.state === Controls.STATES.ORBIT) this.state = Controls.STATES.NONE;
				break;
		}
	}

	onKeyDown = event => {
		this.keyStates[event.keyCode] = true;
	}

	onKeyUp = event => {
		this.keyStates[event.keyCode] = false;
	}
}
