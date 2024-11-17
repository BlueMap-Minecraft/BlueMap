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
import "./BlueMap";
import {MapViewer} from "./MapViewer";
import {MapControls} from "./controls/map/MapControls";
import {FreeFlightControls} from "./controls/freeflight/FreeFlightControls";
import {FileLoader, MathUtils, Vector3} from "three";
import {Map as BlueMapMap} from "./map/Map";
import {alert, animate, EasingFunctions, generateCacheHash} from "./util/Utils";
import {MainMenu} from "./MainMenu";
import {PopupMarker} from "./PopupMarker";
import {MarkerSet} from "./markers/MarkerSet";
import {getLocalStorage, round, setLocalStorage} from "./Utils";
import {i18n, setLanguage} from "../i18n";
import {PlayerMarkerManager} from "./markers/PlayerMarkerManager";
import {NormalMarkerManager} from "./markers/NormalMarkerManager";
import {reactive} from "vue";

export class BlueMapApp {

    /**
     * @param rootElement {Element}
     */
    constructor(rootElement) {
        this.events = rootElement;

        this.mapViewer = new MapViewer(rootElement, this.events);

        this.mapControls = new MapControls(this.mapViewer.renderer.domElement, rootElement);
        this.freeFlightControls = new FreeFlightControls(this.mapViewer.renderer.domElement);

        /** @type {PlayerMarkerManager} */
        this.playerMarkerManager = null;
        /** @type {NormalMarkerManager} */
        this.markerFileManager = null;

        /** @type {{
         *      version: string,
         *      useCookies: boolean,
         *      defaultToFlatView: boolean,
         *      resolutionDefault: number,
         *      minZoomDistance: number,
         *      maxZoomDistance: number,
         *      hiresSliderMax: number,
         *      hiresSliderDefault: number,
         *      hiresSliderMin: number,
         *      lowresSliderMax: number,
         *      lowresSliderDefault: number,
         *      lowresSliderMin: number,
         *      startLocation: string,
         *      mapDataRoot: string,
         *      liveDataRoot: string,
         *      maps: string[],
         *      scripts: string[],
         *      styles: string[]
         *  }}
         **/
        this.settings = null;
        this.savedUserSettings = new Map();

        /** @type BlueMapMap[] */
        this.maps = [];
        /** @type Map<BlueMapMap> */
        this.mapsMap = new Map();

        this.lastCameraMove = 0;

        this.mainMenu = reactive(new MainMenu());

        this.appState = reactive({
            controls: {
                state: "perspective",
                mouseSensitivity: 1,
                showZoomButtons: true,
                invertMouse: false,
                pauseTileLoading: false
            },
            menu: this.mainMenu,
            maps: [],
            theme: null,
            screenshot: {
                clipboard: true
            },
            debug: false
        });

        // init
        this.updateControlsSettings();
        this.initGeneralEvents();

        // popup on click
        this.popupMarkerSet = new MarkerSet("bm-popup-set");
        this.popupMarkerSet.data.toggleable = false;
        this.popupMarker = new PopupMarker("bm-popup", this.appState, this.events);
        this.popupMarkerSet.add(this.popupMarker);
        this.mapViewer.markers.add(this.popupMarkerSet);

        this.updateLoop = null;

        this.hashUpdateTimeout = null;
        this.viewAnimation = null;
    }


    /**
     * @returns {Promise<void|never>}
     */
    async load() {
        let oldMaps = this.maps;
        this.maps = [];
        this.appState.maps.splice(0, this.appState.maps.length);
        this.mapsMap.clear();

        // load settings
        await this.getSettings();
        this.mapControls.minDistance = this.settings.minZoomDistance;
        this.mapControls.maxDistance = this.settings.maxZoomDistance;

        // load settings-styles
        if (this.settings.styles) for (let styleUrl of this.settings.styles) {
            let styleElement = document.createElement("link");
            styleElement.rel = "stylesheet";
            styleElement.href = styleUrl;
            alert(this.events, "Loading style: " + styleUrl, "fine");
            document.head.appendChild(styleElement);
        }

        // unload loaded maps
        await this.mapViewer.switchMap(null);
        oldMaps.forEach(map => map.dispose());

        // load user settings
        await this.loadUserSettings();

        // load maps
        this.maps = await this.loadMaps();
        for (let map of this.maps) {
            this.mapsMap.set(map.data.id, map);
            this.appState.maps.push(map.data);
        }

        // switch to map
        try {
            if (!await this.loadPageAddress()) {
                if (this.maps.length > 0) await this.switchMap(this.maps[0].data.id);
                this.resetCamera();
            }
        } catch (e) {
            console.error("Failed to load map!", e);
        }

        // map position address
        window.addEventListener("hashchange", this.loadPageAddress);
        this.events.addEventListener("bluemapCameraMoved", this.cameraMoved);
        this.events.addEventListener("bluemapMapInteraction", this.mapInteraction);

        // start app update loop
        if(this.updateLoop) clearTimeout(this.updateLoop);
        this.updateLoop = setTimeout(this.update, 1000);

        // save user settings
        this.saveUserSettings();

        // load settings-scripts
        if (this.settings.scripts) for (let scriptUrl of this.settings.scripts) {
            let scriptElement = document.createElement("script");
            scriptElement.src = scriptUrl;
            alert(this.events, "Loading script: " + scriptUrl, "fine");
            document.body.appendChild(scriptElement);
        }
    }

    update = async () => {
        await this.followPlayerMarkerWorld();
        this.updateLoop = setTimeout(this.update, 1000);
    }

    async followPlayerMarkerWorld() {
        /** @type {PlayerLike} */
        let player = this.mapViewer.controlsManager.controls?.data.followingPlayer;

        if (this.mapViewer.map && player) {
            if (player.foreign){

                let matchingMap = await this.findPlayerMap(player.playerUuid)
                if (matchingMap) {
                    this.mainMenu.closeAll();
                    await this.switchMap(matchingMap.data.id, false);
                    let playerMarker = this.playerMarkerManager.getPlayerMarker(player.playerUuid);
                    if (playerMarker && this.mapViewer.controlsManager.controls.followPlayerMarker)
                        this.mapViewer.controlsManager.controls.followPlayerMarker(playerMarker);
                } else {
                    if (this.mapViewer.controlsManager.controls.stopFollowingPlayerMarker)
                        this.mapViewer.controlsManager.controls.stopFollowingPlayerMarker();
                }
            }
        }
    }

    async findPlayerMap(playerUuid) {
        /** @type BlueMapMap */
        let matchingMap = null;

        // search for the map that contains the player
        if (this.maps.length < 20) {
            for (let map of this.maps) {
                let playerData = await this.loadPlayerData(map);
                if (!Array.isArray(playerData.players)) continue;
                for (let p of playerData.players) {
                    if (p.uuid === playerUuid && !p.foreign) {
                        matchingMap = map;
                        break;
                    }
                }

                if (matchingMap) break;
            }
        }

        return matchingMap;
    }

    /**
     * @param mapId {String}
     * @param resetCamera {boolean}
     * @returns {Promise<void>}
     */
    async switchMap(mapId, resetCamera = true) {
        let map = this.mapsMap.get(mapId);
        if (!map) return Promise.reject(`There is no map with the id "${mapId}" loaded!`);

        if (this.playerMarkerManager) this.playerMarkerManager.dispose();
        if (this.markerFileManager) this.markerFileManager.dispose();

        await this.mapViewer.switchMap(map)

        if (resetCamera || !this.mapViewer.map.hasView(this.appState.controls.state))
            this.resetCamera();

        this.updatePageAddress();

        await Promise.all([
            this.initPlayerMarkerManager(),
            this.initMarkerFileManager()
        ]);
    }

    resetCamera() {
        let map = this.mapViewer.map;
        let controls = this.mapViewer.controlsManager;

        if (map) {
            controls.position.set(map.data.startPos.x, 0, map.data.startPos.z);
            controls.distance = 1500;
            controls.angle = 0;
            controls.rotation = 0;
            controls.tilt = 0;
            controls.ortho = 0;
        }

        controls.controls = this.mapControls;
        this.appState.controls.state = "perspective";

        if (this.settings.defaultToFlatView && map.hasView("flat")) {
            this.setFlatView();
        }

        else if (!map.hasView("perspective")) {
            if (map.hasView("flat"))
                this.setFlatView();
            else
                this.setFreeFlight();
        }

        this.updatePageAddress();
    }

    /**
     * @returns Promise<BlueMapMap[]>
     */
    async loadMaps() {
        let settings = this.settings;
        let maps = [];

        // create maps
        if (settings.maps !== undefined){
            let loadingPromises = settings.maps.map(mapId => {
                let map = new BlueMapMap(mapId, settings.mapDataRoot + "/" + mapId + "/", settings.liveDataRoot + "/" + mapId + "/", this.loadBlocker, this.mapViewer.events);
                maps.push(map);

                return map.loadSettings(this.mapViewer.tileCacheHash)
                    .catch(error => {
                        alert(this.events, `Failed to load settings for map '${map.data.id}':` + error, "warning");
                    });
            })

            await Promise.all(loadingPromises);
        }

        // sort maps
        maps.sort((map1, map2) => {
            let sort = map1.data.sorting - map2.data.sorting;
            if (isNaN(sort)) return 0;
            return sort;
        });

        return maps;
    }

    async getSettings() {
        if (!this.settings){
            let loaded = await this.loadSettings();
            this.settings = {
                version: "?",
                useCookies: false,
                defaultToFlatView: false,
                resolutionDefault: 1.0,
                minZoomDistance: 5,
                maxZoomDistance: 100000,
                hiresSliderMax: 500,
                hiresSliderDefault: 100,
                hiresSliderMin: 0,
                lowresSliderMax: 7000,
                lowresSliderDefault: 2000,
                lowresSliderMin: 500,
                mapDataRoot: "maps",
                liveDataRoot: "maps",
                maps: [
                    "world",
                    "world_the_end",
                    "world_nether"
                ],
                scripts: [],
                styles: [],
                ...loaded
            };
        }

        return this.settings;
    }

    /**
     * @returns {Promise<Object>}
     */
    loadSettings() {
        return new Promise((resolve, reject) => {
            let loader = new FileLoader();
            loader.setResponseType("json");
            loader.load("settings.json?" + generateCacheHash(),
                resolve,
                () => {},
                () => reject("Failed to load the settings.json!")
            );
        });
    }

    /**
     * @param map {BlueMapMap}
     * @returns {Promise<Object>}
     */
    loadPlayerData(map) {
        return new Promise((resolve, reject) => {
            let loader = new FileLoader();
            loader.setResponseType("json");
            loader.load(map.data.liveDataRoot + "/live/players.json?" + generateCacheHash(),
                fileData => {
                    if (!fileData) reject(`Failed to parse '${this.fileUrl}'!`);
                    else resolve(fileData);
                },
                () => {},
                () => reject(`Failed to load '${this.fileUrl}'!`)
            )
        });
    }

    initPlayerMarkerManager() {
        if (this.playerMarkerManager)
            this.playerMarkerManager.dispose()

        const map = this.mapViewer.map;
        if (!map) return;

        this.playerMarkerManager = new PlayerMarkerManager(
            this.mapViewer.markers,
            map.data.liveDataRoot + "/live/players.json",
            map.data.mapDataRoot + "/assets/playerheads/",
            this.events
        );
        this.playerMarkerManager.setAutoUpdateInterval(0);
        return this.playerMarkerManager.update()
            .then(() => {
                this.playerMarkerManager.setAutoUpdateInterval(1000);
            })
            .catch(e => {
                alert(this.events, e, "warning");
                this.playerMarkerManager.dispose();
            });
    }

    initMarkerFileManager() {
        if (this.markerFileManager)
            this.markerFileManager.dispose();

        const map = this.mapViewer.map;
        if (!map) return;

        this.markerFileManager = new NormalMarkerManager(this.mapViewer.markers, map.data.liveDataRoot + "/live/markers.json", this.events);
        return this.markerFileManager.update()
            .then(() => {
                this.markerFileManager.setAutoUpdateInterval(1000 * 10);
            })
            .catch(e => {
                alert(this.events, e, "warning");
                this.markerFileManager.dispose();
            });
    }

    updateControlsSettings() {
        let mouseInvert = this.appState.controls.invertMouse ? -1 : 1;

        this.freeFlightControls.mouseRotate.speedCapture = -1.5 * this.appState.controls.mouseSensitivity;
        this.freeFlightControls.mouseAngle.speedCapture = -1.5 * this.appState.controls.mouseSensitivity * mouseInvert;
        this.freeFlightControls.mouseRotate.speedRight = -2 * this.appState.controls.mouseSensitivity;
        this.freeFlightControls.mouseAngle.speedRight = -2 * this.appState.controls.mouseSensitivity * mouseInvert;
    }

    initGeneralEvents() {
        //close menu on fullscreen
        document.addEventListener("fullscreenchange", evt => {
            if (document.fullscreenElement) {
                this.mainMenu.closeAll();
            }
        });
    }

    setPerspectiveView(transition = 0, minDistance = 5) {
        if (!this.mapViewer.map) return;
        if (!this.mapViewer.map.data.perspectiveView) return;
        if (this.viewAnimation) this.viewAnimation.cancel();

        let cm = this.mapViewer.controlsManager;
        cm.controls = null;

        let startDistance = cm.distance;
        let targetDistance = Math.max(5, minDistance, startDistance);

        let startY = cm.position.y;
        let targetY = MathUtils.lerp(this.mapViewer.map.terrainHeightAt(cm.position.x, cm.position.z) + 3, 0, targetDistance / 500);

        let startAngle = cm.angle;
        let targetAngle = Math.min(Math.PI / 2, startAngle, this.mapControls.getMaxPerspectiveAngleForDistance(targetDistance));

        let startOrtho = cm.ortho;
        let startTilt = cm.tilt;

        this.viewAnimation = animate(p => {
            let ep = EasingFunctions.easeInOutQuad(p);
            cm.position.y = MathUtils.lerp(startY, targetY, ep);
            cm.distance = MathUtils.lerp(startDistance, targetDistance, ep);
            cm.angle = MathUtils.lerp(startAngle, targetAngle, ep);
            cm.ortho = MathUtils.lerp(startOrtho, 0, p);
            cm.tilt = MathUtils.lerp(startTilt, 0, ep);
        }, transition, finished => {
            this.mapControls.reset();
            if (finished){
                cm.controls = this.mapControls;
                this.updatePageAddress();
            }
        });

        this.appState.controls.state = "perspective";
    }

    setFlatView(transition = 0, minDistance = 5) {
        if (!this.mapViewer.map) return;
        if (!this.mapViewer.map.data.flatView) return;
        if (this.viewAnimation) this.viewAnimation.cancel();

        let cm = this.mapViewer.controlsManager;
        cm.controls = null;

        let startDistance = cm.distance;
        let targetDistance = Math.max(5, minDistance, startDistance);

        let startRotation = cm.rotation;
        let startAngle = cm.angle;
        let startOrtho = cm.ortho;
        let startTilt = cm.tilt;

        this.viewAnimation = animate(p => {
            let ep = EasingFunctions.easeInOutQuad(p);
            cm.distance = MathUtils.lerp(startDistance, targetDistance, ep);
            cm.rotation = MathUtils.lerp(startRotation, 0, ep);
            cm.angle = MathUtils.lerp(startAngle, 0, ep);
            cm.ortho = MathUtils.lerp(startOrtho, 1, p);
            cm.tilt = MathUtils.lerp(startTilt, 0, ep);
        }, transition, finished => {
            this.mapControls.reset();
            if (finished){
                cm.controls = this.mapControls;
                this.updatePageAddress();
            }
        });

        this.appState.controls.state = "flat";
    }

    setFreeFlight(transition = 0, targetY = undefined) {
        if (!this.mapViewer.map) return;
        if (!this.mapViewer.map.data.freeFlightView) return;
        if (this.viewAnimation) this.viewAnimation.cancel();

        let cm = this.mapViewer.controlsManager;
        cm.controls = null;

        let startDistance = cm.distance;

        let startY = cm.position.y;
        if (!targetY) targetY = this.mapViewer.map.terrainHeightAt(cm.position.x, cm.position.z) + 3 || startY;

        let startAngle = cm.angle;
        let targetAngle = Math.PI / 2;

        let startOrtho = cm.ortho;
        let startTilt = cm.tilt;

        this.viewAnimation = animate(p => {
            let ep = EasingFunctions.easeInOutQuad(p);
            cm.position.y = MathUtils.lerp(startY, targetY, ep);
            cm.distance = MathUtils.lerp(startDistance, 0, ep);
            cm.angle = MathUtils.lerp(startAngle, targetAngle, ep);
            cm.ortho = MathUtils.lerp(startOrtho, 0, Math.min(p * 2, 1));
            cm.tilt = MathUtils.lerp(startTilt, 0, ep);
        }, transition, finished => {
            if (finished){
                cm.controls = this.freeFlightControls;
                this.updatePageAddress();
            }
        });

        this.appState.controls.state = "free";
    }

    setChunkBorders(chunkBorders) {
        this.mapViewer.data.uniforms.chunkBorders.value = chunkBorders;
    }

    setDebug(debug) {
        this.appState.debug = debug;

        if (debug){
            this.mapViewer.stats.showPanel(0);
        } else {
            this.mapViewer.stats.showPanel(-1);
        }
    }

    setTheme(theme) {
        this.appState.theme = theme;

        if (theme === "light") {
            this.mapViewer.rootElement.classList.remove("theme-dark");
            this.mapViewer.rootElement.classList.remove("theme-contrast");
            this.mapViewer.rootElement.classList.add("theme-light");
        }
        else if (theme === "dark") {
            this.mapViewer.rootElement.classList.remove("theme-light");
            this.mapViewer.rootElement.classList.remove("theme-contrast");
            this.mapViewer.rootElement.classList.add("theme-dark");
        }
        else if (theme === "contrast") {
            this.mapViewer.rootElement.classList.remove("theme-light");
            this.mapViewer.rootElement.classList.remove("theme-dark");
            this.mapViewer.rootElement.classList.add("theme-contrast");
        }
        else {
            this.mapViewer.rootElement.classList.remove("theme-light");
            this.mapViewer.rootElement.classList.remove("theme-dark");
            this.mapViewer.rootElement.classList.remove("theme-contrast");
        }
    }

    setScreenshotClipboard(clipboard) {
        this.appState.screenshot.clipboard = clipboard;
    }

    async updateMap() {
        try {
            this.mapViewer.clearTileCache();
            if (this.mapViewer.map) {
                await this.switchMap(this.mapViewer.map.data.id, false);
            }
            this.saveUserSettings();
        } catch (e) {
            alert(this.events, e, "error");
        }
    }

    resetSettings() {
        this.saveUserSetting("resetSettings", true);
        location.reload();
    }

    async loadUserSettings(){
        if (!isNaN(this.settings.resolutionDefault)) this.mapViewer.data.superSampling = this.settings.resolutionDefault;
        if (!isNaN(this.settings.hiresSliderDefault)) this.mapViewer.data.loadedHiresViewDistance = this.settings.hiresSliderDefault;
        if (!isNaN(this.settings.lowresSliderDefault)) this.mapViewer.data.loadedLowresViewDistance = this.settings.lowresSliderDefault;

        if (!this.settings.useCookies) return;

        if (this.loadUserSetting("resetSettings", false)) {
            alert(this.events, "Settings reset!", "info");
            this.saveUserSettings();
            return;
        }

        // Only reuse the user's tile cash hash if the current browser navigation event is not a reload.
        // If it's a reload, we assume the user is troubleshooting and actually wants to refresh the map.
        const [entry] = performance.getEntriesByType("navigation");
        if (entry.type != "reload") {
            this.mapViewer.clearTileCache(this.loadUserSetting("tileCacheHash", this.mapViewer.tileCacheHash));
        }

        this.mapViewer.superSampling = this.loadUserSetting("superSampling", this.mapViewer.data.superSampling);
        this.mapViewer.data.loadedHiresViewDistance = this.loadUserSetting("hiresViewDistance", this.mapViewer.data.loadedHiresViewDistance);
        this.mapViewer.data.loadedLowresViewDistance = this.loadUserSetting("lowresViewDistance", this.mapViewer.data.loadedLowresViewDistance);
        this.mapViewer.updateLoadedMapArea();
        this.appState.controls.mouseSensitivity = this.loadUserSetting("mouseSensitivity", this.appState.controls.mouseSensitivity);
        this.appState.controls.invertMouse = this.loadUserSetting("invertMouse", this.appState.controls.invertMouse);
        this.appState.controls.pauseTileLoading = this.loadUserSetting("pauseTileLoading", this.appState.controls.pauseTileLoading);
        this.appState.controls.showZoomButtons = this.loadUserSetting("showZoomButtons", this.appState.controls.showZoomButtons);
        this.updateControlsSettings();
        this.setTheme(this.loadUserSetting("theme", this.appState.theme));
        this.setScreenshotClipboard(this.loadUserSetting("screenshotClipboard", this.appState.screenshot.clipboard));
        await setLanguage(this.loadUserSetting("lang", i18n.locale.value));
        this.setChunkBorders(this.loadUserSetting("chunkBorders", this.mapViewer.data.uniforms.chunkBorders.value))
        this.setDebug(this.loadUserSetting("debug", this.appState.debug));

        alert(this.events, "Settings loaded!", "info");
    }

    saveUserSettings() {
        if (!this.settings.useCookies) return;

        this.saveUserSetting("resetSettings", false);
        this.saveUserSetting("tileCacheHash", this.mapViewer.tileCacheHash);

        this.saveUserSetting("superSampling", this.mapViewer.data.superSampling);
        this.saveUserSetting("hiresViewDistance", this.mapViewer.data.loadedHiresViewDistance);
        this.saveUserSetting("lowresViewDistance", this.mapViewer.data.loadedLowresViewDistance);
        this.saveUserSetting("mouseSensitivity", this.appState.controls.mouseSensitivity);
        this.saveUserSetting("invertMouse", this.appState.controls.invertMouse);
        this.saveUserSetting("pauseTileLoading", this.appState.controls.pauseTileLoading);
        this.saveUserSetting("showZoomButtons", this.appState.controls.showZoomButtons);
        this.saveUserSetting("theme", this.appState.theme);
        this.saveUserSetting("screenshotClipboard", this.appState.screenshot.clipboard);
        this.saveUserSetting("lang", i18n.locale.value);
        this.saveUserSetting("chunkBorders", this.mapViewer.data.uniforms.chunkBorders.value);
        this.saveUserSetting("debug", this.appState.debug);

        alert(this.events, "Settings saved!", "info");
    }

    loadUserSetting(key, defaultValue){
        let value = getLocalStorage("bluemap-" + key);

        if (value === undefined) return defaultValue;
        return value;
    }

    saveUserSetting(key, value){
        if (this.savedUserSettings.get(key) !== value){
            this.savedUserSettings.set(key, value);
            setLocalStorage("bluemap-" + key, value);
        }
    }

    cameraMoved = () => {
        if (this.hashUpdateTimeout) clearTimeout(this.hashUpdateTimeout);
        this.hashUpdateTimeout = setTimeout(this.updatePageAddress, 1500);
        this.lastCameraMove = Date.now();
    }

    loadBlocker = async () => {
        if (!this.appState.controls.pauseTileLoading) return;

        let timeToWait;
        do {
            let timeSinceLastMove = Date.now() - this.lastCameraMove;
            timeToWait = 250 - timeSinceLastMove;
            if (timeToWait > 0) await this.sleep(timeToWait);
        } while (timeToWait > 0);
    }

    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    updatePageAddress = () => {
        let hash = "#";

        if (this.mapViewer.map) {
            hash += this.mapViewer.map.data.id;

            let controls = this.mapViewer.controlsManager;
            hash += ":" + round(controls.position.x, 0);
            hash += ":" + round(controls.position.y, 0);
            hash += ":" + round(controls.position.z, 0);
            hash += ":" + round(controls.distance, 0);
            hash += ":" + round(controls.rotation, 2);
            hash += ":" + round(controls.angle, 2);
            hash += ":" + round(controls.tilt, 2);
            hash += ":" + round(controls.ortho, 0);
            hash += ":" + this.appState.controls.state;
        }

        history.replaceState(undefined, undefined, hash);

        document.title = i18n.t("pageTitle", {
            map: this.mapViewer.map ? this.mapViewer.map.data.name : "?",
            version: this.settings.version
        });
    }

    loadPageAddress = async () => {
        let hash = window.location.hash?.substring(1) || this.settings.startLocation || "";
        let values = hash.split(":");

        // only world is provided
        if (values.length === 1 && (!this.mapViewer.map || this.mapViewer.map.data.id !== values[0])) {
            try {
                await this.switchMap(values[0]);
            } catch (e) {
                return false;
            }

            return true;
        }

        // load full location
        if (values.length !== 10) return false;

        let controls = this.mapViewer.controlsManager;
        controls.controls = null;

        if (!this.mapViewer.map || this.mapViewer.map.data.id !== values[0]) {
            try {
                await this.switchMap(values[0]);
            } catch (e) {
                return false;
            }
        }

        switch (values[9]) {
            case "flat" : this.setFlatView(0); break;
            case "free" : this.setFreeFlight(0, controls.position.y); break;
            default : this.setPerspectiveView(0); break;
        }

        controls.position.x = parseFloat(values[1]);
        controls.position.y = parseFloat(values[2]);
        controls.position.z = parseFloat(values[3]);
        controls.distance = parseFloat(values[4]);
        controls.rotation = parseFloat(values[5]);
        controls.angle = parseFloat(values[6]);
        controls.tilt = parseFloat(values[7]);
        controls.ortho = parseFloat(values[8]);

        this.updatePageAddress();
        this.mapViewer.updateLoadedMapArea();

        return true;
    }

    mapInteraction = event => {
        if (event.detail.data.doubleTap) {
            let cm = this.mapViewer.controlsManager;
            let pos = event.detail.hit?.point || event.detail.object?.getWorldPosition(new Vector3());
            if (!pos) return;

            let startDistance = cm.distance;
            let targetDistance = Math.max(startDistance * 0.25, 5);

            let startX = cm.position.x;
            let targetX = pos.x;

            let startZ = cm.position.z;
            let targetZ = pos.z;

            this.viewAnimation = animate(p => {
                let ep = EasingFunctions.easeInOutQuad(p);
                cm.distance = MathUtils.lerp(startDistance, targetDistance, ep);
                cm.position.x = MathUtils.lerp(startX, targetX, ep);
                cm.position.z = MathUtils.lerp(startZ, targetZ, ep);
            }, 500);
        }
    }

    takeScreenshot = () => {
        let link = document.createElement("a");
        link.download = "bluemap-screenshot.png";
        link.href = this.mapViewer.renderer.domElement.toDataURL('image/png');
        link.click();

        if (this.appState.screenshot.clipboard) {
            this.mapViewer.renderer.domElement.toBlob(blob => {
                // eslint-disable-next-line no-undef
                navigator.clipboard.write([new ClipboardItem({ ['image/png']: blob })]).catch(e => {
                    alert(this.events, "Failed to copy screenshot to clipboard: " + e, "error");
                });
            });
        }
    }

}
