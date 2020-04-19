import $ from 'jquery';
import {
	Raycaster,
	Vector2,
	BoxBufferGeometry,
	Mesh,
	MeshBasicMaterial
} from 'three';
import {CSS2DObject} from './CSS2DRenderer';
import {pathFromCoords} from "../utils";

export default class HudInfo {

	constructor(blueMap){
		this.blueMap = blueMap;

		let blockMarkerGeo = new BoxBufferGeometry( 1.01, 1.01, 1.01 );
		blockMarkerGeo.translate(0.5, 0.5, 0.5);
		this.blockMarker = new Mesh(blockMarkerGeo, new MeshBasicMaterial( {
			color: 0xffffff,
			opacity: 0.5,
			depthWrite: false,
			transparent: true,
		} ));

		this.rayPosition = new Vector2();
		this.raycaster = new Raycaster();

		this.element = $(`
			<div class="hud-info"><div class="bubble" style="display: none">
				<div class="content"></div>
			</div></div>
		`);
		this.bubble = this.element.find(".bubble");

		this.hudElement = new CSS2DObject(this.element[0]);

		$(document).on('bluemap-info-click', this.onShowInfo);
		$(window).on('mousedown wheel touchstart', this.onHideInfo);
	}

	showInfoBubble(content, x, y, z, onClose) {
		if (this.onClose){
			this.onClose();
			this.onClose = undefined;
		}

		this.bubble.hide();
		this.bubble.find(".content").html(content);

		this.hudElement.position.set(x, y, z);
		this.bubble.stop();
		this.blueMap.hudScene.add(this.hudElement);
		this.bubble.fadeIn(200);

		this.onClose = onClose;

		this.blueMap.updateFrame = true;
	}

	onShowInfo = event => {
		this.rayPosition.x = ( event.pos.x / this.blueMap.element.offsetWidth ) * 2 - 1;
		this.rayPosition.y = - ( event.pos.y / this.blueMap.element.offsetHeight ) * 2 + 1;

		this.raycaster.setFromCamera(this.rayPosition, this.blueMap.camera);

		//check markers first
		let intersects = this.raycaster.intersectObjects( this.blueMap.shapeScene.children );
		console.log(intersects);
		if (intersects.length !== 0){
			try {
				intersects[0].object.userData.marker.onClick(intersects[0].point);
			} catch (ignore) {}
			return;
		}

		//then show position info
		let hiresData = true;
		intersects = this.raycaster.intersectObjects( this.blueMap.hiresScene.children );
		if (intersects.length === 0){
			hiresData = false;
			intersects = this.raycaster.intersectObjects( this.blueMap.lowresScene.children );
		}

		if (intersects.length > 0) {
			let content = $("<div></div>");

			if (this.blueMap.debugInfo){
				console.debug("Tapped position data: ", intersects[0]);
			}

			//clicked position
			let point = intersects[0].point;
			let normal = intersects[0].face.normal;
			let block = {
				x: Math.floor(point.x - normal.x * 0.001),
				y: Math.floor(point.y - normal.y * 0.001),
				z: Math.floor(point.z - normal.z * 0.001),
			};
			if (hiresData) {
				$(`
					<div class="label">block:</div>
					<div class="coords block">
						<div class="coord"><span class="label">x</span><span class="value">${block.x}</span></div>
						<div class="coord"><span class="label">y</span><span class="value">${block.y}</span></div>
						<div class="coord"><span class="label">z</span><span class="value">${block.z}</span></div>
					</div>
				`).appendTo(content);
			} else {
				$(`
					<div class="label">position:</div>
					<div class="coords block">
						<div class="coord"><span class="label">x</span><span class="value">${block.x}</span></div>
						<div class="coord"><span class="label">z</span><span class="value">${block.z}</span></div>
					</div>
				`).appendTo(content);
			}

			//find light-data
			if (hiresData) {
				let vecIndex = intersects[0].face.a;
				let attributes = intersects[0].object.geometry.attributes;
				let sunlight = attributes.sunlight.array[vecIndex * attributes.sunlight.itemSize];
				let blocklight = attributes.blocklight.array[vecIndex * attributes.blocklight.itemSize];

				$(`
					<div class="label" data-show="light">light:</div>
					<div class="coords block">
						<div class="coord"><span class="label">sun</span><span class="value">${sunlight}</span></div>
						<div class="coord"><span class="label">block</span><span class="value">${blocklight}</span></div>
					</div>
				`).appendTo(content);
			}

			if (this.blueMap.debugInfo) {
				//hires tile path
				let hiresTileSize = this.blueMap.settings.maps[this.blueMap.map]['hires']['tileSize'];
				hiresTileSize.y = hiresTileSize.z;
				let hiresTileOffset = this.blueMap.settings.maps[this.blueMap.map]['hires']['translate'];
				hiresTileOffset.y = hiresTileOffset.z;
				let hiresTile = new Vector2(block.x, block.z).sub(hiresTileOffset).divide(hiresTileSize).floor();
				let hrpath = this.blueMap.dataRoot + this.blueMap.map + '/hires/';
				hrpath += pathFromCoords(hiresTile.x, hiresTile.y);
				hrpath += '.json';

				//lowres tile path
				let lowresTileSize = this.blueMap.settings.maps[this.blueMap.map]['lowres']['tileSize'];
				lowresTileSize.y = lowresTileSize.z;
				let lowresTileOffset = this.blueMap.settings.maps[this.blueMap.map]['lowres']['translate'];
				lowresTileOffset.y = lowresTileOffset.z;
				let lowresTile = new Vector2(block.x, block.z).sub(lowresTileOffset).divide(lowresTileSize).floor();
				let lrpath = this.blueMap.dataRoot + this.blueMap.map + '/lowres/';
				lrpath += pathFromCoords(lowresTile.x, lowresTile.y);
				lrpath += '.json';
				$(`
					<div class="files">
						<span class="value">${hrpath}</span><br>
						<span class="value">${lrpath}</span>
					</div>
				`).appendTo(content);
			}

			//add block marker
			if (hiresData){
				this.blockMarker.position.set(block.x, block.y, block.z);
				this.blueMap.hiresScene.add(this.blockMarker);
				this.blockMarker.needsUpdate = true;
			}

			this.showInfoBubble(content.html(), block.x + 0.5, block.y + 1, block.z + 0.5);
		}

	};

	onHideInfo = event => {
		if (!this.bubble.is(':animated')) {
			this.bubble.fadeOut(200, () => {
				this.blueMap.hudScene.remove(this.hudElement);

				if (this.onClose){
					this.onClose();
					this.onClose = undefined;
				}
			});
			this.blueMap.hiresScene.remove(this.blockMarker);
			this.blueMap.updateFrame = true;
		}
	};

};
