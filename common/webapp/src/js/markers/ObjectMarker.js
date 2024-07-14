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
import {animate, htmlToElement} from "../util/Utils";
import {Vector3} from "three";

export class ObjectMarker extends Marker {

    /**
     * @param markerId {string}
     */
    constructor(markerId) {
        super(markerId);
        Object.defineProperty(this, 'isObjectMarker', {value: true});
        this.data.type = "object";

        this.data.label = null;
        this.data.detail = null;
        this.data.link = null;
        this.data.newTab = true;

        this.lastClick = -1;
    }

    onClick(event) {
        let pos = new Vector3();
        if (event.intersection) {
            pos.copy(event.intersection.pointOnLine || event.intersection.point);
            pos.sub(this.position);
        }

        if (event.data.doubleTap) return false;

        if (this.data.detail || this.data.label) {
            let popup = new LabelPopup(this.data.detail || this.data.label);
            popup.position.copy(pos);
            this.add(popup);
            popup.open();
        }

        if (this.data.link){
            window.open(this.data.link, this.data.newTab ? '_blank' : '_self');
        }

        return true;
    }

    /**
     * @param markerData {{
     *      position: {x: number, y: number, z: number},
     *      label: string,
     *      detail: string,
     *      sorting: number,
     *      listed: boolean,
     *      link: string,
     *      newTab: boolean
     *      }}
     */
    updateFromData(markerData) {

        // update position
        let pos = markerData.position || {};
        this.position.setX(pos.x || 0);
        this.position.setY(pos.y || 0);
        this.position.setZ(pos.z || 0);

        // update label
        this.data.label = markerData.label || null;

        //update detail
        this.data.detail = markerData.detail || null;

        //update sorting
        this.data.sorting = markerData.sorting || 0;

        //update listed
        this.data.listed = markerData.listed === undefined ? true : markerData.listed;

        // update link
        this.data.link = markerData.link || null;
        this.data.newTab = !!markerData.newTab;
    }

}

export class LabelPopup extends CSS2DObject {

    /**
     * @param label {string}
     */
    constructor(label) {
        super(htmlToElement(`<div class="bm-marker-labelpopup">${label}</div>`));
    }

    /**
     * @param autoClose {boolean} - whether this object should be automatically closed and removed again on any other interaction
     */
    open(autoClose = true) {
        let targetOpacity = this.element.style.opacity || 1;

        this.element.style.opacity = 0;
        let inAnimation = animate(progress => {
            this.element.style.opacity = (progress * targetOpacity).toString();
        }, 300);

        if (autoClose) {
            let removeHandler = evt => {
                if (evt.composedPath().includes(this.element)) return;

                inAnimation.cancel();
                this.close();

                window.removeEventListener("mousedown", removeHandler);
                window.removeEventListener("touchstart", removeHandler);
                window.removeEventListener("keydown", removeHandler);
                window.removeEventListener("mousewheel", removeHandler);
            };

            // add listeners delayed to prevent closing
            window.setTimeout(() => {
                window.addEventListener("mousedown", removeHandler);
                window.addEventListener("touchstart", removeHandler, { passive: true });
                window.addEventListener("keydown", removeHandler);
                window.addEventListener("mousewheel", removeHandler);
            }, 100);
        }
    }

    /**
     * @param remove {boolean} - whether this object should be removed from its parent when the close-animation finished
     */
    close(remove = true) {
        let startOpacity = parseFloat(this.element.style.opacity);

        animate(progress => {
            this.element.style.opacity = (startOpacity - progress * startOpacity).toString();
        }, 300, completed => {
            if (remove && completed && this.parent) {
                this.parent.remove(this);
            }
        });
    }

}