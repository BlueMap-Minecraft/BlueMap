plugins {
    id ( "bluemap.base" )
    id ( "io.github.goooler.shadow" )
}

val Project.releaseDirectory: File
    get() = rootProject.projectDir.resolve("build/release")

tasks.shadowJar {
    archiveFileName = "${project.name}-${project.version}-shadow.jar"
}

tasks.register<CopyFileTask>("release") {
    group = "publishing"
    dependsOn(tasks.shadowJar, tasks.spotlessCheck)

    val prefix = "bluemap-"
    var implementation = project.name
    if (implementation.startsWith(prefix) ) {
        implementation = implementation.substring(prefix.length)
    }

    inputFile = tasks.shadowJar.flatMap { it.archiveFile }
    outputFile = releaseDirectory.resolve("bluemap-${version}-${implementation}.jar")
}

tasks.getByName<Delete>("clean") {
    delete(releaseDirectory.listFiles())
}
