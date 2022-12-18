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
import {Marker} from "@/js/markers/Marker";
import {CSS2DObject} from "@/js/util/CSS2DRenderer";
import {animate, htmlToElement} from "@/js/util/Utils";
import {BoxGeometry, MeshBasicMaterial, Mesh, Vector2} from "three";
import i18n from "../i18n";

export class PopupMarker extends Marker {

    constructor(id, appState, events) {
        super(id);

        this.data.type = "popup";
        this.data.label = "Last Map Interaction";

        this.appState = appState;
        this.events = events;
        this.visible = false;

        this.elementObject = new CSS2DObject(htmlToElement(`<div id="bm-marker-${this.data.id}" class="bm-marker-${this.data.type}">Test</div>`));
        this.elementObject.position.set(0.5, 1, 0.5);
        this.addEventListener( 'removed', () => {
            if (this.element.parentNode) this.element.parentNode.removeChild(this.element);
        });

        let cubeGeo = new BoxGeometry(1.01, 1.01, 1.01).translate(0.5, 0.5, 0.5);
        let cubeMaterial = new MeshBasicMaterial( {color: 0xffffff, opacity: 0.5, transparent: true} );
        this.cube = new Mesh(cubeGeo, cubeMaterial);
        this.cube.onClick = evt => this.onClick(evt);

        this.add(this.elementObject);
        this.add(this.cube);

        this.animation = null;

        this.events.addEventListener('bluemapMapInteraction', this.onMapInteraction);

        window.addEventListener("mousedown", this.removeHandler);
        window.addEventListener("touchstart", this.removeHandler);
        window.addEventListener("keydown", this.removeHandler);
        window.addEventListener("mousewheel", this.removeHandler);
    }

    onClick(event) {
        return true;
    }

    onMapInteraction = evt => {
        let isHires = true;
        let int = evt.detail.hiresHit;

        if (evt.detail.lowresHits) {
            for (let i = 0; i < evt.detail.lowresHits.length; i++) {
                if (!int) {
                    isHires = false;
                    int = evt.detail.lowresHits[i];
                } else break;
            }
        }

        if (!int) return;

        this.position
            .copy(int.pointOnLine || int.point)
            .add(evt.detail.ray.direction.clone().multiplyScalar(0.05))
            .floor();

        //this.elementObject.position
            //.copy(evt.detail.intersection.pointOnLine || evt.detail.intersection.point)
            //.sub(this.position);

        if (isHires) {
            this.element.innerHTML = `
                <div class="group">
                    <div class="label">${i18n.t("blockTooltip.block")}:</div>
                    <div class="content">
                        <div class="entry"><span class="label">x: </span><span class="value">${this.position.x}</span></div>
                        <div class="entry"><span class="label">y: </span><span class="value">${this.position.y}</span></div>
                        <div class="entry"><span class="label">z: </span><span class="value">${this.position.z}</span></div>
                    </div>
                </div>
            `;
        } else {
            this.element.innerHTML = `
                <div class="group">
                    <div class="label">${i18n.t("blockTooltip.position")}:</div>
                    <div class="content">
                        <div class="entry"><span class="label">x: </span><span class="value">${this.position.x}</span></div>
                        <div class="entry"><span class="label">z: </span><span class="value">${this.position.z}</span></div>
                    </div>
                </div>
            `;
        }

        if (this.appState.debug) {
            let chunkCoords = this.position.clone().divideScalar(16).floor();
            let regionCoords = new Vector2(this.position.x, this.position.z).divideScalar(512).floor();
            let regionFile = `r.${regionCoords.x}.${regionCoords.y}.mca`;

            this.element.innerHTML += `
                <hr>
                <div class="group">
                    <div class="label">${i18n.t("blockTooltip.chunk")}:</div>
                    <div class="content">
                        <div class="entry"><span class="label">x: </span><span class="value">${chunkCoords.x}</span></div>
                        <div class="entry"><span class="label">y: </span><span class="value">${chunkCoords.y}</span></div>
                        <div class="entry"><span class="label">z: </span><span class="value">${chunkCoords.z}</span></div>
                    </div>
                </div>
                <hr>
                <div class="group">
                    <div class="label">${i18n.t("blockTooltip.region.region")}:</div>
                    <div class="content">
                        <div class="entry"><span class="label">x: </span><span class="value">${regionCoords.x}</span></div>
                        <div class="entry"><span class="label">z: </span><span class="value">${regionCoords.y}</span></div>
                    </div>
                    <div class="content">
                        <div class="entry"><span class="label">${i18n.t("blockTooltip.region.file")}: </span><span class="value">${regionFile}</span></div>
                    </div>
                </div>
            `;
        }

        if (this.appState.debug) {
            let faceIndex = int.faceIndex;
            let attributes = int.object.geometry.attributes;
            if (attributes.sunlight && attributes.blocklight) {
                let sunlight = attributes.sunlight.array[faceIndex * 3];
                let blocklight = attributes.blocklight.array[faceIndex * 3];

                this.element.innerHTML += `
                    <hr>
                    <div class="group">
                        <div class="label">${i18n.t("blockTooltip.light.light")}:</div>
                        <div class="content">
                            <div class="entry"><span class="label">${i18n.t("blockTooltip.light.sun")}: </span><span class="value">${sunlight}</span></div>
                            <div class="entry"><span class="label">${i18n.t("blockTooltip.light.block")}: </span><span class="value">${blocklight}</span></div>
                        </div>
                    </div>
                `;
            }
        }

        if (this.appState.debug) {
            let info = "";

            if (isHires) {
                let hrPath = evt.detail.hiresHit?.object?.userData?.tileUrl;
                info += `<div>${hrPath}</div>`;
            }

            if (evt.detail.lowresHits) {
                for (let i = 0; i < evt.detail.lowresHits.length; i++) {
                    let lrPath = evt.detail.lowresHits[i]?.object?.userData?.tileUrl;
                    if (lrPath) info += `<div>${lrPath}</div>`;
                }
            }

            this.element.innerHTML += `
                <hr>
                <div class="files">
                    ${info}
                </div>
            `;
        }

        if (this.appState.debug){
            console.debug("Clicked Position Data:", evt.detail);
        }

        this.open();
    };

    open() {
        if (this.animation) this.animation.cancel();

        this.visible = true;
        this.cube.visible = true;

        let targetOpacity = 1;

        this.element.style.opacity = "0";
        this.animation = animate(progress => {
            this.element.style.opacity = (progress * targetOpacity).toString();
        }, 300);
    }

    removeHandler = evt => {
        if (evt.composedPath().includes(this.element)) return;
        this.close();
    }

    close() {
        if (this.animation) this.animation.cancel();

        this.cube.visible = false;

        let startOpacity = parseFloat(this.element.style.opacity);
        this.animation = animate(progress => {
            this.element.style.opacity = (startOpacity - progress * startOpacity).toString();
        }, 300, finished => {
            if (finished) this.visible = false;
        });
    }

    /**
     * @returns {Element}
     */
    get element() {
        return this.elementObject.element.getElementsByTagName("div")[0];
    }

    dispose() {
        super.dispose();

        if (this.element.parentNode) this.element.parentNode.removeChild(this.element);
    }

}