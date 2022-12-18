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

import {MathUtils, Vector2} from "three";
import {VEC2_ZERO} from "../../../util/Utils";

export class MouseMoveControls {

    static tempVec2_1 = new Vector2();

    /**
     * @param target {Element}
     * @param speed {number}
     * @param stiffness {number}
     */
    constructor(target, speed, stiffness) {
        this.target = target;
        this.manager = null;

        this.moving = false;
        this.lastPosition = new Vector2();
        this.deltaPosition = new Vector2();

        this.speed = speed;
        this.stiffness = stiffness;

        this.pixelToSpeedMultiplierX = 0;
        this.pixelToSpeedMultiplierY = 0;
        this.updatePixelToSpeedMultiplier();
    }

    /**
     * @param manager {ControlsManager}
     */
    start(manager) {
        this.manager = manager;

        this.target.addEventListener("mousedown", this.onMouseDown);
        window.addEventListener("mousemove", this.onMouseMove);
        window.addEventListener("mouseup", this.onMouseUp);

        window.addEventListener("resize", this.updatePixelToSpeedMultiplier);
    }

    stop() {
        this.target.removeEventListener("mousedown", this.onMouseDown);
        window.removeEventListener("mousemove", this.onMouseMove);
        window.removeEventListener("mouseup", this.onMouseUp);

        window.removeEventListener("resize", this.updatePixelToSpeedMultiplier);
    }

    /**
     * @param delta {number}
     * @param map {Map}
     */
    update(delta, map) {
        if (this.deltaPosition.x === 0 && this.deltaPosition.y === 0) return;

        let smoothing = this.stiffness / (16.666 / delta);
        smoothing = MathUtils.clamp(smoothing, 0, 1);

        let directionDelta = MouseMoveControls.tempVec2_1.copy(this.deltaPosition);
        directionDelta.rotateAround(VEC2_ZERO, this.manager.rotation);

        this.manager.position.x += directionDelta.x * smoothing * this.manager.distance * this.speed * this.pixelToSpeedMultiplierX;
        this.manager.position.z += directionDelta.y * smoothing * this.manager.distance * this.speed * this.pixelToSpeedMultiplierY;

        this.deltaPosition.multiplyScalar(1 - smoothing);
        if (this.deltaPosition.lengthSq() < 0.0001) {
            this.deltaPosition.set(0, 0);
        }
    }

    reset() {
        this.deltaPosition.set(0, 0);
    }

    /**
     * @private
     * @param evt {MouseEvent}
     */
    onMouseDown = evt => {
        if ((evt.buttons !== undefined ? evt.buttons === 1 : evt.button === 0) && !evt.altKey) {
            this.moving = true;
            this.deltaPosition.set(0, 0);
            this.lastPosition.set(evt.x, evt.y);
        }
    }

    /**
     * @private
     * @param evt {MouseEvent}
     */
    onMouseMove = evt => {
        let position = MouseMoveControls.tempVec2_1.set(evt.x, evt.y);

        if(this.moving){
            this.deltaPosition.sub(position).add(this.lastPosition);
        }

        this.lastPosition.copy(position);
    }

    /**
     * @private
     * @param evt {MouseEvent}
     */
    onMouseUp = evt => {
        if (evt.button === 0) this.moving = false;
    }

    updatePixelToSpeedMultiplier = () => {
        this.pixelToSpeedMultiplierX = (1 / this.target.clientWidth) * (this.target.clientWidth / this.target.clientHeight);
        this.pixelToSpeedMultiplierY = 1 / this.target.clientHeight;
    }

}