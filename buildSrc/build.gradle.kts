plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    fun plugin(dependency: Provider<PluginDependency>) = dependency.map {
        "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
    }

    implementation ( plugin( libs.plugins.spotless ) )
    implementation ( plugin( libs.plugins.shadow ) )
    implementation ( plugin( libs.plugins.minotaur ) )
    implementation ( plugin( libs.plugins.cursegradle ) )
    implementation ( plugin( libs.plugins.hangar ) )
    implementation ( plugin( libs.plugins.sponge.ore ) )

    // explicitly set guava version to resolve a build-dependency issue
    implementation( libs.guava )

}
