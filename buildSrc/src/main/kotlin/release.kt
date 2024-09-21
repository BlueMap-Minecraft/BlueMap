import org.gradle.api.Project
import java.io.File

val Project.releaseDirectory: File
    get() = rootProject.projectDir.resolve("build/release")
