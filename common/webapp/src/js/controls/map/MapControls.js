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
import {Manager, Pan, Pinch, Rotate, Tap, DIRECTION_ALL, DIRECTION_VERTICAL} from "hammerjs";
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
import {PlayerMarker} from "../../markers/PlayerMarker";
import {reactive} from "vue";

const HALF_PI = Math.PI * 0.5;

export class MapControls {

    static _beforeMoveTemp = new Vector3();

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
        this.mouseZoom.update(delta, map);
        this.keyZoom.update(delta, map);
        this.touchZoom.update(delta, map);

        this.manager.distance = softClamp(this.manager.distance, this.minDistance, this.maxDistance, 0.8);

        // max angle for current distance
        let maxAngleForZoom = this.getMaxPerspectiveAngleForDistance(this.manager.distance);

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
            this.manager.angle = softClamp(this.manager.angle, 0, maxAngleForZoom, 0.8);
        }

        // target height
        if (this.manager.ortho === 0 || this.manager.angle === 0) {
            this.mapHeight.maxAngle = maxAngleForZoom;
            this.mapHeight.update(delta, map);
        }
    }

    reset() {
        this.mouseMove.reset();
        this.mouseRotate.reset();
        this.mouseAngle.reset();
        this.mouseZoom.reset();

        this.touchMove.reset();
        this.touchRotate.reset();
        this.touchAngle.reset();
        this.touchZoom.reset();
    }

    getMaxPerspectiveAngleForDistance(distance) {
        return MathUtils.clamp((1 - Math.pow(Math.max(distance - 5, 0.001) * 0.0005, 0.5)) * HALF_PI,0, HALF_PI)
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