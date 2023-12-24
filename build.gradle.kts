
tasks.register("clean") {
    gradle.includedBuilds.forEach {
        // workaround for https://github.com/neoforged/NeoGradle/issues/18
        if (it.name == "neoforge-1.20.2") return@forEach

        dependsOn(it.task(":clean"))
    }

    doFirst {
        if (!file("build").deleteRecursively())
            throw java.io.IOException("Failed to delete build directory!")
    }
}

tasks.register("build") {
    gradle.includedBuilds.forEach {
        dependsOn(it.task(":release"))
    }
}

tasks.register("test") {
    gradle.includedBuilds.forEach {
        dependsOn(it.task(":test"))
    }
}

tasks.register("spotlessApply") {
    gradle.includedBuilds.forEach {
        dependsOn(it.task(":spotlessApply"))
    }
}

tasks.register("spotlessCheck") {
    gradle.includedBuilds.forEach {
        dependsOn(it.task(":spotlessCheck"))
    }
}

tasks.register("publish") {
    gradle.includedBuilds.forEach {
        dependsOn(it.task(":publish"))
    }
}
