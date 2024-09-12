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
import {Manager, Pan, DIRECTION_ALL} from "hammerjs";
import {animate, EasingFunctions} from "../../util/Utils";
import {KeyMoveControls} from "./keyboard/KeyMoveControls";
import {MouseRotateControls} from "./mouse/MouseRotateControls";
import {MouseAngleControls} from "./mouse/MouseAngleControls";
import {KeyHeightControls} from "./keyboard/KeyHeightControls";
import {TouchPanControls} from "./touch/TouchPanControls";
import {reactive} from "vue";
import {DEG2RAD} from "three/src/math/MathUtils";

export class FreeFlightControls {

    static _beforeMoveTemp = new Vector3();

    /**
     * @param target {Element}
     */
    constructor(target) {
        this.target = target;
        this.manager = null;

        this.data = reactive({
            followingPlayer: null
        });

        this.hammer = new Manager(this.target);
        this.initializeHammer();

        this.keyMove = new KeyMoveControls(this.target, 0.5, 0.1);
        this.keyHeight = new KeyHeightControls(this.target, 0.5, 0.2);
        this.mouseRotate = new MouseRotateControls(this.target, 1.5, -2, -1.5, 0.5);
        this.mouseAngle = new MouseAngleControls(this.target, 1.5, -2, -1.5, 0.5);
        this.touchPan = new TouchPanControls(this.target, this.hammer, 5, 0.15);

        this.started = false;

        this.clickStart = new Vector2();
        this.moveSpeed = 0.5;

        this.animationTargetHeight = 0;
    }

    /**
     * @param manager {ControlsManager}
     */
    start(manager) {
        this.manager = manager;

        this.keyMove.start(manager);
        this.keyHeight.start(manager);
        this.mouseRotate.start(manager);
        this.mouseAngle.start(manager);
        this.touchPan.start(manager);

        this.target.addEventListener("contextmenu", this.onContextMenu);
        this.target.addEventListener("mousedown", this.onMouseDown);
        this.target.addEventListener("mouseup", this.onMouseUp);
        this.target.addEventListener("wheel", this.onWheel, {passive: false});
    }

    stop() {
        this.keyMove.stop();
        this.keyHeight.stop();
        this.mouseRotate.stop();
        this.mouseAngle.stop();
        this.touchPan.stop();

        this.target.removeEventListener("contextmenu", this.onContextMenu);
        this.target.removeEventListener("mousedown", this.onMouseDown);
        this.target.removeEventListener("mouseup", this.onMouseUp);
        this.target.removeEventListener("wheel", this.onWheel);
    }

    /**
     * @param delta {number}
     * @param map {Map}
     */
    update(delta, map) {
        FreeFlightControls._beforeMoveTemp.copy(this.manager.position);
        let beforeMoveRot = this.manager.rotation;
        let beforeMoveAngle = this.manager.angle;

        this.keyMove.update(delta, map);
        this.keyHeight.update(delta, map);
        this.mouseRotate.update(delta, map);
        this.mouseAngle.update(delta, map);
        this.touchPan.update(delta, map);

        // if moved, stop following the marker and give back control
        if (this.data.followingPlayer && (
            !FreeFlightControls._beforeMoveTemp.equals(this.manager.position) ||
            beforeMoveRot !== this.manager.rotation ||
            beforeMoveAngle !== this.manager.angle
        )) {
            this.stopFollowingPlayerMarker();
        }

        // follow player marker
        if (this.data.followingPlayer) {
            this.manager.position.copy(this.data.followingPlayer.position);
            this.manager.rotation = (this.data.followingPlayer.rotation.yaw - 180) * DEG2RAD;
            this.manager.angle = -(this.data.followingPlayer.rotation.pitch - 90) * DEG2RAD;
        }

        this.manager.angle = MathUtils.clamp(this.manager.angle, 0, Math.PI);
        this.manager.distance = 0;
        this.manager.ortho = 0;
    }

    initializeHammer() {
        let touchMove = new Pan({ event: 'move', pointers: 1, direction: DIRECTION_ALL, threshold: 0 });
        this.hammer.add(touchMove);
    }

    onContextMenu = evt => {
        evt.preventDefault();
    }

    onMouseDown = evt => {
        this.clickStart.set(evt.x, evt.y);
    }

    onMouseUp = evt => {
        if (Math.abs(this.clickStart.x - evt.x) > 5) return;
        if (Math.abs(this.clickStart.y - evt.y) > 5) return;

        document.body.requestFullscreen()
            .finally(() => {
                this.target.requestPointerLock();
            });
    }

    /**
     * @param marker {object}
     */
    followPlayerMarker(marker) {
        if (marker.isPlayerMarker) marker = marker.data;
        this.data.followingPlayer = marker;
        this.keyMove.deltaPosition.set(0, 0);
    }

    stopFollowingPlayerMarker() {
        this.data.followingPlayer = null;
    }

    onWheel = evt => {
        evt.preventDefault();

        let delta = evt.deltaY;
        if (evt.deltaMode === WheelEvent.DOM_DELTA_PIXEL) delta *= 0.01;
        if (evt.deltaMode === WheelEvent.DOM_DELTA_LINE) delta *= 0.33;

        this.moveSpeed *= Math.pow(1.5, -delta * 0.25);
        this.moveSpeed = MathUtils.clamp(this.moveSpeed, 0.05, 5);

        this.keyMove.speed = this.moveSpeed;
        this.keyHeight.speed = this.moveSpeed;
    }

}