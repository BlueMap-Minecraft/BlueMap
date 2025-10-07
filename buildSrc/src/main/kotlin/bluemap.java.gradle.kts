plugins {
    id ( "bluemap.base" )
    java
    `java-library`
    id ( "com.diffplug.spotless" )
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "utf-8"
}

tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
    withJavadocJar()
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        links(
            "https://docs.oracle.com/en/java/javase/21/docs/api/",
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
