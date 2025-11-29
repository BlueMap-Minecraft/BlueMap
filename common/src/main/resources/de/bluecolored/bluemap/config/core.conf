##                          ##
##         BlueMap          ##
##       Core-Config        ##
##                          ##

# By changing the setting (accept-download) below to TRUE you are indicating that you have accepted Mojang's EULA (https://account.mojang.com/documents/minecraft_eula),
# you confirm that you own a license to Minecraft (Java Edition),
# and you agree that BlueMap will download and use a Minecraft client file (depending on the Minecraft version) from Mojang's servers (https://piston-meta.mojang.com/) for you.
# This file contains resources that belong to Mojang and you must not redistribute it or do anything else that is not compliant with Mojang's EULA.
# BlueMap uses resources in this file to generate the 3D models used for the map and texture them. Without these, BlueMap will not work.
# ${timestamp}
accept-download: false

# The folder where BlueMap saves data files it needs during runtime.
# For example, the render progress file, which is used to resume the render across restarts.
# Default is "bluemap"
data: "${data}"

# This changes the amount of threads that BlueMap will use to render the maps.
# A higher value can improve the render speed, but could impact performance on the host machine.
# This should be always below or equal to the number of available processor cores.
# Zero or a negative value means the amount of available processor cores subtracted by the value.
# For example, on a machine with 6 cores, a value of -2 would result in 4 render threads.
# Default is 1
render-thread-count: ${render-thread-count}

# Controls whether BlueMap should try to find and load mod resources and datapacks from the server/world directories.
# Default is true
scan-for-mod-resources: true
${metrics<<
# If this is true, BlueMap might send really basic metric reports containing only the implementation type and the version that is being used to https://metrics.bluecolored.de/bluemap/
# This allows me to track the basic usage of BlueMap and helps me stay motivated to further develop this tool! Please leave it on :)
# An example report looks like this: {"implementation":"${implementation}","version":"${version}","mcVersion":"${mcVersion}"}
# Default is true
metrics: true
>>}
# Config-section for debug logging:
log: {
  # The file where the debug log will be written to.
  # Comment out to disable debug logging completely.
  # Java String formatting syntax can be used to add timestamps, see: https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html
  # Default is no logging.
  file: "${logfile}"
  #file: "${logfile-with-time}"

  # Whether the logger should append to an existing file, or overwrite it.
  # Default is false (overwrite the file).
  append: false
}
