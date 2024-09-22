plugins {
    id ( "bluemap.implementation" )
    id ( "io.papermc.hangar-publish-plugin" )
}

hangarPublish {
    publications.register("bluemap") {
        apiKey = System.getenv("HANGAR_TOKEN")

        id = "BlueMap"
        channel = "Release"
        version = project.version as String
        changelog = project.releaseNotes()

        platforms.paper {
            jar = tasks.getByName("release").outputs.files.singleFile
        }
    }
}

tasks.getByName("publishBluemapPublicationToHangar") {
    dependsOn(tasks.getByName("release"))
}
