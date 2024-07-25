plugins {
    bluemap.base
}

dependencies {
    api ( "de.bluecolored.bluemap:bluemap-api" )

    api ( libs.aircompressor )
    api ( libs.bluenbt )
    api ( libs.caffeine )
    api ( libs.commons.dbcp2 )
    api ( libs.configurate.hocon )
    api ( libs.configurate.gson )
    api ( libs.lz4 )

    compileOnly ( libs.jetbrains.annotations )
    compileOnly ( libs.lombok )

    annotationProcessor ( libs.lombok )

    // tests
    testImplementation ( libs.junit.core )
    testRuntimeOnly ( libs.junit.engine )
    testRuntimeOnly ( libs.lombok )
    testAnnotationProcessor ( libs.lombok )
}

tasks.register("zipResourceExtensions", type = Zip::class) {
    from(fileTree("src/main/resourceExtensions"))
    archiveFileName.set("resourceExtensions.zip")
    destinationDirectory.set(file("src/main/resources/de/bluecolored/bluemap/"))
}

tasks.processResources {
    dependsOn("zipResourceExtensions")

    from("src/main/resources") {
        include("de/bluecolored/bluemap/version.json")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        expand (
            "version" to project.version,
            "gitHash" to gitHash() + if (gitClean()) "" else " (dirty)",
        )
    }
}

tasks.getByName("sourcesJar") {
    dependsOn("zipResourceExtensions")
}
