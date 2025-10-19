// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("dev.rikka.tools.materialthemebuilder") version "1.5.1" apply false
    id("dev.rikka.tools.refine") version "4.4.0" apply false
    id("com.android.library") version "8.13.0" apply false
}

val gitCommitId: String = listOf("git", "rev-parse", "--short", "HEAD").execute(project.rootDir).trim()
val gitCommitCount: Int = listOf("git", "rev-list", "--count", "HEAD").execute(project.rootDir).trim().toInt()
val baseVersionName = "1.0.0-rc1"

extra.apply {
    set("compileSdk", 36)
    set("minSdk", 26)
    set("targetSdk", 36)
    set("versionCode", gitCommitCount)
    set("versionName", "${baseVersionName}.r${gitCommitCount}.${gitCommitId}")
}

fun List<String>.execute(workingDir: File): String {
    return try {
        ProcessBuilder(this)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
            .inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        logger.warn("Failed to execute git command: ${e.message}")
        "unknown" // fallback value
    }
}