![title-banner](https://bluecolored.de/paste/BluemapBanner.png)

BlueMap is a tool that generates 3d-maps of your Minecraft worlds and displays them in your browser. Take a look at [this demo](https://bluecolored.de/bluemap). It is really easy to set up - almost plug-and-play - if you use the integrated web-server (optional).

The plugins/mods automatically update your map as soon as something changes in your world, as well as rendering newly generated terrain and managing the render-tasks.

**BlueMap is currently in a BETA state!**

It is however already quite stable and usable. There are just some features still missing, and some blocks - especially tile-entities - will not render correctly/at all.
See below for a list of what is planned for future releases.

![screenshot](https://bluecolored.de/paste/BlueMapScreenshot.jpg)

### Download
You can choose a version and download BlueMap from [here](https://github.com/BlueMap-Minecraft/BlueMap/releases).

### Using BlueMap
BlueMap can be used on the command-line, or as a plugin/mod for your Sponge/Spigot/Paper/Forge/Fabric-Server. Read the [wiki](https://github.com/BlueMap-Minecraft/BlueMap/wiki) to get started!

### Clone
If you have git installed, simply use the command `git clone --recursive https://github.com/BlueMap-Minecraft/BlueMap.git` to clone BlueMap.

### Build
In order to build BlueMap you simply need to run the `./gradlew clean build` command in BlueMap's root directory.
You can find the compiled JAR files in `./build/release`.

### Issues / Suggestions
You found a bug, have another issue or a suggestion? Please create an issue [here](https://github.com/BlueMap-Minecraft/BlueMap/issues)!

### Contributing
You are welcome to contribute!
Just create a pull request with your changes :)

**If you want to have your changes merged, make sure they are complete, documented and well tested!**

Keep in mind that we have to maintain all new features and keep supporting them in the future.
This means we always can decide to not accept a PR for any reason.

### Todo / planned features
Here is a *(surely incomplete)* list of things that i want to include in future versions. *(They are not in any specific order. There is no guarantee that any of those things will ever be included.)*

- render more tile-entities (banners, shulker-chests, etc..)
- render entities (armor-stands, item-frames, maybe even cows and such..)
- free-flight-controls
- more configurations
- easier mod-integration
- ability to display the world-border
- animated textures (if feasible)
- add support for models in obj format (if feasible)

### Links
**Wiki:** https://bluecolo.red/map-wiki <br>
**Reddit:** https://www.reddit.com/r/BlueMap <br>
**Discord:** https://bluecolo.red/map-discord <br>
