plugins {
    bluemap.implementation
    bluemap.modrinth
    bluemap.curseforge
    alias ( libs.plugins.neoforge.gradle )
}

val supportedMinecraftVersions = listOf(
    "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4"
)

val minecraftVersion = supportedMinecraftVersions.first()
val neoVersion = "21.0.0-beta"
val loaderVersion = "4"

val shadowInclude: Configuration by configurations.creating
configurations.api.get().extendsFrom(shadowInclude)

neoForge {
    version = neoVersion
}

dependencies {
    shadowInclude ( project( ":common" ) ) {
        exclude ( group = "com.google.code.gson", module = "gson" )
    }

    shadowInclude ( libs.bluecommands.brigadier ) {
        exclude ( group = "com.mojang", module = "brigadier" )
    }
    shadowInclude ( libs.adventure.gson ) {
        exclude ( group = "com.google.code.gson", module = "gson" )
    }

    jarJar ( libs.flow.math.get().group, libs.flow.math.get().name , "[${libs.flow.math.get().version},)" )
    jarJar ( libs.bluenbt.get().group, libs.bluenbt.get().name , "[${libs.bluenbt.get().version},)" )
}

tasks.shadowJar {
    configurations = listOf(shadowInclude)

    // exclude jarInJar
    dependencies {
        exclude( dependency ( libs.flow.math.get() ) )
        exclude( dependency ( libs.bluenbt.get() ) )
    }

    // adventure
    relocate ("net.kyori", "de.bluecolored.shadow.adventure")

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

tasks.withType(ProcessResources::class).configureEach {
    val replacements = mapOf(
        "version" to project.version,
        "minecraft_version" to minecraftVersion,
        "neo_version" to neoVersion,
        "loader_version" to loaderVersion,
    )
    inputs.properties(replacements)
    filesMatching(listOf(
        "META-INF/neoforge.mods.toml",
        "pack.mcmeta"
    )) { expand(replacements) }
}

val mergeShadowAndJarJar = tasks.create<Jar>("mergeShadowAndJarJar") {
    dependsOn( tasks.shadowJar, tasks.jarJar )
    from (
        zipTree( tasks.shadowJar.map { it.outputs.files.singleFile } ),
        tasks.jarJar.map { it.outputs.files }
    ).exclude(
        "META-INF/services/net.kyori.adventure*" // not correctly relocated and not needed -> exclude
    )
    archiveFileName = "${project.name}-${project.version}-merged.jar"
}

tasks.getByName<CopyFileTask>("release") {
    dependsOn( mergeShadowAndJarJar )
    inputFile = mergeShadowAndJarJar.outputs.files.singleFile
}

modrinth {
    loaders.addAll("neoforge")
    gameVersions.addAll(supportedMinecraftVersions)
}

curseforgeBlueMap {
    addGameVersion("NeoForge")
    addGameVersion("Java ${java.toolchain.languageVersion.get()}")
    //addGameVersion("Server")
    supportedMinecraftVersions.forEach {
        addGameVersion(it)
    }
}
