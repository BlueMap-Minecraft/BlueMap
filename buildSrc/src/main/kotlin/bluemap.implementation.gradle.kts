plugins {
    id ( "bluemap.base" )
    id ( "io.github.goooler.shadow" )
}

tasks.shadowJar {
    archiveFileName = "${project.name}-${project.version}-shadow.jar"
}

tasks.register<CopyFileTask>("release") {
    group = "publishing"
    dependsOn(tasks.shadowJar, tasks.spotlessCheck)

    inputFile = tasks.shadowJar.flatMap { it.archiveFile }
    outputFile = releaseDirectory.resolve("bluemap-${project.version}-${project.name}.jar")
}

tasks.getByName<Delete>("clean") {
    delete(releaseDirectory.listFiles())
}
