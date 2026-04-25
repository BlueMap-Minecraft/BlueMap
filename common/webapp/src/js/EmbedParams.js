const params = new URLSearchParams(window.location.search);
const bool = (key, def = true) => params.has(key) ? params.get(key) !== 'false' && params.get(key) !== '0' : def;

export const embedParams = {
    controls:       bool('controls'),
    menu:           bool('menu'),
    zoom:           bool('zoom'),
    maps:           bool('maps'),
    markers:        bool('markers'),
    players:        bool('players'),
    daynight:       bool('daynight'),
    compass:        bool('compass'),
    position:       bool('position'),
    controlsswitch: bool('controlsswitch'),
    resetcamera:    bool('resetcamera'),
};
