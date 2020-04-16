import $ from 'jquery';
import Marker from "./Marker";
import {CSS2DObject} from "./CSS2DRenderer";
import {Vector3} from "three";

import POI from "../../../assets/poi.svg";

export default class POIMarker extends Marker {

	constructor(blueMap, markerSet, markerData) {
		super(blueMap, markerSet, markerData);

		this.icon = markerData.icon ? markerData.icon : POI;
		this.iconAnchor = {
			x: markerData.iconAnchor.x,
			y: markerData.iconAnchor.y
		};

		this.position = new Vector3(markerData.position.x, markerData.position.y, markerData.position.z);
	}

	setVisible(visible){
		super.setVisible(visible);

		if (!this.renderObject){
			let iconElement = $(`<div class="marker-poi"><img src="${this.icon}" style="transform: translate(50%, 50%) translate(${-this.iconAnchor.x}px, ${-this.iconAnchor.y}px)"></div>`);
			iconElement.find("img").click(this.onClick);
			this.renderObject = new CSS2DObject(iconElement[0]);
			this.renderObject.position.copy(this.position);
			this.renderObject.onBeforeRender = (renderer, scene, camera) => this.updateRenderObject(this.renderObject, scene, camera);
		}

		if (this.visible) {
			this.blueMap.hudScene.add(this.renderObject);
		} else {
			this.blueMap.hudScene.remove(this.renderObject);
		}
	}

	onClick = () => {
		if (this.label) {
			this.setVisible(false);
			this.blueMap.ui.hudInfo.showInfoBubble(this.label, this.position.x, this.position.y, this.position.z, () => {
				this.setVisible(this.markerSet.visible);
			});
		}

		if (this.link){
			if (this.newTab){
				window.open(this.link, '_blank');
			} else {
				location.href = this.link;
			}
		}
	}

}