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

export class TouchZoomControls {

    /**
     * @param hammer {Manager}
     */
    constructor(hammer) {
        this.hammer = hammer;
        this.manager = null;

        this.moving = false;
        this.deltaZoom = 1;
        this.lastZoom = 1;
    }

    /**
     * @param manager {ControlsManager}
     */
    start(manager) {
        this.manager = manager;

        this.hammer.on("zoomstart", this.onTouchDown);
        this.hammer.on("zoommove", this.onTouchMove);
        this.hammer.on("zoomend", this.onTouchUp);
        this.hammer.on("zoomcancel", this.onTouchUp);
    }

    stop() {
        this.hammer.off("zoomstart", this.onTouchDown);
        this.hammer.off("zoommove", this.onTouchMove);
        this.hammer.off("zoomend", this.onTouchUp);
        this.hammer.off("zoomcancel", this.onTouchUp);
    }

    /**
     * @param delta {number}
     * @param map {Map}
     */
    update(delta, map) {
        if (this.deltaZoom === 1) return;

        this.manager.distance /= this.deltaZoom;
        this.deltaZoom = 1;
    }

    reset() {
        this.deltaZoom = 1;
    }

    /**
     * @private
     * @param evt {object}
     */
    onTouchDown = evt => {
        this.moving = true;
        this.lastZoom = 1;
    }

    /**
     * @private
     * @param evt {object}
     */
    onTouchMove = evt => {
        if(this.moving){
            this.deltaZoom *= evt.scale / this.lastZoom;
        }

        this.lastZoom = evt.scale;
    }

    /**
     * @private
     * @param evt {object}
     */
    onTouchUp = evt => {
        this.moving = false;
    }

}