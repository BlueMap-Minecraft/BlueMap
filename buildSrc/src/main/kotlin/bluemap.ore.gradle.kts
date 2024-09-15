plugins {
    id ( "bluemap.implementation" )
    id ( "org.spongepowered.gradle.ore" )
}

oreDeployment {
    apiKey(System.getenv("ORE_TOKEN"))
    defaultPublication {
        projectId = "bluemap"
        createForumPost = true
        versionBody = project.releaseNotes()
    }
}

tasks.getByName("publishDefaultPublicationToOre") {
    dependsOn(tasks.getByName("release"))
}
