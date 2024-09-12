plugins {
    bluemap.implementation
    bluemap.modrinth
}

val supportedMinecraftVersions = listOf(
    "1.16.5",
    "1.17", "1.17.1",
    "1.18", "1.18.1", "1.18.2",
    "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4",
    "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
    "1.21"
)

val apiVersion = "1.16"
val spigotVersion = "1.16.5-R0.1-SNAPSHOT"

dependencies {
    api ( project( ":common" ) ) {
        exclude( group = "com.google.code.gson", module = "gson" )
    }

    shadow ( "org.spigotmc", "spigot-api", spigotVersion )
    api ( libs.bstats.bukkit )
}

tasks.shadowJar {

    // airlift
    relocate ("io.airlift", "de.bluecolored.shadow.airlift")

    // brigadier
    relocate ("com.mojang.brigadier", "de.bluecolored.shadow.brigadier")

    // bluenbt
    relocate ("de.bluecolored.bluenbt", "de.bluecolored.shadow.bluenbt")

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

tasks.processResources {
    from("src/main/resources") {
        include("plugin.yml")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        expand (
            "version" to project.version,
            "api_version" to apiVersion,
        )
    }
}

modrinth {
    loaders.addAll("spigot", "paper", "purpur")
    gameVersions.addAll(supportedMinecraftVersions)
}
