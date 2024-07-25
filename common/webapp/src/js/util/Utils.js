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
import {MathUtils, Vector2, Vector3} from "three";

export const VEC2_ZERO = new Vector2(0, 0);
export const VEC3_ZERO = new Vector3(0, 0, 0);
export const VEC3_X = new Vector3(1, 0, 0);
export const VEC3_Y = new Vector3(0, 1, 0);
export const VEC3_Z = new Vector3(0, 0, 1);

/**
 * Converts a url-encoded image string to an actual image-element
 * @param string {string}
 * @returns {HTMLImageElement}
 */
export const stringToImage = string => {
    let image = document.createElementNS('http://www.w3.org/1999/xhtml', 'img');
    image.src = string;
    return image;
};

/**
 * Creates an optimized path from x,z coordinates used by bluemap to save tiles
 * @param x {number}
 * @param z {number}
 * @returns {string}
 */
export const pathFromCoords = (x, z) => {
    let path = 'x';
    path += splitNumberToPath(x);

    path += 'z';
    path += splitNumberToPath(z);

    path = path.substring(0, path.length - 1);

    return path;
};

/**
 * Splits a number into an optimized folder-path used to save bluemap-tiles
 * @param num {number}
 * @returns {string}
 */
const splitNumberToPath = num => {
    let path = '';

    if (num < 0) {
        num = -num;
        path += '-';
    }

    let s = num.toString();

    for (let i = 0; i < s.length; i++) {
        path += s.charAt(i) + '/';
    }

    return path;
};

/**
 * Hashes tile-coordinates to be saved in a map
 * @param x {number}
 * @param z {number}
 * @returns {string}
 */
export const hashTile = (x, z) => `x${x}z${z}`;

export const generateCacheHash = () => {
    return Math.round(Math.random() * 1000000);
}

/**
 * Dispatches an event to the element of this map-viewer
 * @param element {EventTarget} the element on that the event is dispatched
 * @param event {string}
 * @param detail {object}
 * @returns {undefined|void|boolean}
 */
export const dispatchEvent = (element, event, detail = {}) => {
    if (!element || !element.dispatchEvent) return;

    return element.dispatchEvent(new CustomEvent(event, {
        detail: detail
    }));
}

/**
 * Sends a "bluemapAlert" event with a message and a level.
 * The level can be anything, but the app uses the levels
 * - debug
 * - fine
 * - info
 * - warning
 * - error
 * @param element {EventTarget} the element on that the event is dispatched
 * @param message {object}
 * @param level {string}
 */
export const alert = (element, message, level = "info") => {

    // alert event
    let printToConsole = dispatchEvent(element, "bluemapAlert", {
        message: message,
        level: level
    });

    // log alert to console
    if (printToConsole !== false) {
        if (level === "info") {
            console.log(`[BlueMap/${level}]`, message);
        } else if (level === "warning") {
            console.warn(`[BlueMap/${level}]`, message);
        } else if (level === "error") {
            console.error(`[BlueMap/${level}]`, message);
        } else {
            console.debug(`[BlueMap/${level}]`, message);
        }
    }
}

/**
 * Source: https://stackoverflow.com/questions/494143/creating-a-new-dom-element-from-an-html-string-using-built-in-dom-methods-or-pro/35385518#35385518
 *
 * @param html {string} representing a single element
 * @return {Element}
 */
export const htmlToElement = html => {
    let template = document.createElement('template');
    template.innerHTML = html.trim();
    return template.content.firstChild;
}

/**
 * Source: https://stackoverflow.com/questions/494143/creating-a-new-dom-element-from-an-html-string-using-built-in-dom-methods-or-pro/35385518#35385518
 *
 * @param html {string} representing any number of sibling elements
 * @return {NodeList}
 */
export const htmlToElements = html => {
    let template = document.createElement('template');
    template.innerHTML = html;
    return template.content.childNodes;
}

/**
 * Schedules an animation
 * @param durationMs {number} the duration of the animation in ms
 * @param animationFrame {function(progress: number, deltaTime: number)} a function that is getting called each frame with the parameters (progress (0-1), deltaTime)
 * @param postAnimation {function(finished: boolean)} a function that gets called once after the animation is finished or cancelled. The function accepts one bool-parameter whether the animation was finished (true) or canceled (false)
 * @returns {{cancel: function()}} the animation object
 */
export const animate = function (animationFrame, durationMs = 1000, postAnimation = null) {
    let animation = {
        animationStart: -1,
        lastFrame: -1,
        cancelled: false,

        frame: function (time) {
            if (this.cancelled) return;

            if (this.animationStart === -1) {
                this.animationStart = time;
                this.lastFrame = time;
            }

            let progress = durationMs === 0 ? 1 : MathUtils.clamp((time - this.animationStart) / durationMs, 0, 1);
            let deltaTime = time - this.lastFrame;

            animationFrame(progress, deltaTime);

            if (progress < 1) window.requestAnimationFrame(time => this.frame(time));
            else if (postAnimation) postAnimation(true);

            this.lastFrame = time;
        },

        cancel: function () {
            this.cancelled = true;
            if (postAnimation) postAnimation(false);
        }
    };

    if (durationMs !== 0) {
        window.requestAnimationFrame(time => animation.frame(time));
    } else {
        animation.frame(0);
    }

    return animation;
}

/**
 * Source: https://gist.github.com/gre/1650294
 * @type {{
 *      easeOutCubic: (function(number): number),
 *      linear: (function(number): number),
 *      easeOutQuint: (function(number): number),
 *      easeInQuart: (function(number): number),
 *      easeInOutQuint: (function(number): number),
 *      easeInQuad: (function(number): number),
 *      easeOutQuart: (function(number): number),
 *      easeInCubic: (function(number): number),
 *      easeInQuint: (function(number): number),
 *      easeOutQuad: (function(number): number),
 *      easeInOutQuad: (function(number): number),
 *      easeInOutCubic: (function(number): number),
 *      easeInOutQuart: (function(number): number)
 *      }}
 */
export const EasingFunctions = {
    // no easing, no acceleration
    linear: t => t,
    // accelerating from zero velocity
    easeInQuad: t => t*t,
    // decelerating to zero velocity
    easeOutQuad: t => t*(2-t),
    // acceleration until halfway, then deceleration
    easeInOutQuad: t => t<.5 ? 2*t*t : -1+(4-2*t)*t,
    // accelerating from zero velocity
    easeInCubic: t => t*t*t,
    // decelerating to zero velocity
    easeOutCubic: t => (--t)*t*t+1,
    // acceleration until halfway, then deceleration
    easeInOutCubic: t => t<.5 ? 4*t*t*t : (t-1)*(2*t-2)*(2*t-2)+1,
    // accelerating from zero velocity
    easeInQuart: t => t*t*t*t,
    // decelerating to zero velocity
    easeOutQuart: t => 1-(--t)*t*t*t,
    // acceleration until halfway, then deceleration
    easeInOutQuart: t => t<.5 ? 8*t*t*t*t : 1-8*(--t)*t*t*t,
    // accelerating from zero velocity
    easeInQuint: t => t*t*t*t*t,
    // decelerating to zero velocity
    easeOutQuint: t => 1+(--t)*t*t*t*t,
    // acceleration until halfway, then deceleration
    easeInOutQuint: t => t<.5 ? 16*t*t*t*t*t : 1+16*(--t)*t*t*t*t
}

/**
 * Returns the offset position of an element
 *
 * Source: https://plainjs.com/javascript/styles/get-the-position-of-an-element-relative-to-the-document-24/
 *
 * @param element {Element}
 * @returns {{top: number, left: number}}
 */
export const elementOffset = element => {
    let rect = element.getBoundingClientRect(),
        scrollLeft = window.pageXOffset || document.documentElement.scrollLeft,
        scrollTop = window.pageYOffset || document.documentElement.scrollTop;
    return { top: rect.top + scrollTop, left: rect.left + scrollLeft }
}

/**
 * Very simple deep equals, should not be used for complex objects. Is designed for comparing parsed json-objects.
 * @param object1 {object}
 * @param object2 {object}
 * @returns {boolean}
 */
export const deepEquals = (object1, object2) => {
    if (Object.is(object1, object2)) return true;

    let type = typeof object1;
    if (type !== typeof object2) return false;

    if (type === 'number' || type === 'boolean' || type === 'string') return false;

    if (Array.isArray(object1)){
        let len = object1.length;
        if (len !== object2.length) return false;
        for (let i = 0; i < len; i++) {
            if (!deepEquals(object1[i], object2[i])) return false;
        }

        return true;
    }

    for (let property in object1) {
        if (!object1.hasOwnProperty(property)) continue;
        if (!deepEquals(object1[property], object2[property])) return false;
    }

    return true;
}

/**
 * Adds one listener to multiple events
 * @param target {EventTarget}
 * @param types {string|string[]}
 * @param listener {EventListenerOrEventListenerObject | null}
 * @param options {boolean | AddEventListenerOptions?}
 */
export const addEventListeners = (target, types, listener, options) => {
    if (!Array.isArray(types)){
        types = types.trim().split(" ");
    }

    types.forEach(type => target.addEventListener(type, listener, options));
}

/**
 * Removes one listener to multiple events
 * @param target {EventTarget}
 * @param types {string|string[]}
 * @param listener {EventListenerOrEventListenerObject | null}
 * @param options {boolean | EventListenerOptions?}
 */
export const removeEventListeners = (target, types, listener, options) => {
    if (!Array.isArray(types)){
        types = types.trim().split(" ");
    }

    types.forEach(type => target.removeEventListener(type, listener, options));
}

/**
 * Softly clamps towards a minimum value
 * @param value {number}
 * @param min {number}
 * @param stiffness {number}
 * @returns {number}
 */
export const softMin = (value, min, stiffness) => {
    if (value >= min) return value;
    let delta = min - value;
    if (delta < 0.0001) return min;
    return value + delta * stiffness;
}

/**
 * Softly clamps towards a maximum value
 * @param value {number}
 * @param max {number}
 * @param stiffness {number}
 * @returns {number}
 */
export const softMax = (value, max, stiffness) => {
    if (value <= max) return value;
    let delta = value - max;
    if (delta < 0.0001) return max;
    return value - delta * stiffness;
}

/**
 * Softly clamps towards a minimum and maximum value
 * @param value {number}
 * @param min {number}
 * @param max {number}
 * @param stiffness {number}
 * @returns {number}
 */
export const softClamp = (value, min, max, stiffness) => {
    return softMax(softMin(value, min, stiffness), max, stiffness);
}

export const vecArrToObj = (val, useZ = false) => {
    if (val && val.length >= 2) {
        if (useZ) return {x: val[0], z: val[1]};
        return {x: val[0], y: val[1]};
    }
    return {};
}

const pixel = document.createElement('canvas');
pixel.width = 1;
pixel.height = 1;
const pixelContext = pixel.getContext('2d', {
    willReadFrequently: true
});

export const getPixel = (img, x, y) => {
    pixelContext.drawImage(img, Math.floor(x), Math.floor(y), 1, 1, 0, 0, 1, 1);
    return pixelContext.getImageData(0, 0, 1, 1).data;
}
