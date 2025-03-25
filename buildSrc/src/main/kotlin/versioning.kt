import org.gradle.api.Project
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun Project.gitHash(): String {
    return runCommand("git rev-parse --verify HEAD", "-")
}

fun Project.gitClean(): Boolean {
    if (runCommand("git update-index --refresh", "NOT-CLEAN").equals("NOT-CLEAN")) return false;
    return runCommand("git diff-index HEAD --", "NOT-CLEAN").isEmpty();
}

fun Project.gitVersion(): String {
    val lastTag = if (runCommand("git tag", "").isEmpty()) "" else runCommand("git describe --tags --abbrev=0", "")
    val lastVersion = if (lastTag.isEmpty()) "0.0" else lastTag.substring(1) // remove the leading 'v'
    val commits = runCommand("git rev-list --count $lastTag..HEAD", "0")
    val branch = runCommand("git branch --show-current", "master")
    val gitVersion = lastVersion +
            (if (branch == "master" || branch.isEmpty()) "" else "-${branch.replace('/', '.')}") +
            (if (commits == "0") "" else "-$commits") +
            (if (gitClean()) "" else "-dirty")

    logger.lifecycle("${project.name} version: $gitVersion")

    return gitVersion
}

fun Project.gitIsRelease(): Boolean {
    val lastTag = if (runCommand("git tag", "").isEmpty()) "" else runCommand("git describe --tags --abbrev=0", "")
    val commits = runCommand("git rev-list --count $lastTag..HEAD", "0")
    return commits == "0" && gitClean()
}

fun Project.releaseNotes(): String {
    val file = rootProject.projectDir.resolve("release.md")
    if (!file.exists()) return ""

    return file
        .readText()
        .replace("{version}", project.version.toString())
}

private fun Project.runCommand(cmd: String, fallback: String? = null): String {
    ProcessBuilder(cmd.split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
        .directory(projectDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
        .apply {
            if (!waitFor(10, TimeUnit.SECONDS))
                throw TimeoutException("Failed to execute command: '$cmd'")
        }
        .run {
            val exitCode = waitFor()
            if (exitCode == 0) return inputStream.bufferedReader().readText().trim()

            val error = errorStream.bufferedReader().readText().trim()
            logger.warn("Failed to execute command '$cmd': $error")
            if (fallback != null) return fallback
            throw IOException(error)
        }
}
