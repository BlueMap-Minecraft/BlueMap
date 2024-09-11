import com.matthewprenger.cursegradle.Options

plugins {
    id ( "bluemap.implementation" )
    id ( "com.matthewprenger.cursegradle" )
}

curseforge {
    apiKey = System.getenv("CURSEFORGE_TOKEN") ?: ""
    options(closureOf<Options> {
        javaVersionAutoDetect = false
        javaIntegration = false
        forgeGradleIntegration = false
    })
}

tasks.curseforge {
    group = "publishing"
    dependsOn(tasks.getByName("release"))
}
