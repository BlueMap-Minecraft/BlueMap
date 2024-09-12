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

export class MapHeightControls {

    /**
     * @param cameraHeightStiffness {number}
     * @param targetHeightStiffness {number}
     */
    constructor(cameraHeightStiffness, targetHeightStiffness) {
        this.manager = null;

        this.cameraHeightStiffness = cameraHeightStiffness;
        this.targetHeightStiffness = targetHeightStiffness;
        this.maxAngle = Math.PI / 2;

        this.targetHeight = 0;
        this.cameraHeight = 0;

        this.minCameraHeight = 0;
        this.distanceTagretHeight = 0;
    }

    /**
     * @param manager {ControlsManager}
     */
    start(manager) {
        this.manager = manager;
    }

    stop() {}

    /**
     * @param delta {number}
     * @param map {Map}
     */
    update(delta, map) {

        // adjust target height
        this.updateHeights(delta, map);
        this.manager.position.y = Math.max(this.manager.position.y, this.getSuggestedHeight());
    }

    updateHeights(delta, map) {
        //target height
        let targetSmoothing = this.targetHeightStiffness / (16.666 / delta);
        targetSmoothing = MathUtils.clamp(targetSmoothing, 0, 1);

        let targetTerrainHeight = map.terrainHeightAt(this.manager.position.x, this.manager.position.z) + 3 || 0;

        let targetDelta = targetTerrainHeight - this.targetHeight;
        this.targetHeight += targetDelta * targetSmoothing;
        if (Math.abs(targetDelta) < 0.001) this.targetHeight = targetTerrainHeight;

        // camera height
        this.minCameraHeight = 0;
        if (this.maxAngle >= 0.1) {
            let cameraSmoothing = this.cameraHeightStiffness / (16.666 / delta);
            cameraSmoothing = MathUtils.clamp(cameraSmoothing, 0, 1);

            let cameraTerrainHeight = map.terrainHeightAt(this.manager.camera.position.x, this.manager.camera.position.z) || 0;

            let cameraDelta = cameraTerrainHeight - this.cameraHeight;
            this.cameraHeight += cameraDelta * cameraSmoothing;
            if (Math.abs(cameraDelta) < 0.001) this.cameraHeight = cameraTerrainHeight;

            let maxAngleHeight = Math.cos(this.maxAngle) * this.manager.distance;
            this.minCameraHeight = this.cameraHeight - maxAngleHeight + 1;
        }

        // adjust targetHeight by distance
        this.distanceTagretHeight = MathUtils.lerp(this.targetHeight, 0, Math.min(this.manager.distance / 500, 1));
    }

    getSuggestedHeight() {
        return Math.max(this.distanceTagretHeight, this.minCameraHeight);
    }

}