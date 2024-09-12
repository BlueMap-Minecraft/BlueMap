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
        publishArtifacts.setFrom(tasks.getByName("release").outputs.files.singleFile)
    }
}

tasks.publishToOre {
    dependsOn(tasks.getByName("release"))
}
