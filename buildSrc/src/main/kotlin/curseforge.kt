import io.github.themrmilchmann.gradle.publish.curseforge.ChangelogFormat
import io.github.themrmilchmann.gradle.publish.curseforge.CurseForgePublication
import io.github.themrmilchmann.gradle.publish.curseforge.CurseForgePublishingExtension
import io.github.themrmilchmann.gradle.publish.curseforge.Environment
import io.github.themrmilchmann.gradle.publish.curseforge.ReleaseType
import org.gradle.api.Action
import org.gradle.api.Project

fun Project.curseforgeBlueMap (configuration: Action<CurseForgePublication>) {
    val curseforge = extensions.getByName("curseforge")
            as CurseForgePublishingExtension

    curseforge.publications.register("bluemap") {
        projectId.set("406463")
        environments.set(setOf(Environment.Server))

        artifacts.register("main") {
            val releaseFile = tasks.getByName("release").outputs.files.singleFile

            releaseType.set(ReleaseType.RELEASE)
            displayName.set(releaseFile.name)
            changelog {
                format.set(ChangelogFormat.MARKDOWN)
                from(project.releaseNotes())
            }
            from(releaseFile)
        }

        configuration.execute(this)
    }
}

fun CurseForgePublication.minecraftGameVersion(version: String) {
    val parts = version.split(".")
    val one = parts[0]
    val major = parts[1]
    val minor = parts.getOrElse(2) { "0" }
    gameVersion(type = "minecraft-$one-$major", version = "$one-$major-$minor")
}

fun CurseForgePublication.modLoaderGameVersion(loader: String) {
    gameVersion(type = "modloader", version = loader)
}