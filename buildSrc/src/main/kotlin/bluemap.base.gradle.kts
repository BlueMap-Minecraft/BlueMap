plugins {
    `maven-publish`
}

group = "de.bluecolored"
version = gitVersion()

repositories {
    maven ("https://repo.bluecolored.de/releases") {
        content { includeGroupByRegex ("de\\.bluecolored.*") }
    }
    maven ("https://repo.bluecolored.de/snapshots") {
        content { includeGroupByRegex ("de\\.bluecolored.*") }
    }
    maven ("https://hub.spigotmc.org/nexus/content/repositories/snapshots") {
        content { includeGroup ("org.spigotmc") }
    }

    // lwjgl-freetype-3.3.3-natives-macos-patch.jar is not available on Maven
    // Central - pull it from the Minecraft library server instead.
    maven ("https://libraries.minecraft.net") {
        content { includeModule("org.lwjgl", "lwjgl-freetype") }
    }

    mavenCentral()
    maven ("https://libraries.minecraft.net")
    maven ( "https://maven.minecraftforge.net" )
    maven ("https://repo.papermc.io/repository/maven-public")
    maven ( "https://maven.fabricmc.net" )
    maven ( "https://maven.neoforged.net/releases" )
}

publishing {
    repositories {
        maven {
            name = "bluecolored"
            url = uri("https://repo.bluecolored.de/releases")

            if (!gitIsRelease())
                url = uri("https://repo.bluecolored.de/snapshots")

            credentials {
                username = project.findProperty("bluecoloredUsername") as String? ?: System.getenv("BLUECOLORED_USERNAME")
                password = project.findProperty("bluecoloredPassword") as String? ?: System.getenv("BLUECOLORED_PASSWORD")
            }
        }
    }
}
