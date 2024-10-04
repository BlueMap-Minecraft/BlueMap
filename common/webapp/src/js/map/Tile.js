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
export class Tile {

    /**
     * @param x {number}
     * @param z {number}
     * @param onLoad {function(Tile)}
     * @param onUnload {function(Tile)}
     */
    constructor(x, z, onLoad, onUnload) {
        Object.defineProperty( this, 'isTile', { value: true } );

        /** @type {THREE.Mesh} */
        this.model = null;

        this.onLoad = onLoad;
        this.onUnload = onUnload;

        this.x = x;
        this.z = z;

        this.unloaded = true;
        this.loading = false;
    }

    /**
     * @param tileLoader {TileLoader}
     * @returns {Promise<void>}
     */
    load(tileLoader) {
        if (this.loading) return Promise.reject("tile is already loading!");
        this.loading = true;

        this.unload();

        this.unloaded = false;
        return tileLoader.load(this.x, this.z, () => this.unloaded)
            .then(model => {
                if (this.unloaded){
                    Tile.disposeModel(model);
                    return;
                }

                this.model = model;
                this.onLoad(this);
            }, () => {
                this.unload();
            })
            .finally(() => {
                this.loading = false;
            });
    }

    unload() {
        this.unloaded = true;
        if (this.model) {
            this.onUnload(this);

            Tile.disposeModel(this.model);

            this.model = null;
        }
    }

    static disposeModel(model) {
        if (model.userData?.tileType === "hires") {
            model.geometry.dispose();
        }

        else if (model.userData?.tileType === "lowres") {
            model.material.uniforms.textureImage.value.dispose();
            model.material.dispose();
        }
    }

    /**
     * @returns {boolean}
     */
    get loaded() {
        return !!this.model;
    }
}
