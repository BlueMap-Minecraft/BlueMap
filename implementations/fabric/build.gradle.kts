import net.fabricmc.loom.task.RemapJarTask

plugins {
    bluemap.implementation
    bluemap.modrinth
    bluemap.curseforge
    alias ( libs.plugins.loom )
}

val supportedMinecraftVersions = listOf(
    "1.21.6"
)

val minecraftVersion = supportedMinecraftVersions.first()
val yarnMappings = "${minecraftVersion}+build.1"
val fabricLoaderVersion = "0.16.14"
val fabricApiVersion = "0.127.1+${minecraftVersion}"

val shadowInclude: Configuration by configurations.creating
configurations.api.get().extendsFrom(shadowInclude)

dependencies {

    shadowInclude ( project( ":common" ) ) {
        exclude ( group = "com.google.code.gson", module = "gson" )
    }

    minecraft ("com.mojang:minecraft:${minecraftVersion}")
    mappings ("net.fabricmc:yarn:${yarnMappings}")
    modImplementation ("net.fabricmc:fabric-loader:${fabricLoaderVersion}")
    modImplementation ("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")
    modImplementation ( libs.fabric.permissions )

    shadowInclude ( libs.bluecommands.brigadier ) {
        exclude ( group = "com.mojang", module = "brigadier" )
    }
    shadowInclude ( libs.adventure.gson ) {
        exclude ( group = "com.google.code.gson", module = "gson" )
    }

    // jarInJar
    include ( libs.flow.math )
    include ( libs.bluenbt )

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
        "fabric_loader_version" to fabricLoaderVersion,
        "minecraft_version" to minecraftVersion,
        "java_version" to java.toolchain.languageVersion.get()
    )
    inputs.properties(replacements)
    filesMatching(listOf(
        "fabric.mod.json",
    )) { expand(replacements) }
}

val remappedShadowJar = tasks.register("remappedShadowJar", type = RemapJarTask::class) {
    dependsOn (tasks.shadowJar)
    archiveFileName = "${project.name}-${project.version}-shadow-remapped.jar"
    inputFile = tasks.shadowJar.flatMap { it.archiveFile }
    addNestedDependencies = true
}

tasks.getByName<CopyFileTask>("release") {
    dependsOn(remappedShadowJar)
    inputFile = remappedShadowJar.flatMap { it.archiveFile }
}

modrinth {
    gameVersions.addAll(supportedMinecraftVersions)
    dependencies { required.project("P7dR8mSH") } // Fabric API
}

curseforgeBlueMap {
    addGameVersion("Fabric")
    addGameVersion("Java ${java.toolchain.languageVersion.get()}")
    supportedMinecraftVersions.forEach {
        addGameVersion(it)
    }
}
