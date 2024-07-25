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
import {
    TextureLoader,
    Mesh,
    PlaneGeometry,
    FrontSide,
    ShaderMaterial,
    NearestFilter,
    ClampToEdgeWrapping,
    NearestMipMapLinearFilter,
    Vector2
} from "three";

export class LowresTileLoader {

    constructor(tilePath, tileSettings, lod, vertexShader, fragmentShader, uniforms, loadBlocker = () => Promise.resolve(), tileCacheHash = 0) {
        Object.defineProperty( this, 'isLowresTileLoader', { value: true } );

        this.tilePath = tilePath;
        this.tileSettings = tileSettings;
        this.lod = lod;
        this.loadBlocker = loadBlocker;
        this.tileCacheHash = tileCacheHash;

        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        this.uniforms = uniforms;

        this.textureLoader = new TextureLoader();
        this.geometry = new PlaneGeometry(
            tileSettings.tileSize.x + 1, tileSettings.tileSize.z + 1,
            Math.ceil(100 / (lod * 2)), Math.ceil(100 / (lod * 2))
        );
        this.geometry.deleteAttribute('normal');
        this.geometry.deleteAttribute('uv');
        this.geometry.rotateX(-Math.PI / 2);
        this.geometry.translate(tileSettings.tileSize.x / 2 + 1, 0, tileSettings.tileSize.x / 2 + 1);
    }

    load = (tileX, tileZ, cancelCheck = () => false) => {
        let tileUrl = this.tilePath + this.lod + "/" + pathFromCoords(tileX, tileZ) + '.png';

        //await this.loadBlocker();
        return new Promise((resolve, reject) => {
            this.textureLoader.load(tileUrl + '?' + this.tileCacheHash,
                async texture => {
                    texture.anisotropy = 1;
                    texture.generateMipmaps = false;
                    texture.magFilter = NearestFilter;
                    texture.minFilter = texture.generateMipmaps ? NearestMipMapLinearFilter : NearestFilter;
                    texture.wrapS = ClampToEdgeWrapping;
                    texture.wrapT = ClampToEdgeWrapping;
                    texture.flipY = false;
                    texture.flatShading = true;

                    await this.loadBlocker();
                    if (cancelCheck()){
                        texture.dispose();
                        reject({status: "cancelled"});
                        return;
                    }

                    const scale = Math.pow(this.tileSettings.lodFactor, this.lod - 1);

                    let material = new ShaderMaterial({
                        uniforms: {
                            ...this.uniforms,
                            tileSize: {
                                value: new Vector2(this.tileSettings.tileSize.x, this.tileSettings.tileSize.z)
                            },
                            textureSize: {
                                value: new Vector2(texture.image.width, texture.image.height)
                            },
                            textureImage: {
                                type: 't',
                                value: texture
                            },
                            lod: {
                                value: this.lod
                            },
                            lodScale: {
                                value: scale
                            }
                        },
                        vertexShader: this.vertexShader,
                        fragmentShader: this.fragmentShader,
                        depthWrite: true,
                        depthTest: true,
                        vertexColors: true,
                        side: FrontSide,
                        wireframe: false,
                    });

                    let object = new Mesh(this.geometry, material);

                    object.position.set(tileX * this.tileSettings.tileSize.x * scale, 0, tileZ * this.tileSettings.tileSize.z * scale);
                    object.scale.set(scale, 1, scale);

                    object.userData.tileUrl = tileUrl;
                    object.userData.tileType = "lowres";

                    object.updateMatrixWorld(true);

                    resolve(object);
                },
                undefined,
                reject
            );
        });
    }

}
