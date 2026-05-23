plugins {
    id ( "bluemap.base" )
    java
    `java-library`
    id ( "com.diffplug.spotless" )
}

var libs = project.extensions.getByType(VersionCatalogsExtension::class).named("libs")

dependencies {
    compileOnly ( libs.findLibrary("jetbrains-annotations").get() )
    compileOnly ( libs.findLibrary("lombok").get() )

    annotationProcessor ( libs.findLibrary("lombok").get() )

    testImplementation( platform(libs.findLibrary("junit-bom").get()) )
    testImplementation( libs.findBundle("junit-jupiter").get() )
    testAnnotationProcessor ( libs.findLibrary("lombok").get() )

    testRuntimeOnly( libs.findBundle("junit-runtime").get() )
    testRuntimeOnly ( libs.findLibrary("lombok").get() )
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "utf-8"
}

tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
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
