logger.lifecycle("""
## Building BlueMap ...
Java: ${System.getProperty("java.version")}
JVM: ${System.getProperty("java.vm.version")} (${System.getProperty("java.vendor")})
Arch: ${System.getProperty("os.arch")} 
""")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven ("https://maven.minecraftforge.net" )
        maven ("https://maven.fabricmc.net/" )
        maven ("https://maven.neoforged.net/releases" )
    }
}

rootProject.name = "bluemap"

includeBuild("api")

module("core")
module("common")

implementation("cli")
implementation("fabric")
implementation("forge")
implementation("neoforge")
implementation("paper")
implementation("spigot")
implementation("sponge")

fun module(name: String) {
    val project = ":${rootProject.name}-$name"
    include(project)
    project(project).projectDir = file(name)
}

fun implementation(name: String) {
    val project = ":${rootProject.name}-$name"
    include(project)
    project(project).projectDir = file("implementations/$name")
}
