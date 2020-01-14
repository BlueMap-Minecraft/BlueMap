import $ from 'jquery';
import BlueMap from './libs/BlueMap.js';

import '../style/style.scss';

$(document).ready(() => {
	window.blueMap = new BlueMap($('#map-container')[0], 'data/');
});
