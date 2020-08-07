import MarkerSet from "./MarkerSet";
import $ from "jquery";
import ToggleButton from "../ui/ToggleButton";
import Label from "../ui/Label";
import {cachePreventionNr} from "../utils";
import PlayerMarkerSet from "./PlayerMarkerSet";

export default class MarkerManager {

	constructor(blueMap, ui) {
		this.blueMap = blueMap;
		this.ui = ui;

		this.markerData = null;
		this.liveData = null;
		this.markerSets = [];

		this.playerMarkerSet = null;

		this.readyPromise =
			Promise.all([
					this.loadMarkerData()
						.catch(ignore => {
							if (this.blueMap.debugInfo) console.debug("Failed load markers:", ignore);
						}),
					this.checkLiveAPI()
						.then(this.initializePlayerMarkers)
				])
				.then(this.loadMarkers)
				.then(this.updatePlayerMarkerLoop);

		$(document).on('bluemap-map-change', this.onBlueMapMapChange);
	}

	loadMarkerData() {
		return new Promise((resolve, reject) => {
			this.blueMap.fileLoader.load(this.blueMap.dataRoot + 'markers.json?' + cachePreventionNr(),
				markerData => {
					try {
						this.markerData = JSON.parse(markerData);
						resolve();
					} catch (e){
						reject(e);
					}
				},
				xhr => {},
				error => {
					reject();
				}
			);
		});
	}

	checkLiveAPI() {
		return new Promise((resolve, reject) => {
			this.blueMap.fileLoader.load(this.blueMap.liveApiRoot + 'players?' + cachePreventionNr(),
				liveData => {
					try {
						this.liveData = JSON.parse(liveData);
						resolve();
					} catch (e){
						reject(e);
					}
				},
				xhr => {},
				error => {
					reject();
				}
			);
		});
	}

	loadMarkers = () => {
		if (this.markerData && this.markerData.markerSets) {
			this.markerData.markerSets.forEach(setData => {
				this.markerSets.push(new MarkerSet(this.blueMap, setData));
			});
		}
	};

	initializePlayerMarkers = () => {
		if (this.liveData){
			this.playerMarkerSet = new PlayerMarkerSet(this.blueMap);
			this.markerSets.push(this.playerMarkerSet);
		}
	};

	update(){
		this.markerSets.forEach(markerSet => {
			markerSet.update();
		});
	}

	updatePlayerMarkerLoop = () => {
		if (this.playerMarkerSet){
			this.playerMarkerSet.updateLive();
		}

		setTimeout(this.updatePlayerMarkerLoop, 2000);
	};

	addMenuElements(menu){
		let addedLabel = false;
		this.markerSets.forEach(markerSet => {
			if (markerSet.toggleable) {
				if (!addedLabel){
					menu.addElement(new Label("marker:"));
					addedLabel = true;
				}

				let menuElement = new ToggleButton(markerSet.label, !markerSet.defaultHide, button => {
					markerSet.visible = button.isSelected();
					markerSet.update();
				});

				markerSet.visible = !markerSet.defaultHide;
				markerSet.update();

				menu.addElement(menuElement);
			}
		});
	}

	onBlueMapMapChange = async () => {
		await this.readyPromise;

		this.update();
	};

}