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

import {MathUtils, Vector2, Vector3} from "three";
import {MapControls} from "../MapControls";

export class MouseZoomControls {

    /**
     * @param target {EventTarget}
     * @param speed {number}
     * @param stiffness {number}
     */
    constructor(target, speed, stiffness) {
        this.target = target;
        this.manager = null;

        this.stiffness = stiffness;
        this.speed = speed;

        this.deltaZoom = 0;
        this.lastMousePosition = null;
    }

    /**
     * @param manager {ControlsManager}
     */
    start(manager) {
        this.manager = manager;

        this.target.addEventListener("wheel", this.onMouseWheel, {passive: false});
    }

    stop() {
        this.target.removeEventListener("wheel", this.onMouseWheel);
    }

    /**
     * @param delta {number}
     * @param map {Map}
     */
    update(delta, map) {
        if (this.deltaZoom === 0) {
            this.lastMousePosition = null;
            return;
        }

        let smoothing = this.stiffness / (16.666 / delta);
        smoothing = MathUtils.clamp(smoothing, 0, 1);

        // Calculate world position under mouse before zooming (if we have a mouse position)
        let targetWorldPoint = null;
        if (this.lastMousePosition && this.manager.mapViewer && map && map.isLoaded) {
            const rootElement = this.manager.mapViewer.rootElement;
            const normalizedScreenPos = new Vector2(
                ((this.lastMousePosition.x - rootElement.getBoundingClientRect().left) / rootElement.clientWidth) * 2 - 1,
                -((this.lastMousePosition.y - rootElement.getBoundingClientRect().top) / rootElement.clientHeight) * 2 + 1
            );

            // Update camera matrix before raycasting
            this.manager.mapViewer.camera.updateMatrixWorld();
            this.manager.mapViewer.raycaster.setFromCamera(normalizedScreenPos, this.manager.mapViewer.camera);

            // Raycast to find the world point under the mouse
            const intersectScenes = [map.hiresTileManager.scene];
            for (let i = 0; i < map.lowresTileManager.length; i++) {
                intersectScenes.push(map.lowresTileManager[i].scene);
            }

            const intersects = this.manager.mapViewer.raycaster.intersectObjects(intersectScenes, true);
            if (intersects.length > 0) {
                targetWorldPoint = intersects[0].point.clone();
            } else {
                // If no intersection, calculate point on a plane at the current camera target height
                // Use the camera's look direction to find a point at the current distance
                const cameraDirection = new Vector3();
                this.manager.mapViewer.camera.getWorldDirection(cameraDirection);
                const planeNormal = new Vector3(0, 1, 0);
                const planePoint = this.manager.position.clone();
                
                // Calculate intersection of ray with horizontal plane
                const denom = cameraDirection.dot(planeNormal);
                if (Math.abs(denom) > 0.0001) {
                    const toPlane = planePoint.clone().sub(this.manager.mapViewer.camera.position);
                    const t = toPlane.dot(planeNormal) / denom;
                    targetWorldPoint = this.manager.mapViewer.camera.position.clone().add(cameraDirection.clone().multiplyScalar(t));
                }
            }
        }

        const oldDistance = this.manager.distance;
        this.manager.distance *= Math.pow(1.5, this.deltaZoom * smoothing * this.speed);
        this.manager.angle = Math.min(this.manager.angle, MapControls.getMaxPerspectiveAngleForDistance(this.manager.distance));

        // Adjust camera position to keep the same world point under the mouse
        if (targetWorldPoint && this.lastMousePosition) {
            // Recalculate the normalized screen position
            const rootElement = this.manager.mapViewer.rootElement;
            const normalizedScreenPos = new Vector2(
                ((this.lastMousePosition.x - rootElement.getBoundingClientRect().left) / rootElement.clientWidth) * 2 - 1,
                -((this.lastMousePosition.y - rootElement.getBoundingClientRect().top) / rootElement.clientHeight) * 2 + 1
            );

            // Update camera with new distance
            this.manager.updateCamera();
            this.manager.mapViewer.camera.updateMatrixWorld();

            // Raycast again to find where the mouse now points
            this.manager.mapViewer.raycaster.setFromCamera(normalizedScreenPos, this.manager.mapViewer.camera);
            const intersectScenes = [map.hiresTileManager.scene];
            for (let i = 0; i < map.lowresTileManager.length; i++) {
                intersectScenes.push(map.lowresTileManager[i].scene);
            }
            const newIntersects = this.manager.mapViewer.raycaster.intersectObjects(intersectScenes, true);
            
            let newWorldPoint = null;
            if (newIntersects.length > 0) {
                newWorldPoint = newIntersects[0].point.clone();
            } else {
                // Fallback: use plane intersection
                const cameraDirection = new Vector3();
                this.manager.mapViewer.camera.getWorldDirection(cameraDirection);
                const planeNormal = new Vector3(0, 1, 0);
                const planePoint = this.manager.position.clone();
                const denom = cameraDirection.dot(planeNormal);
                if (Math.abs(denom) > 0.0001) {
                    const toPlane = planePoint.clone().sub(this.manager.mapViewer.camera.position);
                    const t = toPlane.dot(planeNormal) / denom;
                    newWorldPoint = this.manager.mapViewer.camera.position.clone().add(cameraDirection.clone().multiplyScalar(t));
                }
            }

            // Adjust position to compensate for the shift
            if (newWorldPoint && targetWorldPoint) {
                const offset = targetWorldPoint.clone().sub(newWorldPoint);
                this.manager.position.add(offset);
            }
        }

        this.deltaZoom *= 1 - smoothing;
        if (Math.abs(this.deltaZoom) < 0.0001) {
            this.deltaZoom = 0;
            this.lastMousePosition = null;
        }
    }

    reset() {
        this.deltaZoom = 0;
        this.lastMousePosition = null;
    }

    /**
     * @private
     * @param evt {WheelEvent}
     */
    onMouseWheel = evt => {
        evt.preventDefault();

        // Store mouse position for zoom centering
        this.lastMousePosition = {
            x: evt.clientX,
            y: evt.clientY
        };

        let delta = evt.deltaY;
        if (evt.deltaMode === WheelEvent.DOM_DELTA_PIXEL) delta *= 0.01;
        if (evt.deltaMode === WheelEvent.DOM_DELTA_LINE) delta *= 0.33;

        this.deltaZoom += delta;
    }

}