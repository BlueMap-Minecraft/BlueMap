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

import {MathUtils} from "three";

export class MouseRotateControls {

    /**
     * @param target {Element}
     * @param speedLeft {number}
     * @param speedRight {number}
     * @param speedCapture {number}
     * @param stiffness {number}
     */
    constructor(target, speedLeft, speedRight, speedCapture, stiffness) {
        this.target = target;
        this.manager = null;

        this.moving = false;
        this.lastX = 0;
        this.deltaRotation = 0;

        this.speedLeft = speedLeft;
        this.speedRight = speedRight;
        this.speedCapture = speedCapture;
        this.stiffness = stiffness;

        this.pixelToSpeedMultiplier = 0;
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
        if (this.deltaRotation === 0) return;

        let smoothing = this.stiffness / (16.666 / delta);
        smoothing = MathUtils.clamp(smoothing, 0, 1);

        this.manager.rotation += this.deltaRotation * smoothing;

        this.deltaRotation *= 1 - smoothing;
        if (Math.abs(this.deltaRotation) < 0.0001) {
            this.deltaRotation = 0;
        }
    }

    reset() {
        this.deltaRotation = 0;
    }

    /**
     * @private
     * @param evt {MouseEvent}
     */
    onMouseDown = evt => {
        this.moving = true;
        this.deltaRotation = 0;
        this.lastX = evt.x;
    }

    /**
     * @private
     * @param evt {MouseEvent}
     */
    onMouseMove = evt => {
        if (document.pointerLockElement) {
            this.deltaRotation -= evt.movementX * this.speedCapture * this.pixelToSpeedMultiplier;
        }

        else if(this.moving){
            if (evt.buttons === 1) {
                this.deltaRotation -= (evt.x - this.lastX) * this.speedLeft * this.pixelToSpeedMultiplier;
            } else {
                this.deltaRotation -= (evt.x - this.lastX) * this.speedRight * this.pixelToSpeedMultiplier;
            }
        }

        this.lastX = evt.x;
    }

    /**
     * @private
     * @param evt {MouseEvent}
     */
    onMouseUp = evt => {
        this.moving = false;
    }

    updatePixelToSpeedMultiplier = () => {
        this.pixelToSpeedMultiplier = (1 / this.target.clientWidth) * (this.target.clientWidth / this.target.clientHeight);
    }

}