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
import { Object3D } from "three";

export * as Three from "three";

export * from "./controls/freeflight/FreeFlightControls";
export * from "./controls/freeflight/keyboard/KeyHeightControls";
// class name conflicts with map controls
export { KeyMoveControls as FreeFlightKeyMoveControls } from "./controls/freeflight/keyboard/KeyMoveControls";
export { MouseAngleControls as FreeFlightMouseAngleControls } from "./controls/freeflight/mouse/MouseAngleControls";
export { MouseRotateControls as FreeFlightMouseRotateControls } from "./controls/freeflight/mouse/MouseRotateControls";
export * from "./controls/freeflight/touch/TouchPanControls";

export * from "./controls/map/MapControls";
export * from "./controls/map/MapHeightControls";
export * from "./controls/map/keyboard/KeyAngleControls";
export { KeyMoveControls as MapKeyMoveControls } from "./controls/map/keyboard/KeyMoveControls";
export * from "./controls/map/keyboard/KeyRotateControls";
export * from "./controls/map/keyboard/KeyZoomControls";
export { MouseAngleControls as MapMouseAngleControls } from "./controls/map/mouse/MouseAngleControls";
export * from "./controls/map/mouse/MouseMoveControls";
export { MouseRotateControls as MapMouseRotateControls } from "./controls/map/mouse/MouseRotateControls";
export * from "./controls/map/mouse/MouseZoomControls";
export * from "./controls/map/touch/TouchAngleControls";
export * from "./controls/map/touch/TouchMoveControls";
export * from "./controls/map/touch/TouchRotateControls";
export * from "./controls/map/touch/TouchZoomControls";

export * from "./controls/ControlsManager";
export * from "./controls/KeyCombination";

export * from "./map/LowresTileLoader";
export * from "./map/Map";
export * from "./map/Tile";
export * from "./map/TileLoader";
export * from "./map/TileManager";
export * from "./map/TileMap";
export * from "./map/hires/HiresFragmentShader";
export * from "./map/hires/HiresVertexShader";
export * from "./map/lowres/LowresFragmentShader";
export * from "./map/lowres/LowresVertexShader";

export * from "./markers/ExtrudeMarker";
export * from "./markers/HtmlMarker";
export * from "./markers/LineMarker";
export * from "./markers/Marker";
export * from "./markers/MarkerFillFragmentShader";
export * from "./markers/MarkerFillVertexShader";
export * from "./markers/MarkerManager";
export * from "./markers/MarkerSet";
export * from "./markers/NormalMarkerManager";
export * from "./markers/ObjectMarker";
export * from "./markers/PlayerMarker";
export * from "./markers/PlayerMarkerManager";
export * from "./markers/PlayerMarkerSet";
export * from "./markers/PoiMarker";
export * from "./markers/ShapeMarker";

export * from "./skybox/SkyFragmentShader";
export * from "./skybox/SkyVertexShader";
export * from "./skybox/SkyboxScene";

export * from "./util/CSS2DRenderer";
export * from "./util/CombinedCamera";
export * from "./util/LineShader";
export * from "./util/Stats";
export * from "./util/Utils";

export * from "./BlueMapApp";
export * from "./MainMenu";
export * from "./MapViewer";
export * from "./PopupMarker";
export * from "./Utils";

/**
 * @param event {object}
 * @return {boolean} - whether the event has been consumed (true) or not (false)
 */
Object3D.prototype.onClick = function (event) {
    if (this.parent) {
        if (!Array.isArray(event.eventStack)) event.eventStack = [];
        event.eventStack.push(this);

        return this.parent.onClick(event);
    }

    return false;
};
