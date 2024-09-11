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
    version = "20.14.0"
    download = true
    nodeProjectDir = file("webapp/")
}

tasks.register("buildWebapp", type = NpmTask::class) {
    dependsOn ("npmInstall")
    args = listOf("run", "build")

    inputs.dir("webapp/")
    outputs.dir("webapp/dist/")
}

tasks.register("zipWebapp", type = Zip::class) {
    dependsOn ("buildWebapp")
    from (fileTree("webapp/dist/"))
    archiveFileName = "webapp.zip"
    destinationDirectory = file("src/main/resources/de/bluecolored/bluemap/")

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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "bluemap-${project.name}"
            version = project.version.toString()

            from(components["java"])
        }
    }
}
