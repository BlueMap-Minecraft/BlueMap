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

export class KeyCombination {

    static CTRL = 0;
    static SHIFT = 1;
    static ALT = 2;

    /**
     * @param code {string}
     * @param modifiers {...number}
     */
    constructor(code, ...modifiers) {

        this.code = code;
        this.ctrl = modifiers.includes(KeyCombination.CTRL) || this.code === "CtrlLeft" || this.code === "CtrlRight";
        this.shift = modifiers.includes(KeyCombination.SHIFT) || this.code === "ShiftLeft" || this.code === "ShiftRight";
        this.alt = modifiers.includes(KeyCombination.ALT) || this.code === "AltLeft" || this.code === "AltRight";

    }

    /**
     * @param evt {KeyboardEvent}
     * @returns {boolean}
     */
    testDown(evt) {
        return this.code === evt.code &&
            this.ctrl === evt.ctrlKey &&
            this.shift === evt.shiftKey &&
            this.alt === evt.altKey;
    }

    /**
     * @param evt {KeyboardEvent}
     * @returns {boolean}
     */
    testUp(evt) {
        return this.code === evt.code;
    }

    static oneDown(evt, ...combinations) {
        for (let combination of combinations){
            if (combination.testDown(evt)) return true;
        }
        return false;
    }

    static oneUp(evt, ...combinations) {
        for (let combination of combinations){
            if (combination.testUp(evt)) return true;
        }
        return false;
    }

}