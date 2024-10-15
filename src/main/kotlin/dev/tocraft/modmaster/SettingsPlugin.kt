package dev.tocraft.modmaster

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.internal.configuration.problems.projectPathFrom
import java.io.File

@Suppress("unused")
class SettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        settings.rootProject.buildFileName = "root.gradle.kts"

        val forcedVersion: String = settings.providers.gradleProperty("minecraft").getOrElse("")
        val useTestmod: String = settings.providers.gradleProperty("testmod").getOrElse("false")

        settings.rootDir.resolve("props").listFiles()?.forEach { file ->
            // Check if the file's name matches the forced version
            if (forcedVersion.isNotBlank() && !file.name.startsWith(forcedVersion)) {
                return@forEach
            }

            val props = file.readLines()
            val foundFabric = props.any { it.startsWith("fabric") }
            val foundForge = props.any { it.startsWith("forge") }
            val foundNeoForge = props.any { it.startsWith("neoforge") }

            val version = file.name.removeSuffix(".properties")

            // Include main version
            settings.include(":$version")
            settings.project(":$version").apply {
                projectDir = settings.rootDir
                buildFileName = "build.gradle.kts"
            }

            // Include common project
            settings.include(":$version:common")
            settings.project(":$version:common").apply {
                projectDir = settings.rootDir.resolve("common")
            }

            // Include testmod if applicable
            if (useTestmod == "true") {
                settings.include(":$version:testmod-common")
                settings.project(":$version:testmod-common").apply {
                    projectDir = settings.rootDir.resolve("testmod-common")
                }
            }

            // Handle fabric, forge, and neoforge projects
            if (foundFabric) {
                settings.include(":$version:fabric")
                settings.project(":$version:fabric").apply {
                    projectDir = settings.rootDir.resolve("fabric")
                }
                if (useTestmod == "true") {
                    settings.include(":$version:testmod-fabric")
                    settings.project(":$version:testmod-fabric").apply {
                        projectDir = settings.rootDir.resolve("testmod-fabric")
                    }
                }
            }

            if (foundForge) {
                settings.include(":$version:forge")
                settings.project(":$version:forge").apply {
                    projectDir = settings.rootDir.resolve("forge")
                }
                if (useTestmod == "true") {
                    settings.include(":$version:testmod-forge")
                    settings.project(":$version:testmod-forge").apply {
                        projectDir = settings.rootDir.resolve("testmod-forge")
                    }
                }
            }

            if (foundNeoForge) {
                settings.include(":$version:neoforge")
                settings.project(":$version:neoforge").apply {
                    projectDir = settings.rootDir.resolve("neoforge")
                }
                if (useTestmod == "true") {
                    settings.include(":$version:testmod-neoforge")
                    settings.project(":$version:testmod-neoforge").apply {
                        projectDir = settings.rootDir.resolve("testmod-neoforge")
                    }
                }
            }
        }
    }
}
