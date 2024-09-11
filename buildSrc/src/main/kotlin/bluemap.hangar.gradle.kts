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
        changelog = rootProject.projectDir.resolve("release.md")
            .readText()
            .replace("{version}", project.version.toString())

        platforms.paper {
            jar = tasks.getByName("release").outputs.files.singleFile
        }
    }
}

tasks.publishAllPublicationsToHangar {
    dependsOn(tasks.getByName("release"))
}
