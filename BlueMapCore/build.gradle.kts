import java.util.Properties

plugins {
    java
    `java-library`
    id("com.diffplug.spotless") version "6.1.2"
    id ("com.palantir.git-version") version "0.12.3"
}

val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val git = versionDetails()

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
    api ("com.google.code.gson:gson:2.8.0")
    api ("org.apache.commons:commons-lang3:3.6")
    api ("commons-io:commons-io:2.5")
    api ("com.flowpowered:flow-math:1.0.3")
    api ("org.spongepowered:configurate-hocon:4.1.1")
    api ("org.spongepowered:configurate-gson:4.1.1")
    api ("com.github.Querz:NBT:4.0")
    api ("org.apache.commons:commons-dbcp2:2.9.0")

    compileOnly ("org.jetbrains:annotations:16.0.2")

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
            "gitHash" to git.gitHashFull,
            "gitClean" to git.isCleanTag
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