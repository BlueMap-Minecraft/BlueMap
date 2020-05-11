![title-banner](https://bluecolored.de/paste/BluemapBanner.png)

BlueMap is a tool that generates 3d-maps of your Minecraft worlds and displays them in your browser. Take a look at [this demo](https://bluecolored.de/bluemap). It is really easy to set up - almost plug-and-play - if you use the integrated web-server (optional).

The Sponge/Spigot-Plugin automatically updates your map as soon as something changes in your world, as well as rendering newly generated terrain and managing the render-tasks.

**BlueMap is currently in an early development state!**

A lot of features are still missing, and some blocks - especially some tile-entities - will not render correctly/at all.
See below for a list of what is planned for future releases.

### Download
You can choose a version and download BlueMap from [here](https://github.com/BlueMap-Minecraft/BlueMap/releases).

### Using BlueMap
BlueMap can be used on the command-line, or as a plugin for your Sponge/Spigot/Paper-Server. Read the [wiki](https://github.com/BlueMap-Minecraft/BlueMap/wiki) to get started!

### Discord
If you have a question, help others using BlueMap or get the latest news and info you are welcome to join us [on Discord](https://discord.gg/zmkyJa3)!

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

### Todo / planned features
Here is a *(surely incomplete)* list of things that i want to include in future versions. *(They are not in any specific order. There is no guarantee that any of those things will ever be included.)*

- live player positions
- fabric version
- render more tile-entities (banners, shulker-chests, etc..)
- render entities (armor-stands, item-frames, maybe even cows and such..)
- free-flight-controls
- more configurations
- easier mod-integration
- ability to display the world-border
- animated textures (if feasible)
- add support for models in obj format (if feasible)
