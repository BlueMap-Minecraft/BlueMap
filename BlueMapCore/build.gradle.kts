import java.util.Properties
import java.io.IOException

plugins {
    java
    `java-library`
    id("com.diffplug.spotless") version "6.1.2"
}

fun String.runCommand(): String = ProcessBuilder(split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
    .directory(projectDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .apply { waitFor(60, TimeUnit.SECONDS) }
    .run {
        val error = errorStream.bufferedReader().readText().trim()
        if (error.isNotEmpty()) {
            throw IOException(error)
        }
        inputStream.bufferedReader().readText().trim()
    }

val gitHash = "git rev-parse --verify HEAD".runCommand()
val clean = "git status --porcelain".runCommand().isEmpty()
println("Git hash: $gitHash" + if (clean) "" else " (dirty)")

val releaseProperties = Properties()
releaseProperties.load(file("../release.properties").inputStream())

group = "de.bluecolored.bluemap.core"
version = releaseProperties["version"].toString()

val javaTarget = 11
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
}

repositories {
    mavenCentral()
    maven {
        setUrl("https://jitpack.io")
    }
}

@Suppress("GradlePackageUpdate")
dependencies {
    api ("com.github.ben-manes.caffeine:caffeine:2.8.5")
    api ("org.apache.commons:commons-lang3:3.6")
    api ("commons-io:commons-io:2.5")
    api ("org.spongepowered:configurate-hocon:4.1.1")
    api ("org.spongepowered:configurate-gson:4.1.1")
    api ("com.github.Querz:NBT:4.0")
    api ("org.apache.commons:commons-dbcp2:2.9.0")

    api ("de.bluecolored.bluemap.api:BlueMapAPI")

    compileOnly ("org.jetbrains:annotations:23.0.0")

    testImplementation ("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

spotless {
    java {
        target ("src/*/java/**/*.java")

        licenseHeaderFile("../HEADER")
        indentWithSpaces()
        trimTrailingWhitespace()
    }
}

tasks.withType(JavaCompile::class).configureEach {
    options.apply {
        encoding = "utf-8"
    }
}

tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    from("src/main/resources") {
        include("de/bluecolored/bluemap/version.json")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        expand (
            "version" to project.version,
            "gitHash" to gitHash + if (clean) "" else " (dirty)",
        )
    }
}

//resource Extensions
val resourceIds: Array<String> = arrayOf(
    "1_13", "1_15", "1_16", "1_18"
)

tasks.register("zipResourceExtensions") {
    resourceIds.forEach {
        dependsOn("zipResourceExtensions$it")
    }
}

resourceIds.forEach {
    zipResourcesTask(it)
}

fun zipResourcesTask(resourceId: String) {
    tasks.register ("zipResourceExtensions$resourceId", type = Zip::class) {
        from(fileTree("src/main/resourceExtensions/mc$resourceId"))
        archiveFileName.set("resourceExtensions.zip")
        destinationDirectory.set(file("src/main/resources/de/bluecolored/bluemap/mc$resourceId/"))
        outputs.upToDateWhen{ false }
    }
}

//always update the zip before build
tasks.processResources {
    dependsOn("zipResourceExtensions")
}