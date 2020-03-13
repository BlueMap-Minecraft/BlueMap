import $ from 'jquery';
import {
	Raycaster,
	Vector2
} from 'three';
import {pathFromCoords} from "../utils";

export default class HudInfo {

	constructor(blueMap, container){
		this.blueMap = blueMap;
		this.container = container;

		this.rayPosition = new Vector2();
		this.raycaster = new Raycaster();

		this.element = $(`
			<div class="hud-info" style="display: none">
				<div class="content"></div>
			</div>
		`).appendTo(this.container);

		$(document).on('bluemap-info-click', this.onShowInfo);
		$(window).on('mousedown wheel', this.onHideInfo);
	}

	onShowInfo = event => {
		this.rayPosition.x = ( event.pos.x / this.blueMap.element.offsetWidth ) * 2 - 1;
		this.rayPosition.y = - ( event.pos.y / this.blueMap.element.offsetHeight ) * 2 + 1;

		this.raycaster.setFromCamera(this.rayPosition, this.blueMap.camera);
		let hiresData = true;
		let intersects = this.raycaster.intersectObjects( this.blueMap.hiresScene.children );
		if (intersects.length === 0){
			hiresData = false;
			intersects = this.raycaster.intersectObjects( this.blueMap.lowresScene.children );
		}

		if (intersects.length > 0) {
			this.element.hide();
			let content = this.element.find(".content");
			content.html("");

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

			//display the element
			this.element.css('left', `${event.pos.x}px`);
			this.element.css('top', `${event.pos.y}px`);
			if (event.pos.y < this.blueMap.element.offsetHeight / 3){
				this.element.addClass("below");
			} else {
				this.element.removeClass("below");
			}
			this.element.fadeIn(200);
		}

	};

	onHideInfo = event => {
		if (!this.element.is(':animated')) {
			this.element.fadeOut(200);
		}
	};

};
