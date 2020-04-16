import {
	Vector2,
	Shape,
	ExtrudeBufferGeometry,
	MeshBasicMaterial,
	Mesh,
	Object3D,
	DoubleSide
} from 'three';
import Marker from "./Marker";

export default class ShapeMarker extends Marker {

	constructor(blueMap, markerSet, markerData) {
		super(blueMap, markerSet, markerData);

		let points = [];
		if (Array.isArray(markerData.shape)) {
			markerData.shape.forEach(point => {
				points.push(new Vector2(point.x, point.z));
			});
		}
		this.floor = markerData.floor ? markerData.floor : 0;
		this.ceiling = markerData.ceiling ? markerData.ceiling : 128;

		let shape = new Shape(points);
		let extrude = new ExtrudeBufferGeometry(shape, {
			steps: 1,
			depth: this.ceiling - this.floor,
			bevelEnabled: false
		});
		extrude.rotateX(Math.PI * 0.5);
		extrude.translate(0, this.ceiling, 0);
		let material = new MeshBasicMaterial( {
			color: 0xff0000,
			opacity: 0.25,
			transparent: true,
			side: DoubleSide
		} );

		let extrudeMesh = new Mesh( extrude, material );

		this.renderObject = new Object3D();
		this.renderObject.add(extrudeMesh);
	}

	setVisible(visible){
		super.setVisible(visible);

		if (this.visible) {
			console.log(this.renderObject);
			this.blueMap.shapeScene.add(this.renderObject);
		} else {
			this.blueMap.shapeScene.remove(this.renderObject);
		}
	}

	onClick = () => {
		if (this.label) {
			//this.blueMap.ui.hudInfo.showInfoBubble(this.label, this.position.x, this.position.y, this.position.z);
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