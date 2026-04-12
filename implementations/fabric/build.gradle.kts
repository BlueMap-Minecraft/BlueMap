import com.matthewprenger.cursegradle.CurseRelation

plugins {
    bluemap.implementation
    bluemap.modrinth
    bluemap.curseforge
    alias ( libs.plugins.loom )
}

val supportedMinecraftVersions = listOf(
    "26.1", "26.1.1", "26.1.2"
)

val minecraftVersion = "26.1"
val fabricLoaderVersion = "0.18.4"
val fabricApiVersion = "0.144.0+26.1"

val shadowInclude: Configuration by configurations.creating
configurations.api.get().extendsFrom(shadowInclude)

dependencies {

    shadowInclude ( project( ":common" ) ) {
        exclude ( group = "com.google.code.gson", module = "gson" )
    }

    minecraft ("com.mojang:minecraft:${minecraftVersion}")
    implementation ("net.fabricmc:fabric-loader:${fabricLoaderVersion}")
    implementation ("net.fabricmc.fabric-api:fabric-api:${fabricApiVersion}")
    implementation ( libs.fabric.permissions )

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
        "java_version" to java.toolchain.languageVersion.get()
    )
    inputs.properties(replacements)
    filesMatching(listOf(
        "fabric.mod.json",
    )) { expand(replacements) }
}

val mergeShadowAndJarJar = tasks.register<Jar>("mergeShadowAndJarJar") {
    dependsOn( tasks.shadowJar, tasks.jar )
    from (
        zipTree( tasks.shadowJar.map { it.outputs.files.singleFile } ).matching {
            exclude("fabric.mod.json")
        },
        zipTree( tasks.jar.map { it.outputs.files.singleFile } ).matching {
            include("META-INF/jars/**")
            include("fabric.mod.json")
        }
    ).exclude(
        "META-INF/services/net.kyori.adventure*" // not correctly relocated and not needed -> exclude
    )
    archiveFileName = "${project.name}-${project.version}-merged.jar"
}

tasks.getByName<CopyFileTask>("release") {
    dependsOn(mergeShadowAndJarJar)
    inputFile = mergeShadowAndJarJar.flatMap { it.archiveFile }
}

modrinth {
    gameVersions.addAll(supportedMinecraftVersions)
    dependencies { required.project("P7dR8mSH") } // Fabric API
}

curseforgeBlueMap {
    addGameVersion("Fabric")
    addGameVersion("Java ${java.toolchain.languageVersion.get()}")
    //addGameVersion("Server")
    supportedMinecraftVersions.forEach {
        addGameVersion(it)
    }
    relations( closureOf<CurseRelation> {
        requiredDependency("fabric-api")
    })
}
