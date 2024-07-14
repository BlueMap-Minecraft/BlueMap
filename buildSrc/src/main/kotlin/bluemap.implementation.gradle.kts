plugins {
    id ( "bluemap.base" )
    id ( "io.github.goooler.shadow" )
}

val Project.releaseDirectory: File
    get() = rootProject.projectDir.resolve("build/release")

tasks.shadowJar {
    archiveFileName = "${project.name}-${project.version}-shadow.jar"
}

tasks.register<Copy>("release") {
    dependsOn(tasks.shadowJar)
    from ( tasks.shadowJar.map { it.outputs.files.singleFile } )
    into ( releaseDirectory )
    rename { "bluemap-${version}-${project.name}.jar" }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.getByName<Delete>("clean") {
    delete(releaseDirectory.listFiles())
}
