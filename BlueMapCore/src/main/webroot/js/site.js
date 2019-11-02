// global variable to enable access through browser console
var blueMap;

$(document).ready(function () {
	blueMap = new BlueMap($("#map-container")[0], "data/");
});
