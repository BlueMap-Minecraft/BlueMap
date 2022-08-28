[![title-banner](https://bluecolored.de/paste/BluemapBanner.png)](https://bluemap.bluecolored.de/)

<div align="center">

create **3D**-maps of your Minecraft worlds and display them in your browser  
**>> [DEMO MAP](https://bluecolored.de/bluemap) <<**


[![GitHub issues](https://img.shields.io/github/issues-raw/BlueMap-Minecraft/BlueMap)](https://github.com/orgs/BlueMap-Minecraft/projects/2)
[![GitHub all releases](https://img.shields.io/github/downloads/BlueMap-Minecraft/BlueMap/total)](https://github.com/BlueMap-Minecraft/BlueMap/releases)
[![GitHub release (latest by date)](https://img.shields.io/github/v/release/BlueMap-Minecraft/BlueMap)](https://github.com/BlueMap-Minecraft/BlueMap/releases/latest)
[![Discord](https://img.shields.io/discord/665868367416131594?label=discord)](https://discord.gg/zmkyJa3)

</div>

<br>
<br>

## What is BlueMap
BlueMap is basically a program that reads your Minecraft world files and generates not only a map, but also 3D-models of the whole surface.
With the web-app you then can look at those in your browser and basically view the world as if you were ingame! 
Or just look at it from far away to get an overview.

[![screenshot](https://bluecolored.de/paste/BlueMapScreenshot.jpg?2)](https://bluecolored.de/bluemap)

BlueMap comes as a Spigot/Paper or Sponge Plugin, as a Fabric or Forge-Mod and you can also use BlueMap without any Server
from the Command-Line as a standalone tool.

If installed as a Plugin/Mod, BlueMap **renders asynchronously** to your MinecraftServer-Thread. 
This means at no time it will block your server-thread directly. 
So as long as your CPU is not fully utilized, your server should not be slowed down while BlueMap is rendering.


## Using BlueMap
You can download BlueMap from [here](https://github.com/BlueMap-Minecraft/BlueMap/releases).  
Read the [installation instructions](https://bluemap.bluecolored.de/wiki/getting-started/Installation.html) to get started!

Here you can see how many servers are using BlueMap:

[![BlueMap Graph](https://metrics.bluecolored.de/bluemap/graph.php?1)](https://metrics.bluecolored.de/)

## Development
### Clone
If you have git installed, simply use the command `git clone --recursive https://github.com/BlueMap-Minecraft/BlueMap.git` to clone BlueMap.

### Build
In order to build BlueMap you simply need to run the `./gradlew clean build` command in BlueMap's root directory.
You can find the compiled JAR files in `./build/release`.

### Issues
You found a bug, have another issue?  
First, make sure it's not on your end, if you are unsure you can always ask about it in our [Discord](https://bluecolo.red/map-discord).  
If you are sure it's a bug on BlueMap's end, please create an issue [here](https://github.com/BlueMap-Minecraft/BlueMap/issues)!

### Contributing
You are welcome to contribute!
Just create a pull request with your changes :)

**If you want to have your changes merged, make sure they are complete, documented and well tested!**

Keep in mind that we have to maintain all new features and keep supporting them in the future.
This means we always can decide to not accept a PR for any reason.

## Links
**TODO-List:** https://github.com/orgs/BlueMap-Minecraft/projects/2  
**Wiki:** https://bluecolo.red/map-wiki  
**Discord:** https://bluecolo.red/map-discord  
**Reddit:** https://www.reddit.com/r/BlueMap  

---

[![JetBrainsLogo](https://bluecolored.de/paste/jetbrains-variant-4.svg)](https://www.jetbrains.com/?from=BlueMap)<br>
<br>
Special thanks to [JetBrains](https://www.jetbrains.com/?from=BlueMap) for giving out an OpenSource-Licence for BlueMap development!
