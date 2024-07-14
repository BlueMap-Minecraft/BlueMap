plugins {
    bluemap.implementation
}

dependencies {
    api ( project( ":common" ) )
    api ( libs.commons.cli )
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks.jar {
    manifest.attributes (
        "Main-Class" to "de.bluecolored.bluemap.cli.BlueMapCLI"
    )
}
