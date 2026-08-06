import io.github.themrmilchmann.gradle.publish.curseforge.tasks.PublishToCurseForgeRepository

plugins {
    id ( "bluemap.implementation" )
    id ( "io.github.themrmilchmann.curseforge-publish" )
}

curseforge {
    apiToken.set(System.getenv("CURSEFORGE_TOKEN") ?: "")
}

tasks.withType(PublishToCurseForgeRepository::class).configureEach {
    dependsOn(tasks.getByName("release"))
}
