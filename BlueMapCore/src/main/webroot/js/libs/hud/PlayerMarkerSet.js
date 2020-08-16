import POIMarker from "./POIMarker";
import ShapeMarker from "./ShapeMarker";
import {cachePreventionNr} from "../utils";
import PlayerMarker from "./PlayerMarker";
import {Vector3} from "three";

export default class PlayerMarkerSet {

	constructor(blueMap) {
		this.blueMap = blueMap;
		this.id = "bluemap-live-players";
		this.label = "players";
		this.toggleable = true;
		this.defaultHide = false;
		this.marker = [];
		this.markerMap = {};

		this.visible = true;
	}

	update() {
		this.marker.forEach(marker => {
			marker.setVisible(this.visible);
		});
	}

	async updateLive(){
		await new Promise((resolve, reject) => {
			this.blueMap.fileLoader.load(this.blueMap.liveApiRoot + 'players?' + cachePreventionNr(),
				liveData => {
					try {
						liveData = JSON.parse(liveData);
						resolve(liveData);
					} catch (e){
						reject(e);
					}
				},
				xhr => {},
				error => {
					reject(error);
				}
			);
		}).then((liveData) => {
			this.updateWith(liveData)
		}).catch((e) => {
			console.error("Failed to update player-markers!", e);
		});
	}

	updateWith(liveData){
		this.marker.forEach(marker => {
			marker.nowOnline = false;
		});

		for(let i = 0; i < liveData.players.length; i++){
			let player = liveData.players[i];
			let marker = this.markerMap[player.uuid];

			if (!marker){
				marker = new PlayerMarker(this.blueMap, this, {
					type: "playermarker",
					map: null,
					position: player.position,
					label: player.name,
					link: null,
					newTab: false
				}, player.uuid, player.world);

				this.markerMap[player.uuid] = marker;
				this.marker.push(marker);
			}

			marker.nowOnline = true;
			marker.position = new Vector3(player.position.x, player.position.y + 1.5, player.position.z);
			marker.world = player.world;
			marker.updatePosition();
		}

		this.marker.forEach(marker => {
			if (marker.nowOnline !== marker.online){
				marker.online = marker.nowOnline;
				marker.setVisible(this.visible);
			}
		});
	}
}