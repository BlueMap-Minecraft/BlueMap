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
import {CanvasTexture, DoubleSide, FrontSide, MeshBasicMaterial, NearestFilter} from "three";
import {PlayerObject} from "skinview3d";
import {inferModelType, loadSkinToCanvas} from "skinview-utils";

// skinview3d builds at 1 unit = 1 skin pixel; 16 pixels = 1 Minecraft block
const PLAYER_MODEL_SCALE = 1 / 16;

export class PlayerMarker extends Marker {

    /**
     * @param markerId {string}
     * @param playerUuid {string}
     * @param playerSkinUrl {string} - URL to the full 64x64 skin PNG
     * @param playerCapeUrl {string|null} - URL to the cape PNG (optional)
     */
    constructor(markerId, playerUuid, playerSkinUrl = "assets/steve_skin.png", playerCapeUrl = null) {
        super(markerId);
        Object.defineProperty(this, 'isPlayerMarker', {value: true});
        this.data.type = "player";

        this.data.playerUuid = playerUuid;
        this.data.name = playerUuid;
        this.data.playerSkinUrl = playerSkinUrl;
        this.data.playerCapeUrl = playerCapeUrl;
        this.data.rotation = {pitch: 0, yaw: 0};

        // 3D player model via skinview3d
        this.playerObject = new PlayerObject();
        this.playerObject.scale.setScalar(PLAYER_MODEL_SCALE);
        // Lift model 1 block so feet align with the player's Y coordinate
        this.playerObject.position.y = 1.0;
        // BlueMap uses custom shaders with no Three.js scene lights; swap
        // skinview3d's MeshStandardMaterial for unlit MeshBasicMaterial.
        this._replaceWithBasicMaterials();
        // Hide cape by default; only shown if a cape texture loads successfully
        this.playerObject.cape.visible = false;
        this.add(this.playerObject);

        // Load and apply the skin texture
        this._loadSkin(playerSkinUrl);

        // CSS2D name label positioned above the model (~2 blocks up)
        this.elementObject = new CSS2DObject(htmlToElement(`
<div id="bm-marker-${this.data.id}" class="bm-marker-${this.data.type}">
    <div class="bm-player-name"></div>
</div>
        `));
        this.elementObject.position.set(0, 2.2, 0);
        this.elementObject.onBeforeRender = (renderer, scene, camera) => this.onBeforeRender(renderer, scene, camera);

        this.playerNameElement = this.element.getElementsByTagName("div")[0];

        this.addEventListener('removed', () => {
            if (this.element.parentNode) this.element.parentNode.removeChild(this.element);
        });

        this.add(this.elementObject);
    }

    /**
     * Replace skinview3d's MeshStandardMaterial with MeshBasicMaterial so the model
     * renders without scene lights (BlueMap uses custom shaders, not Three.js lights).
     * @private
     */
    _replaceWithBasicMaterials() {
        const s = this.playerObject.skin;
        const toBasic = (src) => new MeshBasicMaterial({
            side: src.side,
            transparent: src.transparent,
            alphaTest: src.alphaTest,
            polygonOffset: src.polygonOffset,
            polygonOffsetFactor: src.polygonOffsetFactor,
            polygonOffsetUnits: src.polygonOffsetUnits,
        });

        // Capture old references before creating new materials
        const oldL1  = s.layer1Material;
        const oldL2  = s.layer2Material;
        const oldL1b = s.layer1MaterialBiased;
        const oldL2b = s.layer2MaterialBiased;

        const l1  = toBasic(oldL1);
        const l2  = toBasic(oldL2);
        const l1b = toBasic(oldL1b);
        const l2b = toBasic(oldL2b);

        // Swap materials on all existing meshes
        s.traverse(child => {
            if (!child.isMesh) return;
            if      (child.material === oldL1)  child.material = l1;
            else if (child.material === oldL2)  child.material = l2;
            else if (child.material === oldL1b) child.material = l1b;
            else if (child.material === oldL2b) child.material = l2b;
        });

        // Update SkinObject references so skin.map setter still works
        s.layer1Material        = l1;
        s.layer2Material        = l2;
        s.layer1MaterialBiased  = l1b;
        s.layer2MaterialBiased  = l2b;

        oldL1.dispose();
        oldL2.dispose();
        oldL1b.dispose();
        oldL2b.dispose();

        // Also replace the cape's MeshStandardMaterial
        const cape = this.playerObject.cape;
        const oldCapeMat = cape.material;
        if (oldCapeMat) {
            cape.material = toBasic(oldCapeMat);
            oldCapeMat.dispose();
        }
    }

    /**
     * Load a skin URL into a canvas, detect model type, and apply as a CanvasTexture.
     * Falls back to steve_skin.png on any error.
     * @param {string} url
     * @private
     */
    _loadSkin(url) {
        const FALLBACK = "assets/steve_skin.png";
        const img = new Image();
        img.crossOrigin = "anonymous";
        img.onload = () => {
            const w = img.naturalWidth, h = img.naturalHeight;
            if (w !== 64 || (h !== 32 && h !== 64)) {
                if (url !== FALLBACK) this._loadSkin(FALLBACK);
                return;
            }
            try {
                const canvas = document.createElement("canvas");
                canvas.width = w;
                canvas.height = h;
                loadSkinToCanvas(canvas, img);

                const texture = new CanvasTexture(canvas);
                texture.magFilter = NearestFilter;
                texture.minFilter = NearestFilter;

                this.playerObject.skin.map = texture;
                this.playerObject.skin.modelType = inferModelType(canvas);
            } catch (e) {
                if (url !== FALLBACK) this._loadSkin(FALLBACK);
            }
        };
        img.onerror = () => {
            if (url !== FALLBACK) this._loadSkin(FALLBACK);
        };
        img.src = url;
    }

    /**
     * Try to load a cape texture. Shows the cape only if the image loads successfully.
     * @param {string} url
     * @private
     */
    _loadCape(url) {
        fetch(url)
            .then(res => res.ok ? res.blob() : null)
            .then(blob => {
                if (!blob) return;
                const objectUrl = URL.createObjectURL(blob);
                const img = new Image();
                img.onload = () => {
                    URL.revokeObjectURL(objectUrl);
                    const canvas = document.createElement("canvas");
                    canvas.width = img.naturalWidth;
                    canvas.height = img.naturalHeight;
                    canvas.getContext("2d").drawImage(img, 0, 0);
                    const texture = new CanvasTexture(canvas);
                    texture.magFilter = NearestFilter;
                    texture.minFilter = NearestFilter;
                    this.playerObject.cape.map = texture;
                    this.playerObject.cape.visible = true;
                };
                img.src = objectUrl;
            })
            .catch(() => {}); // network error — keep cape hidden
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
        if (distance > 1000) value = "med";
        if (distance > 5000) value = "far";

        this.element.setAttribute("distance-data", value);

        const DEG2RAD = Math.PI / 180;
        const yaw   = (this.data.rotation.yaw   || 0) * DEG2RAD;
        const pitch = (this.data.rotation.pitch  || 0) * DEG2RAD;

        // Rotate whole body to face player's yaw direction
        this.playerObject.rotation.y = -yaw;

        // Tilt head with player's pitch (clamped to ±90°)
        if (this.playerObject.skin?.head) {
            this.playerObject.skin.head.rotation.x =
                Math.max(-Math.PI / 2, Math.min(Math.PI / 2, pitch));
        }
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
        let pos = markerData.position || {};
        let rot = markerData.rotation || {};

        if (!this.position.x && !this.position.y && !this.position.z) {
            this.position.set(pos.x || 0, pos.y || 0, pos.z || 0);
            this.data.rotation.pitch = rot.pitch || 0;
            this.data.rotation.yaw   = rot.yaw   || 0;
        } else {
            let startPos = {
                x:     this.position.x,
                y:     this.position.y,
                z:     this.position.z,
                pitch: this.data.rotation.pitch,
                yaw:   this.data.rotation.yaw,
            };
            let deltaPos = {
                x:     (pos.x || 0) - startPos.x,
                y:     (pos.y || 0) - startPos.y,
                z:     (pos.z || 0) - startPos.z,
                pitch: (rot.pitch || 0) - startPos.pitch,
                yaw:   (rot.yaw   || 0) - startPos.yaw,
            };
            while (deltaPos.yaw >  180) deltaPos.yaw -= 360;
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
                    this.data.rotation.yaw   = startPos.yaw   + deltaPos.yaw   * ease || 0;
                }, 1000);
            }
        }

        let name = markerData.name || this.data.playerUuid;
        this.data.name = name;
        if (this.playerNameElement.innerHTML !== name)
            this.playerNameElement.innerHTML = name;

        this.data.foreign = markerData.foreign;

        // Load cape once when the server confirms the player has one
        if (markerData.hasCape && !this.playerObject.cape.visible && this.data.playerCapeUrl) {
            this._loadCape(this.data.playerCapeUrl);
        }
    }

    dispose() {
        super.dispose();
        let element = this.elementObject.element;
        if (element.parentNode) element.parentNode.removeChild(element);
    }

}
