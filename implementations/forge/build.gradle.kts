plugins {
    bluemap.implementation
    bluemap.modrinth
    bluemap.curseforge
    alias ( libs.plugins.forgegradle )
    alias ( libs.plugins.jarjar )
}

val supportedMinecraftVersions = listOf(
    "26.1", "26.1.1"
)

val minecraftVersion = supportedMinecraftVersions.first()
val forgeVersion = "62.0.1"

val shadowInclude: Configuration by configurations.creating
configurations.api.get().extendsFrom(shadowInclude)
jarJar.register();

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
}

dependencies {

    shadowInclude ( project( ":common" ) ) {
        exclude ( group = "com.google.code.gson", module = "gson" )
    }

    implementation ( minecraft.dependency("net.minecraftforge:forge:$minecraftVersion-$forgeVersion") )
    annotationProcessor ( "net.minecraftforge:eventbus-validator:7.0.1" )

    shadowInclude ( libs.bluecommands.brigadier ) {
        exclude ( group = "com.mojang", module = "brigadier" )
    }
    shadowInclude ( libs.adventure.gson ) {
        exclude ( group = "com.google.code.gson", module = "gson" )
    }

    "jarJar" ( libs.flow.math )
    "jarJar" ( libs.bluenbt )

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
        "forge_version" to forgeVersion,
    )
    inputs.properties(replacements)
    filesMatching(listOf(
        "META-INF/mods.toml",
        "pack.mcmeta"
    )) { expand(replacements) }
}

val jarJarTask = tasks.named<Jar>("jarJar") {
    archiveFileName = "${project.name}-${project.version}-jarjar.jar"
}

val mergeShadowAndJarJar = tasks.register<Jar>("mergeShadowAndJarJar") {
    dependsOn( tasks.shadowJar, jarJarTask )
    from (
        zipTree( tasks.shadowJar.map { it.outputs.files.singleFile } ),
        zipTree( jarJarTask.map { it.outputs.files.singleFile } ).matching {
            include("META-INF/jarjar/**")
        }
    ).exclude(
        "META-INF/services/net.kyori.adventure*" // not correctly relocated and not needed -> exclude
    )
    archiveFileName = "${project.name}-${project.version}-merged.jar"
}

tasks.getByName<CopyFileTask>("release") {
    dependsOn( mergeShadowAndJarJar )
    inputFile = mergeShadowAndJarJar.get().outputs.files.singleFile
}

modrinth {
    gameVersions.addAll(supportedMinecraftVersions)
}

curseforgeBlueMap {
    addGameVersion("Forge")
    addGameVersion("Java ${java.toolchain.languageVersion.get()}")
    //addGameVersion("Server")
    supportedMinecraftVersions.forEach {
        addGameVersion(it)
    }
}
