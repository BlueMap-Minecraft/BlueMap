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
import i18n from "../i18n";

export class MainMenu {

    static NULL_PAGE = {
        id: "-",
        title: "-"
    }

    constructor() {
        this.isOpen = false;
        this.pageStack = [];
    }

    currentPage() {
        if (this.pageStack.length === 0) return MainMenu.NULL_PAGE;
        return this.pageStack[this.pageStack.length - 1];
    }

    openPage(id = "root", title = () => i18n.t("menu.title"), data = {}) {
        if (!this.isOpen){
            this.pageStack.splice(0, this.pageStack.length);
            this.isOpen = true;
        }

        if (typeof title === "function"){
            this.pageStack.push({
                id: id,
                get title(){ return title() },
                ...data
            });
        } else {
            this.pageStack.push({
                id: id,
                title: title,
                ...data
            });
        }

    }

    closePage() {
        this.pageStack.splice(this.pageStack.length - 1, 1);

        if (this.pageStack.length < 1) {
            this.isOpen = false
        }
    }

    reOpenPage() {
        if (this.pageStack.length === 0){
            this.openPage();
        } else if (this.pageStack[0].id !== 'root') {
            this.pageStack.splice(0, this.pageStack.length);
            this.openPage();
        } else {
            this.isOpen = true;
        }
    }

    closeAll() {
        this.isOpen = false;
    }

}