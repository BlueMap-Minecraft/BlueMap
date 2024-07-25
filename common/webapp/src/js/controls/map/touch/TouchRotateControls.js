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

export class TouchRotateControls {

    /**
     * @param hammer {Manager}
     * @param speed {number}
     * @param stiffness {number}
     */
    constructor(hammer, speed, stiffness) {
        this.hammer = hammer;
        this.manager = null;

        this.moving = false;
        this.lastRotation = 0;
        this.deltaRotation = 0;

        this.speed = speed;
        this.stiffness = stiffness;
    }

    /**
     * @param manager {ControlsManager}
     */
    start(manager) {
        this.manager = manager;

        this.hammer.on("rotatestart", this.onTouchDown);
        this.hammer.on("rotatemove", this.onTouchMove);
        this.hammer.on("rotateend", this.onTouchUp);
        this.hammer.on("rotatecancel", this.onTouchUp);
    }

    stop() {
        this.hammer.off("rotatestart", this.onTouchDown);
        this.hammer.off("rotatemove", this.onTouchMove);
        this.hammer.off("rotateend", this.onTouchUp);
        this.hammer.off("rotatecancel", this.onTouchUp);
    }

    /**
     * @param delta {number}
     * @param map {Map}
     */
    update(delta, map) {
        if (this.deltaRotation === 0) return;

        let smoothing = this.stiffness / (16.666 / delta);
        smoothing = MathUtils.clamp(smoothing, 0, 1);

        this.manager.rotation += this.deltaRotation * smoothing * this.speed;

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
     * @param evt {object}
     */
    onTouchDown = evt => {
        this.moving = true;
        this.deltaRotation = 0;
        this.lastRotation = evt.rotation;
    }

    /**
     * @private
     * @param evt {object}
     */
    onTouchMove = evt => {
        if(this.moving){
            let delta = evt.rotation - this.lastRotation;
            if (delta > 180) delta -= 360;
            if (delta < -180) delta += 360;

            this.deltaRotation -= delta;
        }

        this.lastRotation = evt.rotation;
    }

    /**
     * @private
     * @param evt {object}
     */
    onTouchUp = evt => {
        this.moving = false;
    }

}