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
import {HtmlMarker} from "./HtmlMarker";

export class PoiMarker extends HtmlMarker {

    /**
     * @param markerId {string}
     */
    constructor(markerId) {
        super(markerId);
        Object.defineProperty(this, 'isPoiMarker', {value: true});
        this.data.type = "poi";

        this.data.detail = null;

        this.html = `<img src="" alt="POI Icon (${this.data.id})" class="bm-marker-poi-icon" draggable="false" style="pointer-events: auto"><div class="bm-marker-poi-label"></div>`;

        this.iconElement = this.element.getElementsByTagName("img").item(0);
        this.labelElement = this.element.getElementsByTagName("div").item(0);

        this._lastIcon = null;
    }

    onClick(event) {
        if (event.data.doubleTap) return false;

        if (this.highlight || !this.data.label) return true;
        this.highlight = true;

        let eventHandler = evt => {
            if (evt.composedPath().includes(this.element)) return;

            this.highlight = false;

            window.removeEventListener("mousedown", eventHandler);
            window.removeEventListener("touchstart", eventHandler);
            window.removeEventListener("keydown", eventHandler);
            window.removeEventListener("mousewheel", eventHandler);
        };

        setTimeout(function () {
            window.addEventListener("mousedown", eventHandler);
            window.addEventListener("touchstart", eventHandler, { passive: true });
            window.addEventListener("keydown", eventHandler);
            window.addEventListener("mousewheel", eventHandler);
        }, 0);

        return true;
    }

    set highlight(highlight) {
        if (highlight) {
            this.element.classList.add("bm-marker-highlight");
        } else {
            this.element.classList.remove("bm-marker-highlight");
        }
    }

    get highlight() {
        return this.element.classList.contains("bm-marker-highlight");
    }

    /**
     * @param markerData {{
     *      position: {x: number, y: number, z: number},
     *      anchor: {x: number, y: number},
     *      iconAnchor: {x: number, y: number},
     *      label: string,
     *      detail: string,
     *      sorting: number,
     *      listed: boolean,
     *      icon: string,
     *      classes: string[],
     *      minDistance: number,
     *      maxDistance: number
     * }}
     */
    updateFromData(markerData) {

        // update position
        let pos = markerData.position || {};
        this.position.setX(pos.x || 0);
        this.position.setY(pos.y || 0);
        this.position.setZ(pos.z || 0);

        // update anchor
        let anch = markerData.anchor || markerData.iconAnchor || {}; //"iconAnchor" for backwards compatibility
        this.iconElement.style.transform = `translate(${-anch.x}px, ${-anch.y}px)`;
        //this.anchor.setX(anch.x || 0);
        //this.anchor.setY(anch.y || 0);


        // update label
        if (this.data.label !== markerData.label){
            this.data.label = markerData.label || "";
        }

        //update sorting
        if (this.data.sorting !== markerData.sorting) {
            this.data.sorting = markerData.sorting || 0;
        }

        //update listed
        if (this.data.listed !== markerData.listed) {
            this.data.listed = markerData.listed === undefined ? true : markerData.listed;
        }

        // update detail
        if (this.data.detail !== markerData.detail){
            this.data.detail = markerData.detail || this.data.label;
            this.labelElement.innerHTML = this.data.detail || "";
        }

        // update icon
        if (this._lastIcon !== markerData.icon){
            this.iconElement.src = markerData.icon || "assets/poi.svg";
            this._lastIcon = markerData.icon;
        }

        // update style-classes
        if (this.data.classes !== markerData.classes) {
            this.data.classes = markerData.classes;
            let highlight = this.element.classList.contains("bm-marker-highlight");

            this.element.classList.value = `bm-marker-html`;
            if (highlight) this.element.classList.add("bm-marker-highlight");
            this.element.classList.add(...markerData.classes);
        }

        // update min/max distances
        this.fadeDistanceMin = markerData.minDistance || 0;
        this.fadeDistanceMax = markerData.maxDistance !== undefined ? markerData.maxDistance : Number.MAX_VALUE;

    }

}