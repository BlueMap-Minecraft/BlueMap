import com.github.gradle.node.npm.task.NpmTask
import java.io.IOException

plugins {
    java
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless") version "6.1.2"
    id ("com.github.node-gradle.node") version "3.5.0"
}

group = "de.bluecolored.bluemap.common"
version = "0.0.0"

val javaTarget = 16
java {
    sourceCompatibility = JavaVersion.toVersion(javaTarget)
    targetCompatibility = JavaVersion.toVersion(javaTarget)
}

repositories {
    mavenCentral()
    maven {
        setUrl("https://libraries.minecraft.net")
    }
    maven {
        setUrl("https://jitpack.io")
    }
}

dependencies {
    api ("com.mojang:brigadier:1.0.17")
    api ("de.bluecolored.bluemap.core:BlueMapCore")

    compileOnly ("org.jetbrains:annotations:16.0.2")
    compileOnly ("org.projectlombok:lombok:1.18.30")

    annotationProcessor ("org.projectlombok:lombok:1.18.30")

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
    version.set("16.15.0")
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

tasks.register("buildWebapp", type = NpmTask::class) {
    doFirst {
        if (!file("webapp/dist/").deleteRecursively())
            throw IOException("Failed to delete build directory!")
    }

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

    //outputs.upToDateWhen { false }
    inputs.dir("webapp/dist/")
    outputs.file("src/main/resources/de/bluecolored/bluemap/webapp.zip")
}

//always update the zip before build
tasks.processResources {
    dependsOn("zipWebapp")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
        }
    }
}
