import com.matthewprenger.cursegradle.CurseProject
import gradle.kotlin.dsl.accessors._94345610689fa610403700b47edcf23c.curseforge
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
