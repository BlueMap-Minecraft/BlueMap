plugins {
    java
    `java-library`
    id ( "com.diffplug.spotless" )
}

group = "de.bluecolored.bluemap"
version = gitVersion()

repositories {
    maven ("https://repo.bluecolored.de/releases") {
        content { includeGroupByRegex ("de\\.bluecolored\\..*") }
    }
    maven ("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        content { includeGroup ("org.spigotmc") }
    }

    mavenCentral()
    maven ("https://libraries.minecraft.net")
    maven ( "https://maven.minecraftforge.net" )
    maven ("https://repo.papermc.io/repository/maven-public/")
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "utf-8"
}

tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(16))
    withSourcesJar()
    withJavadocJar()
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        links(
            "https://docs.oracle.com/en/java/javase/16/docs/api/",
            "https://javadoc.io/doc/com.flowpowered/flow-math/1.0.3/",
            "https://javadoc.io/doc/com.google.code.gson/gson/2.8.9/",
        )
        addStringOption("Xdoclint:none", "-quiet")
        addBooleanOption("html5", true)
    }
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    java {
        target ("src/*/java/**/*.java")

        licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
        indentWithSpaces()
        trimTrailingWhitespace()
    }
}
