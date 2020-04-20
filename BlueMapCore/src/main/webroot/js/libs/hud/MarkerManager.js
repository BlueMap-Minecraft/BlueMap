import MarkerSet from "./MarkerSet";
import $ from "jquery";
import ToggleButton from "../ui/ToggleButton";
import Label from "../ui/Label";

export default class MarkerManager {

	constructor(blueMap, ui) {
		this.blueMap = blueMap;
		this.ui = ui;

		this.markerSets = [];

		this.readyPromise =
			this.loadMarkerData()
				.catch(ignore => {})
				.then(this.loadMarkers);

		$(document).on('bluemap-map-change', this.onBlueMapMapChange);
	}

	loadMarkerData() {
		return new Promise((resolve, reject) => {
			this.blueMap.fileLoader.load(this.blueMap.dataRoot + 'markers.json',
				markerData => {
					this.markerData = JSON.parse(markerData);
					resolve();
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

	update(){
		this.markerSets.forEach(markerSet => {
			markerSet.update();
		});
	}

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