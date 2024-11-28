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
import {MapControls} from "../MapControls";
import {softMax, softSet} from "../../../util/Utils";

export class MouseAngleControls {

    /**
     * @param target {EventTarget}
     * @param speed {number}
     * @param stiffness {number}
     */
    constructor(target, speed, stiffness) {
        this.target = target;
        this.manager = null;

        this.moving = false;
        this.lastY = 0;
        this.deltaAngle = 0;

        this.dynamicDistance = false;
        this.startDistance = 0;

        this.speed = speed;
        this.stiffness = stiffness;

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
        window.addEventListener("wheel", this.onWheel);

        window.addEventListener("resize", this.updatePixelToSpeedMultiplier);
    }

    stop() {
        this.target.removeEventListener("mousedown", this.onMouseDown);
        window.removeEventListener("mousemove", this.onMouseMove);
        window.removeEventListener("mouseup", this.onMouseUp);
        window.removeEventListener("wheel", this.onWheel);

        window.removeEventListener("resize", this.updatePixelToSpeedMultiplier);
    }

    /**
     * @param delta {number}
     * @param map {Map}
     */
    update(delta, map) {
        if (this.deltaAngle === 0) return;

        let smoothing = this.stiffness / (16.666 / delta);
        smoothing = MathUtils.clamp(smoothing, 0, 1);

        this.manager.angle += this.deltaAngle * smoothing * this.speed * this.pixelToSpeedMultiplierY;

        if (this.dynamicDistance) {
            let targetDistance = this.startDistance
            targetDistance = Math.min(targetDistance, MapControls.getMaxDistanceForPerspectiveAngle(this.manager.angle));
            targetDistance = Math.max(targetDistance, this.manager.controls.minDistance);
            this.manager.distance = softSet(this.manager.distance, targetDistance, 0.4);
            this.manager.angle = softMax(this.manager.angle, MapControls.getMaxPerspectiveAngleForDistance(targetDistance), 0.8);
        } else {
            this.manager.angle = softMax(this.manager.angle, MapControls.getMaxPerspectiveAngleForDistance(this.manager.distance), 0.8);
        }

        this.deltaAngle *= 1 - smoothing;
        if (Math.abs(this.deltaAngle) < 0.0001) {
            this.deltaAngle = 0;
        }
    }

    reset() {
        this.deltaAngle = 0;
    }

    /**
     * @private
     * @param evt {MouseEvent}
     */
    onMouseDown = evt => {
        if ((evt.buttons !== undefined ? evt.buttons === 2 : evt.button === 2) ||
            ((evt.altKey || evt.ctrlKey) && (evt.buttons !== undefined ? evt.buttons === 1 : evt.button === 0))) {
            this.moving = true;
            this.deltaAngle = 0;
            this.lastY = evt.y;

            this.startDistance = this.manager.distance;
            this.dynamicDistance = this.manager.distance < 1000;
        }
    }

    /**
     * @private
     * @param evt {MouseEvent}
     */
    onMouseMove = evt => {
        if(this.moving){
            this.deltaAngle -= evt.y - this.lastY;
        }

        this.lastY = evt.y;
    }

    /**
     * @private
     * @param evt {MouseEvent}
     */
    onMouseUp = evt => {
        this.moving = false;
    }

    onWheel = evt => {
        this.dynamicDistance = false;
    }

    updatePixelToSpeedMultiplier = () => {
        this.pixelToSpeedMultiplierY = 1 / this.target.clientHeight;
    }

}