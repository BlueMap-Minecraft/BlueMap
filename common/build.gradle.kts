import com.github.gradle.node.npm.task.NpmTask
import java.io.IOException

plugins {
    bluemap.base
    alias ( libs.plugins.node.gradle )
}

dependencies {
    api ( project( ":core" ) )

    api ( libs.brigadier )

    compileOnly ( libs.jetbrains.annotations )
    compileOnly ( libs.lombok )

    annotationProcessor ( libs.lombok )

    // tests
    testImplementation ( libs.junit.core )
    testRuntimeOnly ( libs.junit.engine )
    testRuntimeOnly ( libs.lombok )
    testAnnotationProcessor ( libs.lombok )
}

node {
    version.set("20.14.0")
    download.set(true)
    nodeProjectDir.set(file("webapp/"))
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

tasks.processResources {
    dependsOn("zipWebapp")
}

tasks.getByName("sourcesJar") {
    dependsOn("zipWebapp")
}

tasks.clean {
    doFirst {
        if (!file("webapp/dist/").deleteRecursively())
            throw IOException("Failed to delete build directory!")
    }
}
