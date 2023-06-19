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
import {Marker} from "./Marker";
import {CSS2DObject} from "../util/CSS2DRenderer";
import {animate, EasingFunctions, htmlToElement} from "../util/Utils";

export class PlayerMarker extends Marker {

    /**
     * @param markerId {string}
     * @param playerUuid {string}
     * @param playerHead {string}
     */
    constructor(markerId, playerUuid, playerHead = "assets/steve.png") {
        super(markerId);
        Object.defineProperty(this, 'isPlayerMarker', {value: true});
        this.data.type = "player";

        this.data.playerUuid = playerUuid;
        this.data.name = playerUuid;
        this.data.playerHead = playerHead;
        this.data.rotation = {
            pitch: 0,
            yaw: 0
        };

        this.elementObject = new CSS2DObject(htmlToElement(`
<div id="bm-marker-${this.data.id}" class="bm-marker-${this.data.type}">
    <img src="${this.data.playerHead}" alt="playerhead" draggable="false">
    <div class="bm-player-name"></div>
</div>
        `));
        this.elementObject.onBeforeRender = (renderer, scene, camera) => this.onBeforeRender(renderer, scene, camera);

        this.playerHeadElement = this.element.getElementsByTagName("img")[0];
        this.playerNameElement = this.element.getElementsByTagName("div")[0];

        this.addEventListener( 'removed', () => {
            if (this.element.parentNode) this.element.parentNode.removeChild(this.element);
        });

        this.playerHeadElement.addEventListener('error', () => {
            this.playerHeadElement.src = "assets/steve.png";
        }, {once: true});

        this.add(this.elementObject);
    }

    /**
     * @returns {Element}
     */
    get element() {
        return this.elementObject.element.getElementsByTagName("div")[0];
    }

    onBeforeRender(renderer, scene, camera) {
        let distance = Marker.calculateDistanceToCameraPlane(this.position, camera);

        let value = "near";
        if (distance > 1000) {
            value = "med";
        }
        if (distance > 5000) {
            value = "far";
        }

        this.element.setAttribute("distance-data", value);
    }

    /**
     * @typedef PlayerLike {{
     *      uuid: string,
     *      name: string,
     *      foreign: boolean,
     *      position: {x: number, y: number, z: number},
     *      rotation: {yaw: number, pitch: number, roll: number}
     * }}
     */

    /**
     * @param markerData {PlayerLike}
     */
    updateFromData(markerData) {

        // animate position update
        let pos = markerData.position || {};
        let rot = markerData.rotation || {};
        if (!this.position.x && !this.position.y && !this.position.z) {
            this.position.set(
                pos.x || 0,
                (pos.y || 0) + 1.8,
                pos.z || 0
            );
            this.data.rotation.pitch = rot.pitch || 0;
            this.data.rotation.yaw = rot.yaw || 0;
        } else {
            let startPos = {
                x: this.position.x,
                y: this.position.y,
                z: this.position.z,
                pitch: this.data.rotation.pitch,
                yaw: this.data.rotation.yaw,
            }
            let deltaPos = {
                x: (pos.x || 0) - startPos.x,
                y: ((pos.y || 0) + 1.8) - startPos.y,
                z: (pos.z || 0) - startPos.z,
                pitch: (rot.pitch || 0) - startPos.pitch,
                yaw: (rot.yaw || 0) - startPos.yaw
            }
            while (deltaPos.yaw > 180) deltaPos.yaw -= 360;
            while (deltaPos.yaw < -180) deltaPos.yaw += 360;

            if (deltaPos.x || deltaPos.y || deltaPos.z || deltaPos.pitch || deltaPos.yaw) {
                animate(progress => {
                    let ease = EasingFunctions.easeInOutCubic(progress);
                    this.position.set(
                        startPos.x + deltaPos.x * ease || 0,
                        startPos.y + deltaPos.y * ease || 0,
                        startPos.z + deltaPos.z * ease || 0
                    );
                    this.data.rotation.pitch = startPos.pitch + deltaPos.pitch * ease || 0;
                    this.data.rotation.yaw = startPos.yaw + deltaPos.yaw * ease || 0;
                }, 1000);
            }
        }

        // update name
        let name = markerData.name || this.data.playerUuid;
        this.data.name = name;
        if (this.playerNameElement.innerHTML !== name)
            this.playerNameElement.innerHTML = name;

        // update world
        this.data.foreign = markerData.foreign;
    }

    dispose() {
        super.dispose();

        let element = this.elementObject.element;
        if (element.parentNode) element.parentNode.removeChild(element);
    }

}