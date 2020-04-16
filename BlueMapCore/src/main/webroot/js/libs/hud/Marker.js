export default class Marker {

	constructor(blueMap, markerSet, markerData) {
		this.blueMap = blueMap;
		this.markerSet = markerSet;
		this.type = markerData.type;
		this.map = markerData.map;
		this.label = markerData.label;
		this.link = markerData.link;
		this.newTab = !!markerData.newTab;

		this.visible = false;

		this.minDistance = parseFloat(markerData.minDistance ? markerData.minDistance : 0);
		this.minDistanceSquared = this.minDistance * this.minDistance;
		this.maxDistance = parseFloat(markerData.maxDistance ? markerData.maxDistance : 100000);
		this.maxDistanceSquared = this.maxDistance * this.maxDistance;
	}

	setVisible(visible) {
		this.visible = visible && this.blueMap.map === this.map;
		this.blueMap.updateFrame = true;
	}

	updateRenderObject(object, scene, camera){
		if (this.visible) {
			//update visiblity
			let distanceSquared = object.position.distanceToSquared(camera.position);
			object.visible = distanceSquared <= this.maxDistanceSquared && distanceSquared >= this.minDistanceSquared;
		} else {
			object.visible = false;
			scene.remove(object);
		}
	}

}