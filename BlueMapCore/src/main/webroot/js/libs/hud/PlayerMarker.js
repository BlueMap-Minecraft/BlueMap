import $ from 'jquery';
import Marker from "./Marker";
import {CSS2DObject} from "./CSS2DRenderer";

export default class PlayerMarker extends Marker {

	constructor(blueMap, markerSet, markerData, playerUuid, worldUuid) {
		super(blueMap, markerSet, markerData);

		this.online = false;
		this.player = playerUuid;
		this.world = worldUuid;

		this.animationRunning = false;
		this.lastFrame = -1;
	}

	setVisible(visible){
		this.visible = visible && this.online && this.world === this.blueMap.settings.maps[this.blueMap.map].world;

		this.blueMap.updateFrame = true;

		if (!this.renderObject){
			let iconElement = $(`<div class="marker-player"><img src="assets/playerheads/${this.player}.png" onerror="this.onerror=null;this.src='assets/playerheads/steve.png';"><div class="nameplate">${this.label}</div></div>`);
			iconElement.find("img").click(this.onClick);

			this.renderObject = new CSS2DObject(iconElement[0]);
			this.renderObject.position.copy(this.position);
			this.renderObject.onBeforeRender = (renderer, scene, camera) => {
				let distanceSquared = this.position.distanceToSquared(camera.position);
				if (distanceSquared > 1000000) {
					iconElement.addClass("distant");
				} else {
					iconElement.removeClass("distant");
				}

				this.updateRenderObject(this.renderObject, scene, camera);
			};
		}

		if (this.visible) {
			this.blueMap.hudScene.add(this.renderObject);
		} else {
			this.blueMap.hudScene.remove(this.renderObject);
		}
	}

	updatePosition = () => {
		if (this.renderObject && !this.renderObject.position.equals(this.position)) {
			if (this.visible) {
				if (!this.animationRunning) {
					this.animationRunning = true;
					requestAnimationFrame(this.moveAnimation);
				}
			} else {
				this.renderObject.position.copy(this.position);
			}
		}
	};

	moveAnimation = (time) => {
		let delta = time - this.lastFrame;
		if (this.lastFrame === -1){
			delta = 20;
		}
		this.lastFrame = time;

		if (this.renderObject && !this.renderObject.position.equals(this.position)) {
			this.renderObject.position.x += (this.position.x - this.renderObject.position.x) * 0.01 * delta;
			this.renderObject.position.y += (this.position.y - this.renderObject.position.y) * 0.01 * delta;
			this.renderObject.position.z += (this.position.z - this.renderObject.position.z) * 0.01 * delta;

			if (this.renderObject.position.distanceToSquared(this.position) < 0.001) {
				this.renderObject.position.copy(this.position);
			}

			this.blueMap.updateFrame = true;

			requestAnimationFrame(this.moveAnimation);
		} else {
			this.animationRunning = false;
			this.lastFrame = -1;
		}
	};

	onClick = () => {

	}

}