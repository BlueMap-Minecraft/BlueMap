import org.spongepowered.gradle.plugin.config.PluginLoaders

plugins {
    bluemap.implementation
    bluemap.modrinth
    bluemap.ore
    id ( libs.plugins.sponge.plugin.get().pluginId )
}

val supportedMinecraftVersions = listOf(
    "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
    "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10"
)

dependencies {
    api ( project( ":common" ) ) {
        exclude( group = "com.google.code.gson", module = "gson" )
        exclude( group = "net.kyori", module = "adventure-api" )
    }

    api ( libs.bstats.sponge )

    compileOnly ( libs.jetbrains.annotations )
}

sponge {
    apiVersion("11.0.0")
    license("MIT")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    plugin("bluemap") {
        displayName("bluemap")
        entrypoint("de.bluecolored.bluemap.sponge.SpongePlugin")
        description("A 3d-map of your Minecraft worlds view-able in your browser using three.js (WebGL)")
        contributor("Blue (TBlueF, Lukas Rieger)") {
            description("Lead Developer")
        }
        dependency("spongeapi") {
            optional(false)
        }
    }
}

tasks.shadowJar {

    // airlift
    relocate ("io.airlift", "de.bluecolored.shadow.airlift")

    // caffeine
    relocate ("com.github.benmanes.caffeine", "de.bluecolored.shadow.caffeine")
    relocate ("org.checkerframework", "de.bluecolored.shadow.checkerframework")
    relocate ("com.google.errorprone", "de.bluecolored.shadow.errorprone")

    // dbcp2
    relocate ("org.apache.commons", "de.bluecolored.shadow.apache.commons")

    // configurate
    relocate ("org.spongepowered.configurate", "de.bluecolored.shadow.configurate")
    relocate ("com.typesafe.config", "de.bluecolored.shadow.typesafe.config")
    relocate ("io.leangen.geantyref", "de.bluecolored.shadow.geantyref")

    // lz4
    relocate ("net.jpountz", "de.bluecolored.shadow.jpountz")

    // bstats
    relocate ("org.bstats", "de.bluecolored.shadow.bstats")

}

oreDeployment {
    defaultPublication {
        publishArtifacts.setFrom( tasks.getByName("release").outputs.files.singleFile )
    }
}

modrinth {
    gameVersions.addAll(supportedMinecraftVersions)
}
