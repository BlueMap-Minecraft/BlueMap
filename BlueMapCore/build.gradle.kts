import java.io.IOException
import java.util.concurrent.TimeoutException

plugins {
    java
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless") version "6.1.2"
}

fun String.runCommand(): String = ProcessBuilder(split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
    .directory(projectDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .apply {
        if (!waitFor(10, TimeUnit.SECONDS)) {
            throw TimeoutException("Failed to execute command: '" + this@runCommand + "'")
        }
    }
    .run {
        val error = errorStream.bufferedReader().readText().trim()
        if (error.isNotEmpty()) {
            throw IOException(error)
        }
        inputStream.bufferedReader().readText().trim()
    }

val gitHash = "git rev-parse --verify HEAD".runCommand()
var clean = false;
try {
    clean = "git status --porcelain".runCommand().isEmpty();
} catch (ex: TimeoutException) {
    println("Failed to run 'git status --porcelain', assuming dirty version.")
}
val lastTag = if ("git tag".runCommand().isEmpty()) "" else "git describe --tags --abbrev=0".runCommand()
val lastVersion = if (lastTag.isEmpty()) "dev" else lastTag.substring(1) // remove the leading 'v'
val commits = "git rev-list --count $lastTag..HEAD".runCommand()
println("Git hash: $gitHash" + if (clean) "" else " (dirty)")

group = "de.bluecolored.bluemap"
version = lastVersion +
        (if (commits == "0") "" else "-$commits") +
        (if (clean) "" else "-dirty")

System.setProperty("bluemap.version", version.toString())
System.setProperty("bluemap.lastVersion", lastVersion)
println("Version: $version")

val javaTarget = 16
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
}

repositories {
    mavenCentral()
    maven ("https://repo.bluecolored.de/releases")
}

@Suppress("GradlePackageUpdate")
dependencies {
    api ("com.github.ben-manes.caffeine:caffeine:2.8.5")
    api ("org.apache.commons:commons-lang3:3.6")
    api ("commons-io:commons-io:2.5")
    api ("org.spongepowered:configurate-hocon:4.1.2")
    api ("org.spongepowered:configurate-gson:4.1.2")
    api ("de.bluecolored.bluenbt:BlueNBT:2.3.0")
    api ("org.apache.commons:commons-dbcp2:2.9.0")
    api ("io.airlift:aircompressor:0.24")
    api ("org.lz4:lz4-java:1.8.0")

    api ("de.bluecolored.bluemap:BlueMapAPI")

    compileOnly ("org.jetbrains:annotations:23.0.0")
    compileOnly ("org.projectlombok:lombok:1.18.30")

    annotationProcessor ("org.projectlombok:lombok:1.18.30")

    testImplementation ("org.junit.jupiter:junit-jupiter:5.8.2")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testCompileOnly ("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.30")
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
    outputs.upToDateWhen { false }
    from("src/main/resources") {
        include("de/bluecolored/bluemap/version.json")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        expand (
            "version" to project.version,
            "gitHash" to gitHash + if (clean) "" else " (dirty)",
        )
    }
}

tasks.register("zipResourceExtensions", type = Zip::class) {
    from(fileTree("src/main/resourceExtensions"))
    archiveFileName.set("resourceExtensions.zip")
    destinationDirectory.set(file("src/main/resources/de/bluecolored/bluemap/"))
    outputs.upToDateWhen{ false }
}

//always update the zip before build
tasks.processResources {
    dependsOn("zipResourceExtensions")
}

publishing {
    repositories {
        maven {
            name = "bluecolored"

            val releasesRepoUrl = "https://repo.bluecolored.de/releases"
            val snapshotsRepoUrl = "https://repo.bluecolored.de/snapshots"
            url = uri(if (version == lastVersion) releasesRepoUrl else snapshotsRepoUrl)

            credentials {
                username = project.findProperty("bluecoloredUsername") as String? ?: System.getenv("BLUECOLORED_USERNAME")
                password = project.findProperty("bluecoloredPassword") as String? ?: System.getenv("BLUECOLORED_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
            }
        }
    }
}
