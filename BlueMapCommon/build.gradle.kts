import com.github.gradle.node.npm.task.NpmTask
import java.io.IOException

plugins {
    java
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless") version "6.1.2"
    id ("com.github.node-gradle.node") version "3.5.0"
}

group = "de.bluecolored.bluemap"
version = System.getProperty("bluemap.version") ?: "?" // set by BlueMapCore
val lastVersion = System.getProperty("bluemap.lastVersion") ?: "?" // set by BlueMapCore

val javaTarget = 16
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
}

repositories {
    mavenCentral()
    maven ("https://libraries.minecraft.net")
    maven ("https://repo.bluecolored.de/releases")
}

dependencies {
    api ("com.mojang:brigadier:1.0.17")

    api ("de.bluecolored.bluemap:BlueMapCore")

    compileOnly ("org.jetbrains:annotations:16.0.2")
    compileOnly ("org.projectlombok:lombok:1.18.32")

    annotationProcessor ("org.projectlombok:lombok:1.18.32")

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

node {
    version.set("20.14.0")
    download.set(true)
    nodeProjectDir.set(file("webapp/"))
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

tasks.clean {
    doFirst {
        if (!file("webapp/dist/").deleteRecursively())
            throw IOException("Failed to delete build directory!")
    }
}

tasks.register("buildWebapp", type = NpmTask::class) {
    dependsOn ("npmInstall")
    args.set(listOf("run", "build"))

    inputs.dir("webapp/")
    outputs.dir("webapp/dist/")
}

tasks.register("zipWebapp", type = Zip::class) {
    dependsOn ("buildWebapp")
    from (fileTree("webapp/dist/"))
    archiveFileName.set("webapp.zip")
    destinationDirectory.set(file("src/main/resources/de/bluecolored/bluemap/"))

    inputs.dir("webapp/dist/")
    outputs.file("src/main/resources/de/bluecolored/bluemap/webapp.zip")
}

//always update the zip before build
tasks.processResources {
    dependsOn("zipWebapp")
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
