plugins {
    id ( "bluemap.implementation" )
    id ( "com.modrinth.minotaur" )
}

modrinth {
    token = System.getenv("MODRINTH_TOKEN")
    projectId = "swbUV1cr"
    uploadFile = tasks.getByName("release").outputs.files.singleFile
    versionNumber = "${project.version}-${project.name}"
    changelog = project.releaseNotes()
}

tasks.modrinth {
    dependsOn(tasks.getByName("release"))
}
