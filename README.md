![title-banner](https://bluecolored.de/paste/bluemap-title.jpg)

BlueMap is a tool that generates 3d-maps of your Minecraft worlds and displays them in your browser. Take a look at [this demo](https://bluecolored.de/bluemap). It is really easy to set up - almost plug-and-play - if you use the integrated web-server (optional). 

The Sponge-Plugin automatically updates your map as soon as something changes in your world, as well as rendering newly generated terrain and managing the render-tasks.

**BlueMap is currently in a really early development state!**

The majority of features are still missing, and some blocks - especially tile-entities - will not render correctly/at all.
See below for a list of what is planned for future releases.

### Clone
Easy:

`git clone https://github.com/BlueMap-Minecraft/BlueMap.git`

### Build
In order to build BlueMap you simply need to run the `./gradlew shadowJar` command.
You can find the compiled JAR file in `./build/libs`

### Issues / Suggestions
You found a bug, have another issue or a suggestion? Please create an issue [here](https://github.com/BlueMap-Minecraft/BlueMap/issues)!

### Contributing
You are welcome to contribute!  
Just create a pull request with your changes :)

## Using the CLI
BlueMap can be used on the command-line, to render your Minecraft-Worlds *(Currently supported versions: 1.12 - 1.14)*.

Use `java -jar bluemap.jar` and BlueMap will generate a default config in the current working directory. You then can configure your maps and even the webserver as you wish. Then, re-run the command and BlueMap will render all the configured maps for you and start the webserver if you turned it on in the config.
To only run the webserver, just don't define any maps in the config. 

You can use `-c <config-file>` on the command-line to define a different configuration-file.

## Using the Sponge-Plugin
### Getting started
BlueMap is mostly plug-and-play. Just install it like every other Sponge-Plugin and start your server. BlueMap will then generate a config-file for you in the `./config/bluemap/` folder. Here you can configure your maps and the webserver. The config has many useful comments in it, explaining everything :)

**Before BlueMap can render anything,** it needs one more thing: resources! To render all the block-models, BlueMap makes use of the default minecraft-resources. Since they are property of mojang i can not include them in the plugin. Fortunately BlueMap can download them from mojangs servers for you, but you need to explicitly agree to this in the config! Simply change the `accept-download: false` setting to `accept-download: true`, and run the `/bluemap reload` command.

After downloading the resources, BlueMap will start updating the configured worlds. To render the whole world for a start, you can use this command `/bluemap render [world]`. 

Then, head over to `http://<your-server-ip>:8100/` and you should see your map! *(If there is only black, you might have to wait a little until BlueMap has rendered enough of the map. You can also try to zoom in: the hires-models are saved first)*

If you need help with the setup i will be happy to help you!

### Metrics and Webserver
**Bluemap uses [bStats](https://bstats.org/) and an own metrics-system and is hosting a web-server!**

Metrics are really useful to keep track of how the plugin is used and helps me stay motivated! Please turn them on :)

**bStats:** All data collected by bStats can be viewed here: https://bstats.org/plugin/sponge/BlueMap. bStats data-collection is controlled by the metrics-setting set in sponges configuration! *(Turned off by default)*

**own metrics:** Additionally to bStats, BlueMap is sending a super small report, containing only the implementation-name and the version of the BlueMap-plugin to my server (`https://metrics.bluecolored.de/bluemap`). I do this, because i might release some other implementations for BlueMap (like a CLI, or a forge-mod) that are not supported by bStats. Here is an example report:
```json
{
    "implementation": "Sponge",
    "version": "0.0.0"
}
```
This data-collection is also controlled by the metrics-setting set in sponges configuration! *(Turned off by default)*

**web-server:** The web-server is a core-functionality of this plugin. It is enabled by default but can be disabled in the plugin-config. By default the web-server is bound to the standard ip-address on port `8100` and is hosting the content of the `./bluemap/web/`-folder.

### Commands and Permissions
command | permission | description
--- | --- | ---
/bluemap | bluemap.status | displays BlueMaps render status
/bluemap reload | bluemap.reload | reloads all resources, configuration-files and the web-server
/bluemap pause | bluemap.pause | pauses all rendering
/bluemap resume | bluemap.resume | resumes all paused rendering
/bluemap render \[world\] | bluemap.rendertask.create.world | renders the whole world
*\[clickable command in /bluemap\]* | bluemap.rendertask.prioritize | prioritizes the clicked render-task
*\[clickable command in /bluemap\]* | bluemap.rendertask.remove | removes the clicked render-task

## Todo / planned features
Here is a *(surely incomplete)* list of things that i want to include in future versions. *(They are not in any specific order. There is no guarantee that any of those things will ever be included.)*

- render tile-entities (chests, etc..)
- render entities
- configurable markers / regions
- marker / region API
- free-flight-controls
- live player positions
- shaders for dynamic day/night
- more configurations
- better resource-pack support
- mod-support (or an easy way for modders to do so themselves)
- BlueMap as spigot plugin
- BlueMap as forge mod
- more render-tasks (commands to render parts of your world)
- config to restrict map-generation to some bounds
- ability to display the world-border
