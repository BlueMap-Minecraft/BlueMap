import {
	Vector2,
	Shape,
	MeshBasicMaterial,
	Mesh,
	Line,
	LineBasicMaterial,
	BufferGeometry,
	ShapeBufferGeometry,
	DoubleSide
} from 'three';
import Marker from "./Marker";
import $ from "jquery";

export default class ShapeMarker extends Marker {

	constructor(blueMap, markerSet, markerData) {
		super(blueMap, markerSet, markerData);

		let points = [];
		if (Array.isArray(markerData.shape)) {
			markerData.shape.forEach(point => {
				points.push(new Vector2(point.x, point.z));
			});
		}
		this.height = markerData.height ? markerData.height : 128;

		this.fillColor = this.prepareColor(markerData.fillColor);
		this.borderColor = this.prepareColor(markerData.borderColor);

		//fill
		let shape = new Shape(points);
		let fillGeo = new ShapeBufferGeometry(shape, 1);
		fillGeo.rotateX(Math.PI * 0.5);
		fillGeo.translate(0, this.height + 0.0072, 0);
		let fillMaterial = new MeshBasicMaterial({
			color: this.fillColor.rgb,
			opacity: this.fillColor.a,
			transparent: true,
			side: DoubleSide,
		});
		let fill = new Mesh( fillGeo, fillMaterial );

		//border
		points.push(points[0]);
		let lineGeo = new BufferGeometry().setFromPoints(points);
		lineGeo.rotateX(Math.PI * 0.5);
		lineGeo.translate(0, this.height + 0.0072, 0);
		let lineMaterial = new LineBasicMaterial({
			color: this.borderColor.rgb,
			opacity: this.borderColor.a,
			transparent: true,
			depthTest: false,
		});
		let line = new Line( lineGeo, lineMaterial );

		this.renderObject = fill;
		fill.add(line);

		this.renderObject.userData = {
			marker: this,
		};

	}

	setVisible(visible){
		super.setVisible(visible);

		if (this.visible) {
			this.blueMap.shapeScene.add(this.renderObject);
			$(document).on('bluemap-update-frame', this.onRender);
		} else {
			this.blueMap.shapeScene.remove(this.renderObject);
			$(document).off('bluemap-update-frame', this.onRender);
		}
	}

	onRender = () => {
		this.updateRenderObject(this.renderObject, this.blueMap.shapeScene, this.blueMap.camera);
	};

	onClick = (clickPos) => {
		if (this.label) {
			this.blueMap.ui.hudInfo.showInfoBubble(this.label, clickPos.x, clickPos.y, clickPos.z);
		}

		if (this.link){
			if (this.newTab){
				window.open(this.link, '_blank');
			} else {
				location.href = this.link;
			}
		}
	};

	prepareColor(color){
		if (color.r === undefined) color.r = 0;
		if (color.g === undefined) color.g = 0;
		if (color.b === undefined) color.b = 0;
		if (color.a === undefined) color.a = 1;

		color.rgb = (color.r << 16) + (color.g << 8) + (color.b);
		return color;
	}

}