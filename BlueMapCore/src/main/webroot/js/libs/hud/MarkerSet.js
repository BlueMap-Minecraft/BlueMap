import POIMarker from "./POIMarker";
import ShapeMarker from "./ShapeMarker";

export default class MarkerSet {

	constructor(blueMap, setData) {
		this.blueMap = blueMap;
		this.id = setData.id;
		this.label = setData.label ? this.escapeHTML(setData.label) : this.id;
		this.toggleable = setData.toggleable !== undefined ? !!setData.toggleable : true;
		this.defaultHide = !!setData.defaultHide;
		this.marker = [];

		this.visible = true;

		if (Array.isArray(setData.marker)){
			setData.marker.forEach(markerData => {
				switch (markerData.type){
					case 'poi':
						this.marker.push(new POIMarker(this.blueMap, this, markerData));
						break;
					case 'shape':
						this.marker.push(new ShapeMarker(this.blueMap, this, markerData));
						break;
				}
			});
		}
	}

	update() {
		this.marker.forEach(marker => {
			marker.setVisible(this.visible);
		});
	}

	escapeHTML(text) {
		return text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
	}

}