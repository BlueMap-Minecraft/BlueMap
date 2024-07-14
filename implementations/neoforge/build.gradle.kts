plugins {
    bluemap.implementation
    alias ( libs.plugins.neoforge.gradle )
}

val minecraftVersion = "1.21"
val neoVersion = "21.0.0-beta"
val loaderVersion = "4"

val shadowInclude: Configuration by configurations.creating
configurations.api.get().extendsFrom(shadowInclude)
jarJar.enable()

dependencies {
    shadowInclude ( project( ":common" ) ) {
        exclude ( group = "com.google.code.gson", module = "gson" )
        exclude ( group = "com.mojang", module = "brigadier" )
    }

    implementation ( "net.neoforged", "neoforge", neoVersion )

    jarJar ( libs.flow.math.get().group, libs.flow.math.get().name , "[${libs.flow.math.get().version},)" )

}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.shadowJar {
    configurations = listOf(shadowInclude)

    // exclude jarInJar
    dependencies {
        exclude( dependency ( libs.flow.math.get() ) )
    }

    // airlift
    relocate ("io.airlift", "de.bluecolored.shadow.airlift")

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

tasks.jarJar.configure {
    archiveFileName = "${project.name}-${project.version}-jarjar.jar"
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
        zipTree( tasks.jarJar.map { it.outputs.files.singleFile } ).matching {
            include("META-INF/jarjar/**")
        }
    )
    archiveFileName = "${project.name}-${project.version}-merged.jar"
}

tasks.getByName<Copy>("release") {
    dependsOn( mergeShadowAndJarJar )
    from ( mergeShadowAndJarJar.outputs.files.singleFile )
}