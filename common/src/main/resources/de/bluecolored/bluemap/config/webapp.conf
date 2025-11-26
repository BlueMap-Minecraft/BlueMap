##                          ##
##         BlueMap          ##
##      Webapp-Config       ##
##                          ##

# With this setting you can disable the creation and updating of all webapp related files.
# Default is true
enabled: true

# The webroot where the webapp files will be created.
# Usually this should be set to the same directory like in the webserver.conf!
# Default is "bluemap/web"
webroot: "${webroot}"

# Whether the settings.json of the webapp should be updated/synchronized with the current BlueMap settings.
# If this is set to "false", BlueMap will only add maps to the settings.json but never remove unknown ones or update other settings.
# Disabling this is for example useful if you are running multiple BlueMap instances on the same webroot and don't want them to overwrite each others maps.
# Default is true
update-settings-file: true

# If the webapp should use cookies to save the configurations of a user.
# Default is true
use-cookies: true

# If the webapp will default to flat-view instead of perspective-view.
# Default is false (perspective-view)
default-to-flat-view: false

# The default map and camera location where a user will start after opening the webapp.
# This is in form of the URL anchor: Open your map in a browser and look at the URL, everything after the '#' is the value for this setting.
# Default is "no anchor" (The camera will start with the topmost map and at that map's starting point).
#start-location: "world:0:16:-32:390:0.1:0.19:0:0:perspective"

# The minimum (closest) and maximum (furthest) distance (in blocks) that the camera can be from the ground.
# Default is min:5 max:100000
min-zoom-distance: 5
max-zoom-distance: 100000

# The default value of the resolution (settings menu).
# Possible values are: 0.5, 1, 2
# Default is 1
resolution-default: 1

# The min, max and default values of the hires render distance slider (settings menu).
# The values are in blocks.
# Default is max:500 default:100 and min:0
hires-slider-max: 500
hires-slider-default: 100
hires-slider-min: 0

# The min, max and default values of the lowres render distance slider (settings menu).
# The values are in blocks.
# Default is max:7000 default:2000 and min:500
lowres-slider-max: 7000
lowres-slider-default: 2000
lowres-slider-min: 500

# Here you can specify an alternative base URL where all of the map data is loaded from.
# Default is "maps"
#map-data-root: "https://cdn.my-domain.com/mapdata"

# Here you can specify an alternative base URL where all of the live data is loaded from.
# Default is "maps"
#live-data-root: "https://cdn.my-domain.com/livedata"

# Here you can add URLs to custom scripts (js) so they will be loaded by the webapp.
# You can place them somewhere in BlueMap's webroot and add the (relative) link here.
scripts: [
  #"js/my-custom-script.js"
]

# Here you can add URLs to custom styles (css) so they will be loaded by the webapp.
# You can place them somewhere in BlueMap's webroot and add the (relative) link here.
styles: [
  #"css/my-custom-style.css"
]
