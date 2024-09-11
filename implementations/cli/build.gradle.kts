plugins {
    bluemap.implementation
}

dependencies {
    api ( project( ":common" ) )
    api ( libs.commons.cli )
}

tasks.jar {
    manifest.attributes (
        "Main-Class" to "de.bluecolored.bluemap.cli.BlueMapCLI"
    )
}
