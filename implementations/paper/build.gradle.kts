plugins {
    bluemap.implementation
    bluemap.modrinth
    bluemap.hangar
}

val supportedMinecraftVersions = listOf(
    "1.20.6",
    "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5"
)

val minecraftVersion = supportedMinecraftVersions.first()
val paperVersion = "${minecraftVersion}-R0.1-SNAPSHOT"
val apiVersion = "1.20.6" // paper-version but without minor

dependencies {
    api ( project( ":common" ) ) {
        exclude( group = "com.google.code.gson", module = "gson" )
        exclude( group = "net.kyori", module = "adventure-api" )
    }

    shadow ( "io.papermc.paper", "paper-api", paperVersion )
    api ( libs.bstats.bukkit )
    api ( libs.bluecommands.brigadier ) {
        exclude ( group = "com.mojang", module = "brigadier" )
    }
}

tasks.shadowJar {

    // exclude libraries added via plugin.yml
    dependencies {
        exclude( dependency ( libs.flow.math.get() ) )
    }

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

tasks.processResources {
    from("src/main/resources") {
        include("plugin.yml")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        expand (
            "version" to project.version,
            "api_version" to apiVersion,
            "flow_math_version" to libs.flow.math.get().version
        )
    }
}

modrinth {
    loaders.addAll("paper", "purpur", "folia")
    gameVersions.addAll(supportedMinecraftVersions)
}

hangarPublish {
    publications.named("bluemap") {
        platforms.paper {
            platformVersions = supportedMinecraftVersions
        }
    }
}
