import com.matthewprenger.cursegradle.CurseProject
import gradle.kotlin.dsl.accessors._30aa10cce2eda716151152e22de91a4c.curseforge
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.kotlin.dsl.closureOf

fun Project.curseforgeBlueMap (configuration: Action<CurseProject>) {
    curseforge.project(closureOf<CurseProject> {
        id = "406463"
        changelogType = "markdown"
        changelog = project.releaseNotes()
        releaseType = "release"
        mainArtifact(tasks.getByName("release").outputs.files.singleFile)

        configuration.execute(this)
    })
}
