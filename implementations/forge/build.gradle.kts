plugins {
    bluemap.implementation
    bluemap.modrinth
    bluemap.curseforge
    alias ( libs.plugins.forgegradle )
}

val supportedMinecraftVersions = listOf(
    "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5"
)

val minecraftVersion = supportedMinecraftVersions.first()
val forgeVersion = "51.0.1"

val shadowInclude: Configuration by configurations.creating
configurations.api.get().extendsFrom(shadowInclude)
jarJar.enable()

dependencies {

    shadowInclude ( project( ":common" ) ) {
        exclude ( group = "com.google.code.gson", module = "gson" )
    }

    minecraft ( "net.minecraftforge", "forge", "$minecraftVersion-$forgeVersion" )

    shadowInclude ( libs.bluecommands.brigadier ) {
        exclude ( group = "com.mojang", module = "brigadier" )
    }
    shadowInclude ( libs.adventure.gson ) {
        exclude ( group = "com.google.code.gson", module = "gson" )
    }

    jarJar ( libs.flow.math.get().group, libs.flow.math.get().name , "[${libs.flow.math.get().version},)" )
    jarJar ( libs.bluenbt.get().group, libs.bluenbt.get().name , "[${libs.bluenbt.get().version},)" )

}

minecraft {
    mappings( "official", minecraftVersion )
    reobf = false
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

tasks.jarJar.configure {
    archiveFileName = "${project.name}-${project.version}-jarjar.jar"
}

tasks.withType(ProcessResources::class).configureEach {
    val replacements = mapOf(
        "version" to project.version,
        "minecraft_version" to minecraftVersion,
        "forge_version" to forgeVersion,
    )
    inputs.properties(replacements)
    filesMatching(listOf(
        "META-INF/mods.toml",
        "pack.mcmeta"
    )) { expand(replacements) }
}

val mergeShadowAndJarJar = tasks.create<Jar>("mergeShadowAndJarJar") {
    dependsOn( tasks.shadowJar, tasks.jarJar )
    from (
        zipTree( tasks.shadowJar.map { it.outputs.files.singleFile } ),
        zipTree( tasks.jarJar.map { it.outputs.files.singleFile } ).matching {
            include("META-INF/jarjar/**")
        }
    )
    archiveFileName = "${project.name}-${project.version}-merged.jar"
}

tasks.getByName<CopyFileTask>("release") {
    dependsOn( mergeShadowAndJarJar )
    inputFile = mergeShadowAndJarJar.outputs.files.singleFile
}

modrinth {
    gameVersions.addAll(supportedMinecraftVersions)
}

curseforgeBlueMap {
    addGameVersion("Forge")
    addGameVersion("Java ${java.toolchain.languageVersion.get()}")
    supportedMinecraftVersions.forEach {
        addGameVersion(it)
    }
}
