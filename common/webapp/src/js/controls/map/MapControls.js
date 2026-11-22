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

import {MouseMoveControls} from "./mouse/MouseMoveControls";
import {MouseZoomControls} from "./mouse/MouseZoomControls";
import {MouseRotateControls} from "./mouse/MouseRotateControls";
import {MouseAngleControls} from "./mouse/MouseAngleControls";
import {MathUtils, Vector2, Vector3} from "three";
import {DIRECTION_ALL, DIRECTION_VERTICAL, Manager, Pan, Pinch, Rotate, Tap} from "hammerjs";
import {softClamp} from "../../util/Utils";
import {MapHeightControls} from "./MapHeightControls";
import {KeyMoveControls} from "./keyboard/KeyMoveControls";
import {KeyAngleControls} from "./keyboard/KeyAngleControls";
import {KeyRotateControls} from "./keyboard/KeyRotateControls";
import {KeyZoomControls} from "./keyboard/KeyZoomControls";
import {TouchMoveControls} from "./touch/TouchMoveControls";
import {TouchRotateControls} from "./touch/TouchRotateControls";
import {TouchAngleControls} from "./touch/TouchAngleControls";
import {TouchZoomControls} from "./touch/TouchZoomControls";
import {reactive} from "vue";

const HALF_PI = Math.PI * 0.5;
const HALF_PI_DIV = 1 / HALF_PI;

export class MapControls {

    static _beforeMoveTemp = new Vector3();
    static _rayNearTemp = new Vector3();
    static _rayFarTemp = new Vector3();
    static _rayDirectionTemp = new Vector3();
    static _zoomAnchorOffsetTemp = new Vector2();

    /**
     * @param rootElement {Element}
     * @param scrollCaptureElement {Element}
     */
    constructor(rootElement, scrollCaptureElement) {
        this.rootElement = rootElement;
        this.scrollCaptureElement = scrollCaptureElement;

        this.data = reactive({
            followingPlayer: null
        });

        /** @type {ControlsManager} */
        this.manager = null;

        this.hammer = new Manager(this.rootElement);
        this.initializeHammer();

        //controls
        this.mouseMove = new MouseMoveControls(this.rootElement, 1.5,0.3);
        this.mouseRotate = new MouseRotateControls(this.rootElement, 6, 0.3);
        this.mouseAngle = new MouseAngleControls(this.rootElement, 3, 0.3);
        this.mouseZoom = new MouseZoomControls(this.scrollCaptureElement, 1, 0.2);

        this.keyMove = new KeyMoveControls(this.rootElement, 0.025, 0.2);
        this.keyRotate = new KeyRotateControls(this.rootElement, 0.06, 0.15);
        this.keyAngle = new KeyAngleControls(this.rootElement, 0.04, 0.15);
        this.keyZoom = new KeyZoomControls(this.rootElement, 0.2, 0.15);

        this.touchMove = new TouchMoveControls(this.rootElement, this.hammer, 1.5,0.3);
        this.touchRotate = new TouchRotateControls(this.hammer, 0.0174533, 0.3);
        this.touchAngle = new TouchAngleControls(this.rootElement, this.hammer, 3, 0.3);
        this.touchZoom = new TouchZoomControls(this.hammer);

        this.mapHeight = new MapHeightControls(0.2, 0.1);

        this.lastTap = -1;
        this.lastTapCenter = null;

        this.minDistance = 5;
        this.maxDistance = 100000;

        this.zoomToCursor = true;

        this.zoomAnchorPicked = false;
        this.zoomAnchorPickPosition = new Vector2();
        this.zoomAnchorHeight = null;
    }

    /**
     * @param manager {ControlsManager}
     */
    start(manager) {
        this.manager = manager;

        this.rootElement.addEventListener("contextmenu", this.onContextMenu);
        this.hammer.on("tap", this.onTap);

        this.mouseMove.start(manager);
        this.mouseRotate.start(manager);
        this.mouseAngle.start(manager);
        this.mouseZoom.start(manager);

        this.keyMove.start(manager);
        this.keyRotate.start(manager);
        this.keyAngle.start(manager);
        this.keyZoom.start(manager);

        this.touchMove.start(manager);
        this.touchRotate.start(manager);
        this.touchAngle.start(manager);
        this.touchZoom.start(manager);

        this.mapHeight.start(manager);
    }

    stop() {
        this.stopFollowingPlayerMarker();

        this.rootElement.removeEventListener("contextmenu", this.onContextMenu);
        this.hammer.off("tap", this.onTap);

        this.mouseMove.stop();
        this.mouseRotate.stop();
        this.mouseAngle.stop();
        this.mouseZoom.stop();

        this.keyMove.stop();
        this.keyRotate.stop();
        this.keyAngle.stop();
        this.keyZoom.stop();

        this.touchMove.stop();
        this.touchRotate.stop();
        this.touchAngle.stop();
        this.touchZoom.stop();

        this.mapHeight.stop();
    }

    /**
     * @param delta {number}
     * @param map {Map}
     */
    update(delta, map) {
        this.manager.position.y = -10000; // reset target y position

        // move
        MapControls._beforeMoveTemp.copy(this.manager.position);
        this.mouseMove.update(delta, map);
        this.keyMove.update(delta, map);
        this.touchMove.update(delta, map);

        // if moved, stop following the marker and give back control
        if (this.data.followingPlayer && !MapControls._beforeMoveTemp.equals(this.manager.position)) {
            this.stopFollowingPlayerMarker();
        }

        // follow player marker
        if (this.data.followingPlayer) {
            this.manager.position.copy(this.data.followingPlayer.position);
        }

        // zoom
        const pinching = this.touchZoom.moving || this.touchZoom.deltaZoom !== 1;
        const zoomAnchorOffset = this.getZoomAnchorOffset(pinching, map);
        const distanceBeforeZoom = this.manager.distance;

        this.mouseZoom.update(delta, map);
        this.keyZoom.update(delta, map);
        this.touchZoom.update(delta, map);

        this.manager.distance = softClamp(this.manager.distance, this.minDistance, this.maxDistance, 0.8);

        // keep the point under the cursor in place by moving the target relative to the zoom
        if (zoomAnchorOffset) {
            const zoomFactor = this.manager.distance / distanceBeforeZoom;
            this.manager.position.x += (1 - zoomFactor) * zoomAnchorOffset.x;
            this.manager.position.z += (1 - zoomFactor) * zoomAnchorOffset.y;
        }

        // rotation
        this.mouseRotate.update(delta, map);
        this.keyRotate.update(delta, map);
        this.touchRotate.update(delta, map);

        const rotating = this.mouseRotate.moving || this.touchRotate.moving ||
            this.keyRotate.left || this.keyRotate.right

        // snap rotation to north on orthographic view
        if (this.manager.ortho !== 0 && Math.abs(this.manager.rotation) < (rotating ? 0.05 : 0.3)) {
            this.manager.rotation = softClamp(this.manager.rotation, 0, 0, 0.1);
        }

        // tilt
        if (this.manager.ortho === 0) {
            this.mouseAngle.update(delta, map);
            this.keyAngle.update(delta, map);
            this.touchAngle.update(delta, map);
            this.manager.angle = softClamp(this.manager.angle, 0, HALF_PI, 0.8);
        }

        // target height
        if (this.manager.ortho === 0 || this.manager.angle === 0) {
            this.mapHeight.update(delta, map);
        }
    }

    reset() {
        this.zoomAnchorPicked = false;

        this.mouseMove.reset();
        this.mouseRotate.reset();
        this.mouseAngle.reset();
        this.mouseZoom.reset();

        this.touchMove.reset();
        this.touchRotate.reset();
        this.touchAngle.reset();
        this.touchZoom.reset();
    }

    /**
     * Calculates the horizontal offset from the camera-target to the point under the zoom-anchor (mouse cursor or pinch center),
     * on the horizontal plane at the height of the camera-target, or close up in perspective view at the height of the terrain under the anchor.
     * Drops the mouse anchor while the camera is rotated with the mouse.
     * @private
     * @param pinching {boolean}
     * @param map {Map}
     * @returns {Vector2 | null} - the offset (x, z), or null if the zoom should be centered.
     * The returned vector is reused and only valid until the next call.
     */
    getZoomAnchorOffset(pinching, map) {
        // only anchor the zoom if the control that set the anchor is actually zooming
        let anchor = null;
        if (pinching) {
            anchor = this.touchZoom.anchor;
        } else if (this.mouseZoom.deltaZoom !== 0) {
            // don't zoom towards the cursor while the camera is (still visibly) rotated or tilted with the mouse
            const mouseTurning = this.mouseRotate.moving ||
                Math.abs(this.mouseRotate.deltaRotation) > 1 ||
                (this.manager.ortho === 0 && Math.abs(this.mouseAngle.deltaAngle) > 1);

            if (mouseTurning) this.mouseZoom.anchor = null;
            else anchor = this.mouseZoom.anchor;
        }
        if (!anchor || !this.zoomToCursor || this.data.followingPlayer) {
            this.zoomAnchorPicked = false;
            return null;
        }

        const rect = this.rootElement.getBoundingClientRect();
        if (rect.width === 0 || rect.height === 0) return null;

        const screenX = ((anchor.x - rect.left) / rect.width) * 2 - 1;
        const screenY = -((anchor.y - rect.top) / rect.height) * 2 + 1;
        if (Math.abs(screenX) > 1 || Math.abs(screenY) > 1) return null;

        // updateCamera() moves the camera without updating its matrices, and render() might have been skipped
        const camera = this.manager.camera;
        camera.updateProjectionMatrix();
        camera.updateMatrixWorld();

        const near = MapControls._rayNearTemp.set(screenX, screenY, -1).unproject(camera);
        const far = MapControls._rayFarTemp.set(screenX, screenY, 1).unproject(camera);

        // the camera has been built from lastPosition, position.y has already been reset at this point
        const target = this.manager.lastPosition;
        const rayY = far.y - near.y;
        if (Math.abs(rayY) < 0.000001) return null;

        // close up in perspective view the target height follows the terrain, use the height of the terrain under
        // the anchor there, so the zoom heads for e.g. a mountain and not for the point behind it at the target height.
        // the lookup is expensive, so only redo it when the anchor moves
        let height = target.y;
        if (this.manager.ortho === 0 && this.manager.distance < 500) {
            if (!this.zoomAnchorPicked || this.zoomAnchorPickPosition.distanceTo(anchor) > 10) {
                const direction = MapControls._rayDirectionTemp.subVectors(far, near).normalize();
                const hit = map.raycastTerrain(near, direction, 10 * Math.max(this.manager.distance, 100));

                this.zoomAnchorPicked = true;
                this.zoomAnchorPickPosition.copy(anchor);
                this.zoomAnchorHeight = hit ? hit.y : null;
            }
            if (this.zoomAnchorHeight !== null) height = this.zoomAnchorHeight;
        } else {
            this.zoomAnchorPicked = false;
        }

        const t = (height - near.y) / rayY;
        if (!Number.isFinite(t) || t < 0) return null;

        const offset = MapControls._zoomAnchorOffsetTemp.set(
            near.x + (far.x - near.x) * t - target.x,
            near.z + (far.z - near.z) * t - target.z
        );

        // looking close to the horizon puts the point very far away, so limit the offset
        const maxOffset = 8 * Math.max(this.manager.distance, 1);
        if (offset.length() > maxOffset) offset.setLength(maxOffset);

        return offset;
    }

    static getMaxPerspectiveAngleForDistance(distance) {
        return MathUtils.clamp((1 - Math.pow(Math.max(distance - 5, 0.001) * 0.0005, 0.5)) * HALF_PI,0, HALF_PI)
    }

    // getMaxPerspectiveAngleForDistance but in reverse (its simple maths, they said)
    static getMaxDistanceForPerspectiveAngle(angle) {
        return Math.pow(-(angle * HALF_PI_DIV) + 1, 2) * 2000 + 5;
    }

    initializeHammer() {
        let touchTap = new Tap({ event: 'tap', pointers: 1, taps: 1, threshold: 5 });
        let touchMove = new Pan({ event: 'move', pointers: 1, direction: DIRECTION_ALL, threshold: 0 });
        let touchTilt =  new Pan({ event: 'tilt', pointers: 2, direction: DIRECTION_VERTICAL, threshold: 0 });
        let touchRotate = new Rotate({ event: 'rotate', pointers: 2, threshold: 0 });
        let touchZoom = new Pinch({ event: 'zoom', pointers: 2, threshold: 0 });

        touchMove.recognizeWith(touchRotate);
        touchMove.recognizeWith(touchTilt);
        touchMove.recognizeWith(touchZoom);
        touchTilt.recognizeWith(touchRotate);
        touchTilt.recognizeWith(touchZoom);
        touchRotate.recognizeWith(touchZoom);

        this.hammer.add(touchTap);
        this.hammer.add(touchTilt);
        this.hammer.add(touchMove);
        this.hammer.add(touchRotate);
        this.hammer.add(touchZoom);
    }

    /**
     * @param marker {object}
     */
    followPlayerMarker(marker) {
        if (marker.isPlayerMarker) marker = marker.data;
        this.data.followingPlayer = marker;
    }

    stopFollowingPlayerMarker() {
        this.data.followingPlayer = null;
    }

    onContextMenu = evt => {
        evt.preventDefault();
    }

    onTap = evt => {
        let doubleTap = false;
        let center = new Vector2(evt.center.x, evt.center.y);

        let now = Date.now();
        if (this.lastTap > 0 && this.lastTapCenter && now - this.lastTap < 500 && this.lastTapCenter.distanceTo(center) < 5){
            doubleTap = true;
            this.lastTap = -1;
        } else {
            this.lastTap = now;
            this.lastTapCenter = center;
        }

        this.manager.handleMapInteraction(new Vector2(evt.center.x, evt.center.y), {doubleTap: doubleTap});
    }

}