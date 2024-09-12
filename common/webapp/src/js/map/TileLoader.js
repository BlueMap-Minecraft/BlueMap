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
import {pathFromCoords} from "../util/Utils";
import {BufferGeometryLoader, FileLoader, Mesh, Material} from "three";
import {PRBMLoader} from "./hires/PRBMLoader";

export class TileLoader {

    /**
     * @param tilePath {string}
     * @param material {Material | Material[]}
     * @param tileSettings {{
     *      tileSize: {x: number, z: number},
     *	    scale: {x: number, z: number},
     *      translate: {x: number, z: number}
     * }}
     * @param loadBlocker {function: Promise}
     * @param tileCacheHash {number}
     */
    constructor(tilePath, material, tileSettings, loadBlocker = () => Promise.resolve(), tileCacheHash = 0) {
        Object.defineProperty( this, 'isTileLoader', { value: true } );

        this.tilePath = tilePath;
        this.material = material;
        this.tileSettings = tileSettings;

        this.tileCacheHash = tileCacheHash;

        this.loadBlocker = loadBlocker;

        this.fileLoader = new FileLoader();
        this.fileLoader.setResponseType('arraybuffer');

        this.bufferGeometryLoader = new PRBMLoader();
    }

    load = (tileX, tileZ, cancelCheck = () => false) => {
        let tileUrl = this.tilePath + pathFromCoords(tileX, tileZ) + '.prbm';

        return new Promise((resolve, reject) => {
            this.fileLoader.load(tileUrl + '?' + this.tileCacheHash,
                async data => {

                    await this.loadBlocker();
                    if (cancelCheck()){
                        reject({status: "cancelled"});
                        return;
                    }

                    let geometry = this.bufferGeometryLoader.parse(data);

                    let object = new Mesh(geometry, this.material);

                    let tileSize = this.tileSettings.tileSize;
                    let translate = this.tileSettings.translate;
                    let scale = this.tileSettings.scale;
                    object.position.set(tileX * tileSize.x + translate.x, 0, tileZ * tileSize.z + translate.z);
                    object.scale.set(scale.x, 1, scale.z);

                    object.userData.tileUrl = tileUrl;
                    object.userData.tileType = "hires";

                    object.updateMatrixWorld(true);

                    resolve(object);
                },
                () => {},
                reject
            );
        });
    }

}
