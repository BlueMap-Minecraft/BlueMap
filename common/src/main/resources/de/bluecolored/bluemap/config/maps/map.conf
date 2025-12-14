##                          ##
##         BlueMap          ##
##        Map-Config        ##
##                          ##

# The path to the save folder of the world to render.
# If this is not defined (commented out or removed), the map will be only registered to the webserver and the webapp
# but not rendered or loaded by BlueMap. This can be used to display a map that has been rendered somewhere else.
world: "${world}"

# The dimension of the world. Can be "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end",
# or any dimension key introduced by a mod or datapack.
dimension: "${dimension}"

# The display name of this map (how this map will be named on the webapp).
# You can change this at any time.
# Default is the id of this map
name: "${name}"

# A lower value makes the map appear first (in lists and menus), a higher value makes it appear later.
# The value needs to be an integer but it can be negative.
# You can change this at any time.
# Default is 0
sorting: ${sorting}

# The position in the world where the map will be centered on when you open it.
# You can change this at any time.
# Default is { x: 0, z: 0 }
start-pos: { x: 0, z: 0 }

# The color of the sky as a hex-color.
# You can change this at any time.
# Default is "#7dabff"
sky-color: "${sky-color}"

# The color of the void as a hex-color.
# You can change this at any time.
# Default is "#000000"
void-color: "${void-color}"

# Defines the initial sky light strength the map will be set to when it is opened.
# 0 is no sky light, 1 is fully lit up.
# You can change this at any time.
# Default is 1
sky-light: 1

# Defines the ambient light strength that every block is receiving, regardless of the sunlight/blocklight.
# 0 is no ambient light, 1 is fully lit up.
# You can change this at any time.
# Default is 0
ambient-light: ${ambient-light}

# BlueMap tries to omit all blocks that are below this Y-level and are not visible from above ground.
# More specifically, block faces that have a sunlight/skylight value of 0 are removed.
# This improves the performance of the map on slower devices by a lot, but might cause some blocks to disappear that should normally be visible.
# Changing this value requires a re-render of the map.
# Set to a very high value to remove caves everywhere (e.g. 10000).
# Set to a very low value to remove nothing and render all caves (e.g. -10000).
# Default is 55 (slightly below water-level).
remove-caves-below-y: ${remove-caves-below-y}

# This is the amount of blocks relative to the "ocean floor" heightmap that the cave detection will start at.
# Everything above that (heightmap relative) y-level will not be removed.
# Comment or set to a very high value to disable using the ocean floor heightmap for cave detection.
# Changing this value requires a re-render of the map.
# Defaults to 10000 (disabled).
cave-detection-ocean-floor: -5

# With this value set to true, BlueMap also uses the block light value (additionally to the sky light) to "detect caves"
# (see the option above: remove-caves-below-y).
# Changing this value requires a re-render of the map.
# Default is false
cave-detection-uses-block-light: false

# The minimum "inhabitedTime" value that a chunk must have to be rendered.
# The "inhabitedTime" value of a chunk refers to the cumulative number of ticks players have been near this chunk.
# If you set this to a value greater than 0, BlueMap will only render chunks that players have visited already.
# Default is 0 (all generated chunks).
min-inhabited-time: 0

# With the render-mask you can limit the map render.
# This can be used to render only a certain part of a world or ignore the Nether's ceiling.
# If you change the render-mask, BlueMap automatically tries to update the map,
# including deleting map tiles which are outside the new limits.
# You can use "/bluemap fix-edges <map>" to fix any remaining issues.
#
# Please check out the wiki for more detailed information on how to configure this:
# https://bluemap.bluecolored.de/wiki/customization/Masks.html
#
# Default is no mask, BlueMap will render everything that exists.
render-mask: [
  {
    #min-x: -4000
    #max-x: 4000
    #min-z: -4000
    #max-z: 4000
    #min-y: 50
    #max-y: 100
  }${remove-nether-ceiling<<
  {
    # This removes everything at and between y 90 and 127 (the Nether's ceiling).
    # Structures above the bedrock ceiling remain visible.
    subtract: true
    min-y: 90
    max-y: 127
  }>>}
]

# Using this, BlueMap pretends that every Block outside of the defined render-mask is AIR,
# this means you can see the blocks where the world is cut (instead of having a see-through/xray view).
# This only has an effect if you set some render-mask above.
# Changing this value requires a re-render of the map.
# Default is true
render-edges: true

# The sun-light strength that blocks at map edges will receive if render-edges is enabled.
# Should be a value between 0 and 15.
# Default is 15
edge-light-strength: 8

# Whether the perspective view will be enabled for this map.
# Changing this to true requires a re-render of the map, only if the hires-layer is enabled and free-flight view is disabled.
# Default is true
enable-perspective-view: true

# Whether the flat (isometric, top-down) view will be enabled for this map.
# Having only flat-view enabled while disabling free-flight and perspective will speed up the render and reduce the maps storage size.
# Default is true
enable-flat-view: true

# Whether the free-flight view will be enabled for this map.
# Changing this to true requires a re-render of the map, only if the hires-layer is enabled and perspective view is disabled.
# Default is true
enable-free-flight-view: true

# Whether the hires-layer will be enabled.
# Disabling this will speed up rendering and reduce the size of the map files a lot.
# But you will not be able to see the full 3D models if you zoom in on the map.
# Changing this to false will not remove any existing tiles, existing tiles just won't get updated anymore.
# Changing this to true will require a re-render of the map.
# Default is true
enable-hires: true

# This defines the storage-config that will be used to save this map.
# You can find your storage configs next to this config file in the 'storages'-folder.
# Changing this value requires a re-render of the map. The map in the old storage will not be deleted.
# Default is "file"
storage: "file"

# Normally BlueMap detects if a chunk has not yet generated it's light data and omits rendering those chunks.
# If this is set to true BlueMap will render Chunks even if there is no light data!
# This can be useful for example if some mod prevents light data from being saved correctly.
# However, this also has a few drawbacks:
#  - Cave rendering will always be enabled (BlueMap is using the sky light data to detect "caves").
#  - Everything will be rendered fully lit (sky light value of 15, looks similar to having night vision).
#  - Night mode might not work correctly.
# Default is false (wait for light data).
ignore-missing-light-data: false

# Here you can define any static marker-sets with markers that should be displayed on the map.
# You can change this at any time.
# If you need dynamic markers, you can use any plugin that integrates with BlueMap's API.
# Here is a list: https://bluemap.bluecolored.de/community/3rdPartySupport.html
marker-sets: {

  # Please check out the wiki for information on how to configure this:
  # https://bluemap.bluecolored.de/wiki/customization/Markers.html

}
